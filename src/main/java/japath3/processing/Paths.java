package japath3.processing;

import java.util.Comparator;

import static japath3.core.Japath.walkr;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import japath3.core.Japath.NodeProcessing.Kind;
import japath3.core.Node;
import japath3.util.Basics.Ref;

public class Paths {

	public static class Path {
		
		private List<Node> path = List.empty();
		public Node leafValue;
		
		public Path() {
		}
		
		public Path(List<Node> path, Node leafValue) {
			this.path = path;
			this.leafValue = leafValue;
		}

		@Override
		public String toString() {
			return toString(false);
		}

		public String toString(boolean canon) {
			return pathText(canon, false, false) + " = " + leafValue.val();
		}
		
		public String pathText() {
			return pathText(false, false, false);
		}

		
		public String pathText(boolean canon, boolean wildIdx, boolean nameConform) {
			
			StringBuilder sb = new StringBuilder();
			Ref<Integer> i = Ref.of(0);
			path.forEach(x -> {
				Object selector = x.selector;
				boolean isIdx = selector instanceof Integer;
				if (i.r > 0 && (!isIdx || canon)) sb.append(".");
				if (isIdx && !nameConform) sb.append("[");
				boolean nonIdent = !selector.toString().matches("(\\w|\\$)+");
				if (nonIdent) sb.append("`");
				if ( !isIdx || !wildIdx  ) sb.append(selector.toString());
				if (nonIdent) sb.append("`");
				if (isIdx && !nameConform) sb.append("]");
				i.r++;
			});
			
			return sb.toString();
		}

		public void addIdxs(Path p, Idxs idxs) {
		}
		
		public static class Idxs {
			List<Tuple2<Integer, Integer>> v;
			public Idxs(List<Tuple2<Integer, Integer>> v) { this.v = v; }
			public static Idxs empty() { return new Idxs(List.empty()); }
			@Override public String toString() { return v.toString(); }
		}

	}
	
	public static class PathList {
		
		public List<Path> pathList = List.empty();
		
		public PathList append(Path path) {
			pathList = pathList.append(path);
			return this;
		}
		
		public PathList sort(boolean wildIdx) {
			pathList = pathList.sorted(new Comparator<Path>() {
				@Override
				public int compare(Path o1, Path o2) {
					return o1.pathText(false, wildIdx, false).compareTo(o2.pathText(false, wildIdx, false));
				}
			});
			return this;
		}
		
		public String toCRString(boolean canon, boolean wildIdx) {
			
			StringBuilder ret = new StringBuilder();
			pathList.forEach(x -> {
				ret.append(x.pathText(canon, wildIdx, false) + "\n");
			});
			return ret.toString();
		}

		public void addVariant(Path p) {
		}
		
		@Override public String toString() {return pathList.toString();}

		public static PathList create(Node n) {
			
			PathList paths = new PathList();		
			Ref<List<Node>> list = Ref.of(List.empty());
			
			walkr(n, (x, kind, level, orderNo, isLast) -> {
				
				if (kind == Kind.Pre) {
					if (level != 0) list.r = list.r.push(x);
					if (x.isCheckedLeaf()) {
						Path path = new Path(list.r.reverse(), x);
						paths.append(path);
					} else {
					}
				} else { // Post
					if (level != 0) list.r = list.r.pop();
				}
			});
			return paths;
		}
	}
	
	public static Set<String> selectorNameSet(Node n) {
		
		Ref<Set<String>> ret = Ref.of(TreeSet.empty());
		walkr(n, (x, kind, level, orderNo, isLast) -> {
			if (kind == Kind.Pre) {
				Object selector = x.selector;
				if (selector instanceof String) ret.r = ret.r.add(selector.toString());
			}
		});
		return ret.r;
		
	}


}
