package japath3.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.florianingerl.util.regex.Matcher;

import static io.vavr.collection.List.ofAll;
import static japath3.core.Japath.BoolExpr.Op.and;
import static japath3.core.Japath.BoolExpr.Op.not;
import static japath3.core.Japath.BoolExpr.Op.or;
import static japath3.core.Japath.Comparison.Op.eq;
import static japath3.core.Japath.Comparison.Op.ge;
import static japath3.core.Japath.Comparison.Op.gt;
import static japath3.core.Japath.Comparison.Op.le;
import static japath3.core.Japath.Comparison.Op.lt;
import static japath3.core.Japath.Comparison.Op.neq;
import static japath3.core.Node.nil;
import static japath3.core.Node.nilo;
import static japath3.core.Node.nullo;
import static japath3.util.Basics.embrace;
import static japath3.util.Basics.it;
import static japath3.util.Basics.stream;
import static java.util.Arrays.asList;

import io.vavr.Tuple2;
import japath3.core.Japath.NodeProcessing.Kind;
import japath3.core.Node.DefaultNode;
import japath3.core.Node.PrimitiveType;
import japath3.processing.Language;
import japath3.processing.Language.Env;
import japath3.util.Basics;
import japath3.util.Regex;

/**
 * core containing ADT and 'select & walking'. In order to be compact, 'single line'-code is preferred (similar to functional languages).
 * Furthermore, as usual in languages like scala, relevant (ADT-)classes are defined within this class. 
 * For efficiency reasons primitive java arrays and collections are used in parallel to the functional vavr lib.
 *   
 * @author andreas-fm
 *
 */
public class Japath {

	//----------------- adt ------------------------

	public static abstract class NodeIter implements Iterator<Node> {

		public <T> T val() {
			T val = val(null);
			if (val == null) throw new JapathException("empty node iter");
			else return val;
		};
		public <T> T val(T d) { T val = node().val(); return val == nilo ? d : val; };
		public Node node() { return hasNext() ? next() : nil;};
		
		public boolean arrayFlag() { return false; }
	}
	public static NodeIter single(Node node) {
		return new NodeIter() {
			
			boolean consumed;
			
			@Override public boolean hasNext() { return !consumed; }
			@Override public Node next() { consumed = true; return node; }
		};
	}
	public static NodeIter nodeIter(Iterator<Node> nodes) {
		return new NodeIter() {
			
			@Override public boolean hasNext() { return nodes.hasNext(); }
			@Override public Node next() { return nodes.next(); }
		};
	}
	public static NodeIter empty = new NodeIter() {
		@Override public boolean hasNext() {return false;}
		@Override public Node next() { throw new UnsupportedOperationException(); }
	};
	// for 'opt'-use in 'testIt'
	public static NodeIter emptyTrueCut = new NodeIter() {
		@Override public boolean hasNext() {return false;}
		@Override public Node next() { throw new UnsupportedOperationException(); }
	};
	public static NodeIter ok = singleBool(true, nil);

	@FunctionalInterface
	public static interface Expr {
		public NodeIter eval(Node node);
		public default void clearVars(Ctx ctx) {};
		public default Stream<Expr> stream() {throw new UnsupportedOperationException();};
		public default void visit(BiConsumer<Expr, Boolean> c) {c.accept(this, true);};
		public static Expr Nop = new Expr() {
			@Override public NodeIter eval(Node node) { return single(node); }
			@Override public String toString() { return "Nop"; }
		};	
	}

	/** usually, these are lisp-like expressions */
	public static abstract class CompoundExpr implements Expr {
		public Expr[] exprs = new Expr[0];
		@Override public Stream<Expr> stream() { return Arrays.stream(exprs); }
		@Override public void clearVars(Ctx ctx) { for (Expr expr : exprs) expr.clearVars(ctx); }
		@Override public String toString() { return strStream(); }
		protected String strStream() {
			return strStream(exprs);
		}
		protected String strStream(Expr... exprs) {
			return "(" +  Arrays.stream(exprs).map(x -> {
				return x.toString();
			}).collect(Collectors.joining(",")) + ")";
		}
		@Override public void visit(BiConsumer<Expr, Boolean> c) { 
			c.accept(this, true);
			for (Expr e : exprs) {
				e.visit(c); 
			}
			c.accept(this, false);
		}
		public Expr last() {return exprs[exprs.length - 1];};
	}
	
	public static Expr srex(String regex) {
		return y -> {
			return y.selector.toString().matches(regex) ? single(y) : empty;
		};
	}
	public static <T> Expr bind(Var<T> v) {
		return bind(null, v);
	}
	public static <T> Expr bind(Expr e, Var<T> v) {
		
		return new Expr() {
			
			@Override public NodeIter eval(Node y) { 
				if (e == null) {
					v.bindNode(y);
				} else {
					NodeIter nit = e.eval(y);
					if (nit.hasNext()) {
//						if (nit.hasNext()) throw new JapathException("not allowed"); TOOD
						v.bindNode(nit.next());
					} else {
						v.clear();
					}
				}
				return single(y);
			}
			@Override public String toString() { return "bind(" + e + ", " + v + ")"; }
		};
	}
	
	public static class Bind implements Expr {
		
		public String vname;

		public Bind(String vname) { this.vname = vname; }

		@Override public NodeIter eval(Node node) {
			
			if (vname.equals("_")) vname = node.selector.toString();
			
			boolean firstOcc_;
			Vars vars = node.ctx.getVars();
			Var var = vars.getVar(vname);
			if (var == null) {
				
				Tuple2<Var, Boolean> reg = vars.register(vname, this);
				var = reg._1;
				firstOcc_ = true;
			} 
			else {
				firstOcc_ = vars.firstOcc(vname, this);
			}
			if (firstOcc_) {
				var.bindNode(node);
				return single(node);
			} else {
				throw new JapathException("var '" + vname + "' already defined");
//				return node.val().equals(var.val()) ? single(node) : empty;
			}
		}
		@Override
		public void clearVars(Ctx ctx) {
			if (ctx == null) throw new JapathException();
			Vars vars = ctx.getVars();
			Var var = vars.getVar(vname);
			if (var != null && vars.firstOcc(vname, this)) {
				var.clearVars(ctx);
			}
		}

		@Override public String toString() { return "bind_(" + embrace(vname, '"') + ")"; }
	}
	
	public static class VarAppl implements Expr {

		public String vname;

		public VarAppl(String vname) { this.vname = vname; }

		@Override public NodeIter eval(Node node) {
			Var var = node.ctx.getVars().v(vname);
			// play save:
			if (!var.bound()) throw new JapathException("var '" + vname + "' not bound");
			
			return single(var.node());
		}
		@Override public String toString() { return "varAppl(" + embrace(vname, '"') + ")"; }
	}
	
	public static class ParamExprDef extends CompoundExpr {

		public String name;

		public ParamExprDef(String name, Expr e) {
			this.name = name;
			exprs = new Expr[] { e };
		}
		@Override public NodeIter eval(Node node) { return single(node); }
		@Override public String toString() { return "def('" + name + "', " + exprs[0] + ")"; }
	}

	public static class ParamExprAppl extends CompoundExpr {

		public String name;
		public Expr[] params;

		public ParamExprAppl(String name, Expr[] exprs) {
			this.name = name;
			this.params = exprs == null ? new Expr[0] : exprs;
		}
		@Override public NodeIter eval(Node node) { return exprs[0].eval(node); }
		
		public ParamExprAppl resolve(Env env, ParamExprDef ped, boolean schemaProc) {

//			boolean schemaMode = 
			if (!ped.name.equals(name)) throw new JapathException("name mismatch");
			Expr ed_ = Language.clone(env, ped.exprs[0], schemaProc);
			ed_.visit((x, pre) -> {
				if (x instanceof ParamAppl) {
					ParamAppl pa = (ParamAppl) x;
					try {
						pa.exprs = new Expr[] { Language.clone(env, params[pa.i], schemaProc) };
					} catch (ArrayIndexOutOfBoundsException e) {
						throw new JapathException("bad (zero-based) parameter number " + pa.i);
					}
				}
			});
			exprs = new Expr[] { ed_ };
			return this;
		}
		@Override public String toString() { return name + strStream(params); }
	}

	public static class ParamAppl extends CompoundExpr {

		public int i;

		public ParamAppl(int idx) { this.i = idx; }

		@Override public NodeIter eval(Node node) { return exprs[0].eval(node); }
		@Override public void visit(BiConsumer<Expr, Boolean> c) { c.accept(this, true); }
		@Override public String toString() { return "#" + i; }
	}
	
	public static Expr all = new All();
	public static class All implements Expr {
		@Override public NodeIter eval(Node node) { return node.all(); }
		@Override public String toString() { return "all" ; }
	}

	public static Expr desc = new Desc();
	public static class Desc implements Expr {
		@Override public NodeIter eval(Node node) { return node.desc(); }
		@Override public String toString() { return "desc" ; }
	}
	
	public static Expr self = new Self();
	public static class Self implements Expr {
		@Override public NodeIter eval(Node node) { return single(node); }
		@Override public String toString() { return "self" ; }
	}
	
	public static Expr create = new Create();
	public static class Create implements Expr {
		@Override public NodeIter eval(Node node) {
			return single(node.create(Node.undefWo, "", null, node.ctx).setConstruct(true));
		}
		@Override public String toString() { return "create" ; }
	}

	public static Expr text = new Text();
	public static class Text implements Expr {
		@Override public NodeIter eval(Node node) { return node.text(); }
		@Override public String toString() { return "text()" ; }
	}
	
	// classical path steps according to xpath formal semantics
	public static class PathExpr extends CompoundExpr {

		private PathExpr(Expr[] exprs) {
			this.exprs = exprs;
		}
		@Override
		public NodeIter eval(Node node) {
			return eval(exprs, node);
		}
		private NodeIter eval(Expr[] steps, Node node) {
			if (steps == null || steps.length == 0) {
				return single(node);
			} else {
				NodeIter itY = steps[0].eval(node);
//				if (itY == emptyTrue) return singleBool(true);
				if (itY == emptyTrueCut) return itY;
				return new NodeIter() {
					NodeIter itZ = empty;
					@Override
					public boolean hasNext() {
						
						loop: while(true) {
							
							if (!itZ.hasNext()) {
								if (itY.hasNext()) {
									Expr[] tail = steps.length == 1 ? null : Arrays.copyOfRange(steps, 1, steps.length);
									if (tail != null) for (Expr e : tail) e.clearVars(node.ctx);
									Node next = itY.next();
//									System.out.println(">>> steps: " + asList(steps));
//									System.out.println(">>> tail: " + (tail != null ? asList(tail) : "null"));
//									System.out.println(">>> next: " + next);
									itZ = eval(tail, next);
									if (itZ.hasNext()) {
										return true;
									} else {
										continue loop;
									}
								} else {
									return false;
								}
							} else {
								return true;
							}
						}
					}
					@Override
					public Node next() {
						return itZ.next();
					}
				};
			}
		}
		
		public PathExpr revTail() {return path(Arrays.copyOfRange(exprs, 0, exprs.length - 1));}
		
		@Override public String toString() { return "path" + strStream(); }
		
	}
	
	public static class Walk extends CompoundExpr {
		private Walk(Expr[] exprs) { this.exprs = exprs; }
		@Override public NodeIter eval(Node node) {
			for (@SuppressWarnings("unused") Node n : walki(node, exprs));
			return single(node);
		}
		@Override public String toString() { return "Walk" + asList(exprs) ; }
	}

	public static class Property extends Selection {

		public String name;
		public boolean isTrueRegex;
		
		private Property(String name) {
			
			this.name = name;
//			if (!Language.isIdentifier(name) && (isTrueRegex = Regex.isTrueRegex(name))) {
			if (isTrueRegex = Regex.isTrueRegex(name)) {
				String expl = Regex.check(name);
				if (expl != null) throw new JapathException("'" + name + "' is not a regex (" + expl + ")");
			}
		}
		public NodeIter eval(Node node) {
			
			if (isTrueRegex) {
				return regexSelection(node);
			} else {
				// TODO better attribute handling
				if (!node.isAttribute(name)) node.ctx.checkName(name);
				//
				return node.getChecked(name, this);
			}
		}
		private NodeIter regexSelection(Node node) {
			
			if (node.isCheckedArray()) throw new JapathException("no selector regex match allowed at arrays");
			io.vavr.collection.Iterator<String> matchedSelectors =
					io.vavr.collection.Iterator.ofAll(node.childrenSelectors()).filter(sel -> {
						return sel.matches(name);
					});
			Selection sel = this;
			return nodeIter(new io.vavr.collection.Iterator<NodeIter>() {
				@Override public boolean hasNext() { return matchedSelectors.hasNext(); }
				@Override public NodeIter next() { return node.getChecked(matchedSelectors.next(), sel); }
			}.flatMap(nit -> {
				return it(nit);
			}));
		}
		@Override public boolean isProperty() {return true;}
		@Override public Object selector() { return name; }
		@Override public String toString() { return "__" + (isTrueRegex ? "r" : "")
				+ "(" + embrace(name, '"').replace("\\`", "`") + ")"; }
	}
	
	public static class Idx extends Selection {

		public int i;
		public boolean seq;
		// only for seq i..upper, -1 is length, null is no-slice
		public Integer upper;
		
		private Idx(int i, Integer upper, boolean seq) {
			this.i = i;
			this.upper = upper;
			this.seq = seq;
		}
		public NodeIter eval(Node node) {
			
			if (seq) {
				if (node.construct) throw new JapathException("construction not allowd for sequence subscript");
				return node.order >= i && node.order <= (upper == null ? i : upper == -1 ? Integer.MAX_VALUE : upper)
						? single(node)
						: empty;
			} else {
				NodeIter e = node.getChecked(i, this);
				return e == empty && node.construct ? node.undef(i) : e;
			}
		}
		@Override public boolean isProperty() { return false; }
		@Override public Object selector() { return i; }
		@Override public String toString() { return "__(" + i + (seq ? ", true" : "") + ", " + upper + "" + ")"; }
	}
	
	public static abstract class Selection implements Expr {
		protected Assignment.Scope scope = Assignment.Scope.none;
		public abstract boolean isProperty();
		public abstract Object selector();
		public static Expr expr(Object selector) {
			return selector instanceof String ? (((String) selector).equals("*") ? all : __((String) selector))
					: selector instanceof Integer ? __((Integer) selector) : null;
		}
	}
	
	public static Expr sel = new Selector();
	public static class Selector implements Expr {

		@Override public NodeIter eval(Node node) { return single(new DefaultNode(node.selector.toString(), node.ctx)); }
		@Override public String toString() { return "sel"; }
	}
	
	// for compactness:
	public static <T> Expr c_(T o) {
		return constExpr(o);
	}
	public static <T> Expr constExpr(T o) {
		return new Expr() {
			
			@Override public NodeIter eval(Node node) { 
				return singleObject(o, node); }
			@Override public String toString() {
				return o instanceof String ? embrace(((String) o).replace("'", "\\'"), '\'') : o.toString();
			}
		};
	}
	
	public static class Comparison<T> extends CompoundExpr {
		public enum Op {eq, neq, lt, gt, le, ge, match};
		public Op op;
		
		private Comparison(Op op, Expr e) { this.op = op; exprs = new Expr[] { e }; }

		@Override public NodeIter eval(Node node) {
			
			boolean ret = false;
			NodeIter nit = exprs[0].eval(node);
			if (nit.hasNext()) {
				
				T o = nit.val();
				Object v = node.val();
				
				if (op == eq || op == neq || op == lt || op == gt || op == le || op == ge) {
					
					checkVals(node, o, v);
						
					int cmp = o == nullo ? (v == node.nullWo() ? 0 : -1) : ((Comparable<T>) v).compareTo(o);
					ret = op == lt ? cmp < 0 //
							: op == gt ? cmp > 0 //
									: op == le ? cmp <= 0 //
									: op == ge ? cmp >= 0 //
									: op == eq ? cmp == 0 //
									: op == neq ? cmp != 0 //
									: false
									;
				} else { // regex
					Matcher m = Regex.match(o.toString(), v.toString());
					String g = Regex.group(m, 1);
					return m != null ? (g != null ? singleObject(g, node) : singleBool(true, node)) : singleBool(false, node);
				}
			}
			return singleBool(ret, node); 
		}
		private void checkVals(Node node, T o, Object v) {
			
			if (o != nullo && !v.getClass().equals(o.getClass())) { 
				throw new JapathException(
						"'" + v + "' and '" + o + "' must be of same class (found '"
					+ v.getClass() + "', '"
					+ o.getClass() + "')");
			}
			if (o != nullo && !(v instanceof Comparable))
				throw new JapathException("'" + v.getClass() + "' is not instance of 'Comparable' at '" + this + "'");
			
			if (!(op == eq || op == neq) && (o == node.nullWo() || v == node.nullWo())) 
				throw new JapathException("null not comparable at '" + this + "'");
		}
		@Override public String toString() { return op + "(" + exprs[0] + ")"; }
	}
	
	public static class JavaCall extends CompoundExpr {
		
		public String kind;
		public String ns;
		public String func;
		
		private JavaCall(String kind, String ns, String func, Expr[] exprs) {
			this.kind = kind;
			this.ns = ns;
			this.func = func;
			this.exprs = exprs == null ? new Expr[0] : exprs;
		}
		public NodeIter eval(Node node) {
			
			NodeIter[] nits = new NodeIter[exprs.length];
			for (int i = 0; i < nits.length; i++) nits[i] = exprs[i].eval(node);
			if (kind.equals("directive")) {
				return node.ctx.handleDirective(ns, func, node, nits);
			} else { // so far java
				return Ctx.invoke(ns, func, node, nits);
			}
		}
		@Override public String toString() { return "javaCall[" + kind + ", " + ns + ", " + func + ", " + asList(exprs) + "]"; }
	}
	

	public static class HasType implements Expr {
		
		public PrimitiveType t;		
		
		public HasType(PrimitiveType t) { this.t = t; }
//		public HasType(String t) { this.t = PrimitiveType.valueOf(t); }
		@Override public NodeIter eval(Node node) { 
			return singleBool(node.type(t), node); 
			}
		@Override public String toString() { return "type(" + t + ")"; }
	}
	
	public static class Union extends CompoundExpr {
		
		public boolean arrayFlag;

		private Union(boolean arrayFlag, Expr[] exprs) { this.arrayFlag = arrayFlag; this.exprs = exprs; }

		@Override public NodeIter eval(Node node) {
			
			Iterable<Node>[] iters = new Iterable[exprs.length * 2];
			for (int i = 0, j = 0; i < iters.length; i += 2, j++) {
				Expr ei = exprs[j];
				iters[i] = it(ei.eval(node));
				iters[i + 1] = it(new Iterator<Node>() {
					@Override public boolean hasNext() {
						ei.clearVars(node.ctx);
						return false;
					}
					@Override public Node next() { throw new UnsupportedOperationException(); }
				});
			}
			io.vavr.collection.Iterator<Node> c = io.vavr.collection.Iterator.concat(iters);
			return new NodeIter() {
				@Override public boolean arrayFlag() { return arrayFlag; }
				@Override public Node next() { return c.next(); }
				@Override public boolean hasNext() { return c.hasNext(); }
			};
		}
		
		@Override public String toString() { return "union" + (arrayFlag ? "*" : "") + strStream(); }
	}
	
	public static class Optional extends CompoundExpr {
		
		private Optional(Expr... opt) { exprs = opt; }
		
		@Override public NodeIter eval(Node node) {
			
			NodeIter nit = exprs[0].eval(node);
			return nit.hasNext() ? nit : emptyTrueCut;
		}
		@Override public String toString() { return "opt(" + exprs[0] + ")"; }
	}
	
	public static class BoolExpr extends CompoundExpr {
		
		public static enum Op {and, or, xor, not, imply, constant};
		public Op op;
		
		// pub for special.
		protected BoolExpr(Op op, Expr[] exprs) {
			this.exprs = exprs;
			this.op = op;
		}
		public NodeIter eval(Node node) {
			
			boolean b = true;
			switch (op) {
			case and:
			case not:
				b = true;
				for (Expr e : exprs) 
					b = testIt(e.eval(node)) && b;
				break;
			case or:
			case xor:
				b = false;
				for (Expr e : exprs) { 
					boolean test = testIt(e.eval(node));
					b = op == or ? test || b : test ^ b;
				}
				break;
			case imply:
				b = testIt(exprs[0].eval(node)) ? testIt(exprs[1].eval(node)) : true;
				break;
			case constant:
				return exprs[0].eval(node);
			default: throw new UnsupportedOperationException();
			}
			return singleBool( op == not ? !b : b, node);
		}

		public static Expr True = new Expr() {
			@Override public NodeIter eval(Node node) { return singleBool(true, node); }
			@Override public String toString() { return "true"; }
		};
		public static Expr False = new Expr() {
			@Override public NodeIter eval(Node node) { return singleBool(false, node); }
			@Override public String toString() { return "false"; }
		};
		@Override public String toString() { return (op != Op.constant ? op.toString() + strStream() : exprs[0].toString() ); }
	}
	
	public static class SubExpr extends CompoundExpr {
		
		protected SubExpr(boolean passNode, Expr[] exprs) { this.exprs = exprs; }
		
		@Override public NodeIter eval(Node node) {
			Node x = node;
			for (Expr e : exprs)
				e.eval(x).forEachRemaining(dummy -> {});
			
			return single(x);
		}
		
		@Override public String toString() { return "subExpr" + strStream(); }
	}
	
	public static class Struct extends CompoundExpr {
		
		protected Struct(Expr[] exprs) { this.exprs = exprs; }
		
		@Override public NodeIter eval(Node node) {
			Node x = node.create(Node.undefWo, "", null, node.ctx).setConstruct(true);
			for (Expr e : exprs) {
				if (!(e instanceof Assignment))
					throw new JapathException("only assignments allowed at " + this);
				((Assignment) e).assignEval(x, node);
			}
			return single(x);
		}
		
		@Override public String toString() { return "struct" + strStream(); }
	}
	
	public static class Cond extends CompoundExpr {
		
		private Cond(Expr b, Expr ifExpr, Expr elseExpr) {
			exprs = new Expr[] { b, ifExpr, elseExpr == null ? Nop : elseExpr };
		}
		
		@Override public NodeIter eval(Node node) {
			return testIt(exprs[0].eval(node)) ? exprs[1].eval(node)
					: exprs[2] == Nop ? empty : exprs[2].eval(node);
		}
		
		@Override public String toString() {
			return "cond(" + exprs[0] + ", " + exprs[1] + (exprs[2] == Nop ? "" : ", " + exprs[2]) + ")";
		}
	}
	
	public static class QuantifierExpr extends CompoundExpr {
		
		public static enum Op {every, some};
		public Op op;
				
		protected QuantifierExpr(Op op, Expr qant, Expr check) {
			this.op = op;
			exprs = new Expr[] { qant == null ? all : qant, check };
		}

		@Override public NodeIter eval(Node node) {
			
			NodeIter nit = exprs[0].eval(node);
			boolean b = op == Op.every; // acc. to xpath sem, false if 'some'
			while (nit.hasNext()) {
				boolean t = testIt(exprs[1].eval(nit.next()));
				b = op == Op.every ? t && b : t || b;
			}
			return singleBool(b, node);
		}

		@Override public String toString() { return op + "(" + (exprs[0] instanceof All ? "" : exprs[0] + ", ") + exprs[1] + ")"; }
	}
	
	public static class Filter extends CompoundExpr {

		private Filter(Expr expr) { exprs = new Expr[] { expr }; }
		@Override public NodeIter eval(Node node) { return testIt(exprs[0].eval(node)) ? single(node) : empty; }
		@Override public String toString() { return "?filter(" + exprs[0] + ")"; }
	}
	
	public static class Assignment extends CompoundExpr {
		
		public static enum Scope { lhs, rhs, none }
		
		private Assignment(Expr lhs, Expr rhs) { 
			exprs = new Expr[] { lhs, rhs };
			lhs.visit((x, pre) -> {
				setScope(x, Scope.lhs);
			});
			rhs.visit((x, pre) -> {
				setScope(x, Scope.rhs);
			});
		}

		private void setScope(Expr x, Scope scope) {
			if (x instanceof Selection && ((Selection) x).scope == Scope.none) 
				((Selection) x).scope = scope;
		}
		
		public NodeIter assignEval(Node lhsCtxNode, Node rhsCtxNode) {

			List<Node> ret = new ArrayList<>();
			
			NodeIter nit = exprs[1].eval(rhsCtxNode);
			io.vavr.collection.List<Node> nl = ofAll(it(nit));
			boolean single = nl.size() == 1;
			
			exprs[0].eval(lhsCtxNode).forEachRemaining(lhNode -> {
				
				int i = 0;
				for (Node n : nl) {
					if (single && !nit.arrayFlag()) {
						lhNode.wo = n.woCopy();
						lhNode.setAncestors(this);
					} else {
						lhNode.create(n.woCopy(), i, lhNode, lhNode.ctx).setAncestors(this);
					}
					i++;
				}
				ret.add(lhNode);
			});
			return nodeIter(ret.iterator());
		}
		
		@Override public NodeIter eval(Node node) { 
			return assignEval(node, node);
		}

		@Override public String toString() { return "assign(" + exprs[0] + ", " + exprs[1] + ")"; }
	}
	
	public static NodeIter singleBool(boolean b, Node scopeNode) {
		
		return single(new DefaultNode(b, scopeNode.ctx) {
			@Override public PrimitiveType type() { return PrimitiveType.Boolean; }
		});
	}
	public static NodeIter singleObject(Object o, Node scopeNode) {
		return single(new DefaultNode(o, scopeNode.ctx) {
			@Override public PrimitiveType type() { return PrimitiveType.Any; }
		});
	}	

	//----------------- end adt ------------------------
	
	//----------------- constructor methods (to be used in japath java expressions) in scala it would be implicit in class head ;-) -------
	
	public static Bind bind_(String vname) { return new Bind(vname); }
	public static VarAppl varAppl(String vname) { return new VarAppl(vname); }
	public static ParamExprDef paramExprDef(String name, Expr e) { return new ParamExprDef(name, e); }
	public static ParamExprAppl paramExprAppl(String name, Expr... exprs) { return new ParamExprAppl(name, exprs); }
	public static ParamAppl paramAppl(int i) { return new ParamAppl(i); }
	public static PathExpr path(Expr... exprs) { return new PathExpr(exprs); }
	// for compactness:
	public static PathExpr p_(Expr... exprs) { return new PathExpr(exprs); }
	public static Walk walk(Expr... exprs) { return new Walk(exprs); }
	public static Expr __(String... names) {
		if (names.length == 1) {
			return new Property(names[0]);
		} else {
			PathExpr path = path(new Expr[names.length]);
			for (int i = 0; i < names.length; i++) {
				path.exprs[i] = new Property(names[i]);
			}
			return path;
		}
	}
	public static Idx __(int i) { return __(i, false); }
	public static Idx __(int i, boolean seq) { return new Idx(i, null, seq); }
	public static Idx __(int i, Integer upper, boolean seq) { return new Idx(i, upper, seq); }
	public static <T> Comparison<T> cmpConst(Comparison.Op op, T o) { return new Comparison<T>(op, constExpr(o)); }
	public static <T> Comparison<T> cmp(Comparison.Op op, Expr expr) { return new Comparison<T>(op, expr); }
	public static <T> Comparison<T> eq(T o) { return cmpConst(eq, o); }
	public static <T> Comparison<T> neq(T o) { return cmpConst(neq, o); }
	public static <T> Expr javaCall(String kind, String ns, String func, Expr[] exprs) { return new JavaCall(kind, ns, func, exprs); }
	public static HasType type(PrimitiveType t) { return new HasType(t); }
	public static HasType type(String t) { return type(PrimitiveType.valueOf(t)); }
	public static Union union(Expr... exprs) { return new Union(false, exprs); }
	public static Union union(boolean arrayFlag, Expr... exprs) { return new Union(arrayFlag, exprs); }
	public static Optional optional(Expr expr) { return new Optional(expr); }
	public static Optional opt(Expr expr) { return optional(expr); }
	public static BoolExpr boolExpr(BoolExpr.Op op, Expr... exprs) { return new BoolExpr(op, exprs); }
	public static SubExpr subExpr(boolean passNode, Expr... exprs) { return new SubExpr(passNode, exprs); }
	public static SubExpr subExpr(Expr... exprs) { return new SubExpr(true, exprs); }
	public static Struct struct(Expr[] exprs) { return new Struct(exprs); }
	public static BoolExpr and(Expr... exprs) { return boolExpr(and, exprs); }
	public static BoolExpr or(Expr... exprs) { return boolExpr(or, exprs); }
	public static BoolExpr not(Expr... exprs) { return boolExpr(not, exprs); }
	public static Cond cond(Expr b, Expr ifExpr, Expr elseExpr) { return new Cond(b, ifExpr, elseExpr); }
	public static Cond cond(Expr b, Expr ifExpr) { return new Cond(b, ifExpr, null); }
	public static QuantifierExpr quantifierExpr(QuantifierExpr.Op op, Expr qant, Expr check) { return new QuantifierExpr(op, qant, check); }
	public static QuantifierExpr every(Expr qant, Expr check) { return quantifierExpr(QuantifierExpr.Op.every, qant, check); }
	public static Assignment assign(Expr lhs, Expr rhs) { return new Assignment(lhs, rhs); }
	public static <T> Assignment assign(Expr lhs, T o) { return new Assignment(lhs, constExpr(o)); }
	public static Filter filter(Expr... exprs) {
		return exprs.length > 1 ? filter(path(exprs)) : new Filter(exprs[0]);
	}

	//----------------- end constructors ------------------------
	
	public static boolean testIt(NodeIter nit) {

		boolean ret;
		if (nit.hasNext()) {
			Node next = nit.next();
			next.ctx.preventClearing = true;
			ret = nit.hasNext() ? true : (next.val() instanceof Boolean ? next.val() : true);
			next.ctx.preventClearing = false;
		} else {
			ret = nit == emptyTrueCut;
		}
		return ret;
	}
	
	public static Stream<Node> walks(Node n, Expr... path) {return stream(walki(n, path));}
	
	public static Iterable<Node> walki(Node n, Expr... path) { 
		n.ctx.initSalience(n);
		n.ctx.getVars().add(Var.of(n), "root");
		
		Iterable<Node> it = it(path(path).eval(n));
		return n.ctx.salient() ? io.vavr.collection.Iterator.concat(it, Basics.action(() -> {
			n.ctx.checkSalience();
		})) : it;
	}

	public static Node select(Node n, Expr... path) {		
		Node node = nil;
		for (Node n_: walki(n, path)) node = n_;
		return node;
	}
	
	@FunctionalInterface
	public static interface NodeProcessing  {
		public static enum Kind {Pre, Post}
		public void process(Node x, Kind kind, int level, int orderNo, boolean isLast);
	}
	
	public static void walkr(Node n, NodeProcessing np) { walkr(n, np, 0, 0, true); }
	
	private static void walkr(Node n, NodeProcessing np, int level, int orderNo, boolean isLast) {

		np.process(n, Kind.Pre, level, orderNo, isLast);
		
		Iterator<Node> it = path(all).eval(n);
		int i = 0;
		boolean b = it.hasNext();
		while (b) {
			Node x = it.next();
			b = it.hasNext(); // lookahead !
			walkr(x, np, level + 1, i, !b);
			i++;
		}
		np.process(n, Kind.Post, level, orderNo, isLast);
	}

}
