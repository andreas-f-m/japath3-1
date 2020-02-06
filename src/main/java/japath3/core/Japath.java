package japath3.core;

import java.util.Arrays;
import java.util.Iterator;

import io.vavr.Tuple;
import io.vavr.Tuple2;

public class Japath {

	//----------------- adt ------------------------

	public static abstract class Node {
		public NodeIter get(String name) {throw new UnsupportedOperationException();};
		public NodeIter get(int i) {throw new UnsupportedOperationException();};
		public NodeIter all() {throw new UnsupportedOperationException();};
		public NodeIter desc() {throw new UnsupportedOperationException();};
		public Object selector() {throw new UnsupportedOperationException();};
		public <T> T val() {throw new UnsupportedOperationException();};
		public static <T> Node jconst(T o) {
			return new Node() {
				@Override
				public T val() {
					return o;
				}
			};
		}
		public static Tuple2<String, Object> p(String prop, Object v)  {
			return Tuple.of(prop, v);
		}
		public Object jo(Tuple2<String, Object>... args) {throw new UnsupportedOperationException();};
		public Object ja(Object... args) {throw new UnsupportedOperationException();};
		public Node set(String name, Object o) {throw new UnsupportedOperationException();};
	}
	public static class NodeIter extends Node implements Iterator<Node> {

		@Override
		public boolean hasNext() {throw new UnsupportedOperationException();}
		@Override
		public Node next() {throw new UnsupportedOperationException();}
	}
	public static NodeIter singleNode(Node node) {
		return new NodeIter() {
			
			boolean consumed;
			
			@Override
			public boolean hasNext() {
				return !consumed;
			}
			@Override
			public Node next() {
				consumed = true;
				return node;
			}
			@Override public <T> T val() {return node.val();}
			@Override public Object selector() {return node.selector();}
		};
	}
	public static NodeIter nodeIter(Iterator<Node> nodes) {
		return new NodeIter() {
			
			@Override
			public boolean hasNext() {
				return nodes.hasNext();
			}
			@Override
			public Node next() {
				return nodes.next();
			}
		};
	}
	public static NodeIter emptyIter() {
		return new NodeIter() {
			@Override
			public boolean hasNext() {return false;}
			@Override
			public Node next() {return null;}
		};
	}
	@FunctionalInterface
	public static interface Expr {
		public NodeIter eval(Node node);
		public default void clearVars() {};
		public static void clearVars(Expr... exprs) { for (Expr expr : exprs) {expr.clearVars();}}
	}
	
	public static NodeIter fail = new NodeIter() {
		@Override
		public String toString() {
			return "fail";
		}

		@Override
		public boolean hasNext() {
			return false;
		}
	};
	public static Expr all = new Expr() {@Override public NodeIter eval(Node node) { return node.all();}};
	public static Expr rex(String regex) {
		return y -> {
			return y.selector().toString().matches(regex) ? singleNode(y) : fail;
		};
	}
	public static <T> Expr __(String s, Var<T> v) {
		return Path.__(Property.__(s), v);
	}

	
	public static final class Var<T> implements Expr{

		private Node node_;
		boolean preventClearing;

		public Var() {
		}
		public Var(Node node) {
			this.node_ = node;
		}
		public static Var of() {
			return new Var(null);
		}
		@Override
		public String toString() {
			return "^" + (node_ == null ? "null" : node_.toString());
		}
		@Override
		public NodeIter eval(Node node) {
			node_ = node;
			return node == null ? fail : singleNode(node);
		}
		public Var<T> clear() {
			if (!preventClearing) node_ = null;
			return this;
		}
		public Node getNode() {
			return node_;
		}
		public T val() {
			return node_ == null ? null : (T) node_.val();
		}
		public boolean isNull() {
			return val() == null;
		}
		public Var<T> bindVal(T o) {
			this.node_ = Node.jconst(o);
			return this;
		}
		@Override
		public void clearVars() {
			clear();
		}
		public void preventClearing(boolean preventClearing) {
			this.preventClearing = preventClearing;
		}
	}

	public static class Path implements Expr {
		Expr[] exprs;

		public Path(Expr[] exprs) {
			this.exprs = exprs;
		}
		@Override
		public NodeIter eval(Node node) {
			return eval(exprs, node);
		}
		private NodeIter eval(Expr[] steps, Node node) {
			if (steps == null || steps.length == 0) {
				return singleNode(node);
			} else {
				NodeIter itY = steps[0].eval(node);
				return new NodeIter() {
					NodeIter itZ = emptyIter();
					@Override
					public boolean hasNext() {
						
						if (!itZ.hasNext()) {
							if (itY.hasNext()) {
								Expr[] tail = steps.length == 1 ? null : Arrays.copyOfRange(steps, 1, steps.length);
								if (tail != null) for (Expr e : tail) e.clearVars();
								itZ = eval(tail, itY.next());
								return itZ.hasNext() ? true : hasNext();
							} else {
								return false;
							}
						} else {
							return true;
						}
					}
					@Override
					public Node next() {
						return itZ.next();
					}
				};
			}
		}
		public static Path __(Expr... exprs) {
			return new Path(exprs);
		}
		@Override
		public void clearVars() {
			for (Expr expr : exprs) expr.clearVars();
		}
	}

	public static class Property implements Expr {

		public String name;
		
		public Property(String name) {
			this.name = name;
		}
		public NodeIter eval(Node node) {
			NodeIter e = node.get(name);
			return e == null ? fail : e;
		}
		public static Expr __(String... names) {
			if (names.length == 1) {
				return new Property(names[0]);
			} else {
				Path path = Path.__(new Expr[names.length]);
				for (int i = 0; i < names.length; i++) {
					path.exprs[i] = new Property(names[i]);
				}
				return path;
			}
		}
	}
	
	public static class Idx implements Expr {

		int i;
		
		public Idx(int i) {
			this.i = i;
		}
		public NodeIter eval(Node node) {
			return node.get(i);
		}
		public static Idx __(int i) {
			return new Idx(i);
		}
	}
	
	public static class BoolExpr implements Expr {
		
		enum Op {and, or, not};
		Expr[] exprs;
		Op op;
		
		public BoolExpr(Expr[] exprs, Op op) {
			this.exprs = exprs;
			this.op = op;
		}
		public NodeIter eval(Node node) {
			
			boolean b = op == Op.and ? true : (op == Op.or ? false : true);
			for (Expr e : exprs) {
				boolean b_ = test(e.eval(node));
				b = op == Op.and ? b && b_ : (op == Op.or ? b || b_ : true);
			}
			return singleNode(Node.jconst(b));
		}
		public static BoolExpr and(Expr... exprs) {
			return new BoolExpr(exprs, Op.and);
		}
		public static BoolExpr or(Expr... exprs) {
			return new BoolExpr(exprs, Op.or);
		}
		@Override
		public void clearVars() {
			for (Expr expr : exprs) expr.clearVars();
		}
	}

	//----------------- end adt ------------------------
	
	public static boolean test(NodeIter nit) {

		if (nit.hasNext()) {
			Node next = nit.next();
			return nit.hasNext() ? true : (next.val() instanceof Boolean ? next.val() : true);
		} else {
			return false;
		}
	}
	
	public static Node walk(Node o, Expr... path) {
		
		NodeIter eval = Path.__(path).eval(o);
		Node node = null;
		while (eval.hasNext()) node = eval.next();
		return node;
		
	}
}
