package japath3.core;

import static japath3.core.Japath.empty;
import static japath3.core.Japath.single;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.graalvm.polyglot.Value;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import japath3.core.Japath.NodeIter;
import japath3.processing.EngineGraal;
import japath3.processing.StringFuncs;
import japath3.processing.TimeFuncs;
import japath3.schema.Schema;
import japath3.util.Basics.Ref;

public class Ctx {

	private Schema schema;
	private boolean checkValidity;

	private Vars vars;

	boolean preventClearing;

	private boolean salient;
	public Set<String> defSelectors;
	public Set<String> undefSelectors;
	
	private static Map<String, Object> nsToEnvObj = HashMap.empty();
	// (ns, func) -> (inst, method, has-val-args)
	private static Map<Tuple2<String, String>, Tuple3<Object, Method, Boolean>> methodMap;
	
	// js
	private static EngineGraal jsEngine;
	
	static {
		loadJInst("str", new StringFuncs());		
		loadJInst("time", new TimeFuncs());
		
		jsEngine = new EngineGraal();
	}
	
	public Ctx() {
		vars = new Vars();
		defSelectors = TreeSet.empty();
		undefSelectors = TreeSet.empty();
//		nsToEnvObj = HashMap.empty();
		methodMap = HashMap.empty();
	}

	public boolean checkValidity() { return checkValidity; }

	public Ctx setCheckValidity(boolean checkValidity, Schema schema) {
		this.checkValidity = checkValidity;
		this.schema = schema;
		return this;
	}

	public void initSalience(Node n) { if (salient && defSelectors.isEmpty()) defSelectors = n.selectorNameSet(); }

	public boolean salient() { return salient; }

	public Ctx setSalient(boolean salient) {
		this.salient = salient;
		return this;
	}

	public Vars getVars() { return vars; }

	public Ctx setVars(Vars vars) {
		this.vars = vars;
		return this;
	}

	public Schema getSchema() { return schema; }

	public Ctx setSchema(Schema schema) {
		this.schema = schema;
		return this;
	}

	public void clearVars() { vars.clearVars(); }

	public void checkName(String name) {

		if (salient) {
			if (!defSelectors.contains(name)) undefSelectors = undefSelectors.add(name);
		}

	}

	public void checkSalience() {
		if (undefSelectors.nonEmpty()) throw new JapathException("salience: selectors " + "["
				+ undefSelectors.mkString(",")
				+ "]"
				+ " used but not found (available seletors: ["
				+ defSelectors.filter(x -> {
					return !x.trim().equals("");
				}).mkString(", ")
				+ "])");
	}
	
	public static Tuple3<Object, Method, Boolean> getTarget(String ns, String func) {
		try {
			Object inst = nsToEnvObj.getOrElse(ns, null);
			if (inst == null) throw new JapathException("ns '" + ns + "' not resolvable");
			
			Class<? extends Object> clazz = inst.getClass();
			Method actMethod = null;
			boolean hasValArgs = false;
			Method[] methods = clazz.getMethods();
			for (Method m : methods) {
				if (m.getName().equals(func)) {
					actMethod = m;
					hasValArgs = m.getParameterTypes()[0] != Node.class;
				}
			}
			if (actMethod == null) throw new JapathException("method '" + ns + ":" + func + "' not found");
			
			return Tuple.of(inst, actMethod, hasValArgs);
					
		} catch (SecurityException e) {
			throw new JapathException("cannot initialize funcCall (" + e + ")");
		}
	}
	
	public static void loadJInst( Tuple2<String, Object>... nss) {
		for (Tuple2<String, Object> ns : nss) {
			loadJInst(ns._1, ns._2);
		}
	}
	
	public static void loadJInst(String ns, Object o) {
		if (nsToEnvObj.containsKey(ns)) throw new JapathException("multiple def of '" + ns + "'");
		nsToEnvObj = nsToEnvObj.put(ns, o);
	}

	public static NodeIter invoke(String ns, String func, Node node, NodeIter[] nits) {

		Tuple2<String, String> nsf = Tuple.of(ns, func);
		// (inst, method, has-val-args)
		Tuple3<Object, Method, Boolean> m = methodMap.getOrElse(nsf, null);
		if (m == null) {
			m = getTarget(ns, func);
			methodMap = methodMap.put(Tuple.of(nsf, m));
		}
		try {
			try {
				if (m._3) {
					Object[] args = new Object[nits.length];
					for (int i = 0; i < args.length; i++) {
						Object val = nits[i].val(null);
						if (val == null) return empty;
						args[i] = val;
					}
					Object ret = m._2.invoke(m._1, args);
					return ret == null ? empty : Japath.singleObject(ret, node);
				} else {
					return nits.length == 0 ? (NodeIter) m._2.invoke(m._1, node) : (NodeIter) m._2.invoke(m._1, node, nits);
				}

			} catch (ClassCastException e) {
				throw new JapathException("bad result type of " + ns + ":" + func + " (" + e + ")");
			}

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new JapathException("cannot invoke " + ns
					+ ":"
					+ func
					+ " ("
					+ (e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e)
					+ ")");
		}
	}
	
	public static void loadJs(Reader js, String name) {
		jsEngine.eval(js, name);
	}
	
	public static NodeIter invokeJs(String func, Node node, NodeIter[] nits) {

		Object[] args = new Object[nits.length + 1];
		args[0] = node.val();
		for (int i = 0; i < nits.length; i++) {
			Node n = nits[i].node();
			String messPrefix = "invoking js '" + func + "': " + (i + 1);
			if (nits[i].hasNext()) throw new JapathException(messPrefix + "-th argument must be a single node");
			if (!n.isLeaf()) throw new JapathException(messPrefix + "-th argument must be a primitive value");

			args[i + 1] = n == Node.nil || n.isNull() ? null : n.val();
		}
		Value v = jsEngine.exec(func, args);

		Ref<Integer> i = Ref.of(0);

		if (v.hasArrayElements()) {
			
			return new NodeIter() {

				@Override public boolean hasNext() { 
					return i.r < v.getArraySize(); }

				@Override public Node next() {
					Object o = v.getArrayElement(i.r++).as(Object.class);
					check(func, i.r, o);
					return new Node.DefaultNode(o == null ? node.nullWo() : o, node.ctx);
				}

			};
		} else {
			Object o = v.as(Object.class);
			return Japath.singleObject(o == null ? node.nullWo() : o, node);
		}

	}
	private static void check(String func, Integer i, Object o) { 
	}

	public NodeIter handleDirective(String ns, String func, Node node, NodeIter[] nits) {

		switch (ns) {
		case "":  // e.g. d:complete

			if (this.schema == null) {
//				throw new JapathException("not in schema mode");
				// TODO Robustness 
				this.schema = new Schema();
			}
			switch (func) {
			case "complete":
				schema.extendPropHits(node);
				return single(node);
			// deferred
			case "modifiable":
				if (node != node.ctx.getVars().getVar("root").node())
					throw new JapathException("only the root node is modifiable as a whole");
				return single(node.setConstruct(true));
			}
			break;
		}
		throw new JapathException("directive '" + ns + ":" + func + "' not found");
	}


}
