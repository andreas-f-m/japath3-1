package japath3.wrapper;

import static japath3.core.Japath.emptyIter;
import static japath3.core.Japath.fail;
import static japath3.core.Japath.nodeIter;
import static japath3.core.Japath.singleNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.vavr.Tuple2;
import japath3.core.Japath.Node;
import japath3.core.Japath.NodeIter;

public class WJsonOrg extends Node {
	
	Object wjo;
	Object selector;
	
	public WJsonOrg(Object wjo) {
		this(wjo, "root");
	}

	public WJsonOrg(Object wjo, Object selector) {
		this.wjo = wjo;
		this.selector = selector;
	}
	
	public static Node w(Object wjo) {
		return new WJsonOrg(wjo);
	};
	
	@Override
	public NodeIter get(String name) {
		return singleNode(wrap(((JSONObject) wjo).opt(name), name));
	}

	@Override
	public NodeIter get(int i) {
		return singleNode(wrap(((JSONArray) wjo).get(i), i));
	}

	@Override
	public NodeIter all() {
		return all(wjo);
	}
	
	public NodeIter all(Object wjo) {
		
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
					return wrap(jait.next(), i++);
				}
			};
		} else if (wjo instanceof JSONObject) {
			Iterator<String> keys = ((JSONObject) wjo).keys();
			return new NodeIter() {

				@Override
				public boolean hasNext() {
					return keys.hasNext();
				}

				@Override
				public Node next() {
					String key = keys.next();
					return wrap(((JSONObject) wjo).get(key), key);
				}
			};
		} else {
			return emptyIter();
		}
	}
	
	@Override
	public NodeIter desc() {
		ArrayList<Node> descs = new ArrayList<Node>();
		gatherDesc(descs, wrap(wjo, selector));
		return nodeIter(descs.iterator());
	}
	
	private void gatherDesc(List<Node> descs, Node node) {
		// TODO here we materialize all descendants. future work should iterate. 
		
		descs.add(node);
		all(node.val()).forEachRemaining(x -> {
			gatherDesc(descs, x);
		});
	}

	@Override
	public JSONObject jo(Tuple2<String, Object>... args) {
		JSONObject jo = new JSONObject();
		for (Tuple2<String, Object> t : args) {
			jo.put(t._1, t._2);
		}
		return jo;
	};
	
	@Override
	public JSONArray ja(Object... args) {
		JSONArray ja = new JSONArray();
		for (Object arg : args) {
			ja.put(arg);
		}
		return ja;
	}
	
	@Override
	public Node set(String name, Object o) {
		((JSONObject) wjo).put(name, o);
		return this;
	}
	
	private Node wrap(Object x, Object selector) {
		return x == null ? fail : new WJsonOrg(x, selector);
	}

	@Override
	public Object selector() {
		return selector;
	}
	
	@Override
	public <T> T val() {
		return (T) wjo;
	}
	
	@Override
	public String toString() {
		return "->" + wjo.toString();
	}
}