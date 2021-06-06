package japath3.wrapper;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import static japath3.core.Japath.empty;
import static japath3.core.Japath.nodeIter;
import static japath3.core.Japath.single;
import static japath3.core.Node.PrimitiveType.Any;
import static japath3.core.Node.PrimitiveType.Boolean;
import static japath3.core.Node.PrimitiveType.Number;
import static japath3.core.Node.PrimitiveType.String;

import japath3.core.Ctx;
import japath3.core.Japath;
import japath3.core.Japath.NodeIter;
import japath3.core.Node;

public class WJsonOrg extends Node {
	
	public WJsonOrg(Object wo, Object selector, Node previousNode, Ctx ctx) { super(wo, selector, previousNode, ctx); }	
	
	public static Node w_(Object x) {
//		return w_(x, "", null, null).setCtx(new Ctx());
		return w_(x, new Ctx());
	}
	public static Node w_(Object x, Ctx ctx) {
		return w_(x, "", null, ctx);
	}
	public static Node w_(Object wo, Object selector, Node previousNode, Ctx ctx) {
		return new WJsonOrg(wo, selector, previousNode, ctx);
	}
	public Node w_(Object x, Object selector) {
		return create(x, selector, this, ctx);
	}
	
	@Override public Node create(Object wo, Object selector, Node previousNode, Ctx ctx) {
		return new WJsonOrg(wo, selector, previousNode, ctx);
	}
	
	@Override public Object createWo(boolean array) { return array ? new JSONArray() : new JSONObject(); }
		
	@Override
	public NodeIter get(String name) {
		
		Object o = wo instanceof JSONObject ? ((JSONObject) wo).opt(name) : null; // TODO undef?
		return o == null ? empty : single(w_(o, name));
	}

	@Override
	public NodeIter get(int i) {
		
		Object o = wo instanceof JSONArray ?  ((JSONArray) wo).opt(i) : null; // TODO undef?
		return o == null ? empty : single(w_(o, i));
	}
	
	@Override public boolean exists(Object selector) {
		return wo instanceof JSONArray ? //
				(selector instanceof Integer ? ((JSONArray) wo).opt((int) selector) != null : false)
				: (wo instanceof JSONObject ? ((JSONObject) wo).has(selector.toString()) : false);
	}
	
	@Override public Iterator<String> childrenSelectors() { return ((JSONObject) wo).keys(); }

	@Override
	public NodeIter all(Object wjo) {
		
		Node prev = this;
		
		if (wjo instanceof JSONArray) {
			Iterator<Object> jait = ((JSONArray) wjo).iterator();
			return new NodeIter() {

				int i = 0;
				
				@Override
				public boolean hasNext() {
					return jait.hasNext();
				}

				@Override
				public Node next() {
					Node n = create(jait.next(), i, prev, ctx).setOrder(i);
					i++;
					return n;
				}
			};
		} else if (wjo instanceof JSONObject) {
			Iterator<String> keys = ((JSONObject) wjo).keys();
			return new NodeIter() {

				int i = 0;
				
				@Override
				public boolean hasNext() {
					return keys.hasNext();
				}

				@Override
				public Node next() {
					String key = keys.next();
					return create(((JSONObject) wjo).get(key), key, prev, ctx).setOrder(i++);
				}
			};
		} else {
			return empty;
		}
	}
	
	@Override
	public NodeIter desc() {
		ArrayList<Node> descs = new ArrayList<Node>();
		gatherDesc(descs, create(wo, selector, previousNode, ctx));
		return nodeIter(descs.iterator());
	}
	
	@Override
	public Node set(String name, Object o) {
		((JSONObject) wo).put(name, o); // TODO overwrite
		return this;
	}
	@Override
	public Node set(int idx, Object o) {
		((JSONArray) wo).put(idx, o); // TODO overwrite
		return this; 
	}
	
	@Override
	public void remove(String name) {
//		if (selector.equals(s)) previousNode.set(s, null);
		if (wo instanceof JSONObject) set(name, null);
	}
	
	@Override public Object woCopy() {
		return wo instanceof JSONObject ? new JSONObject(wo.toString())
				: wo instanceof JSONArray ? new JSONArray(wo.toString()) : wo;
	}
	
	@Override public Object nullWo() { return JSONObject.NULL; }
	@Override public boolean isNull() { return wo == JSONObject.NULL; }
		
	@Override
	public boolean type(PrimitiveType t) {

		switch (t) {
		case String: return wo instanceof String;
		case Number: return wo instanceof Number;
		case Boolean: return wo instanceof Boolean;
		case Any: return true;
		}
		return false;
	}
	
	@Override
	public PrimitiveType type() {

		return wo instanceof String ? String
				: wo instanceof Boolean ? Boolean //
						: wo instanceof Number ? Number : Any;
	}

	@Override public boolean isLeaf() { return !(wo instanceof JSONObject || wo instanceof JSONArray); } 
	@Override public boolean isArray() { return wo instanceof JSONArray; } 
	
	@Override public NodeIter text() { return Japath.singleObject(wo.toString(), previousNode); }
	
	@Override
	public String toString() {
		
		return 
				"`" + selector +
				"`->" + (wo instanceof JSONObject ? ((JSONObject) wo).toString(3)
				: wo instanceof JSONArray ? ((JSONArray) wo).toString(3) : wo.toString());
	}
	
	
}