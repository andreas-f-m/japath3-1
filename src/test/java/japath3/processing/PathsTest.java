package japath3.processing;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static japath3.wrapper.WJsonOrg.w_;
import static org.junit.Assert.assertEquals;

import japath3.core.Node;
import japath3.processing.Paths.Path;
import japath3.processing.Paths.PathList;
import japath3.util.Basics;

public class PathsTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {}

	@Test
	public void testPaths() {
		
		Node n = w_(new JSONObject(" {a: [ {b: 99, b1: {b2: 88} }, {c: 'lala'  } ]}  "));
		
		PathList paths = PathList.create(n);
		assertEquals("List(a[0].b = 99, a[0].b1.b2 = 88, a[1].c = lala)", paths.toString());
		
		paths = PathList.create(n).sort(false);
		
		assertEquals("List(a[0].b = 99, a[0].b1.b2 = 88, a[1].c = lala)", paths.toString());
		
		System.out.println(paths.toCRString(true, false));
		
		n = w_(new JSONObject(" {a: 1}  "));
		
		paths = PathList.create(n);
		assertEquals("List(a = 1)", paths.toString());

	}
	
//!!!	@Test
//	public void testConstruct() {
//		
//		String res = "a0[1].c.b1";
//		
//		PathExpr pe = e_(res);
//		
//		JSONObject xx = new JSONObject();
//		JSONObject jo = construct(w_(xx), pe, 99).val();
//		
//		assertEquals("{\"a0\":[null,{\"c\":{\"b1\":99}}]}", jo.toString());
//		
//		jo = construct(w_(xx), e_("a0[0]"), 88).val();
//		
//		assertEquals("{\"a0\":[88,{\"c\":{\"b1\":99}}]}", jo.toString());
//	}

//!!!	@Test
//	public void testRegexConstruct() {
//		
//		String r = "a0([**]).(**)";
//		
//		String res = "a0[1].c.b1".replaceAll(RegexPathTransformer.clean(r), "e$1.$2");
//		
//		PathExpr pe = e_(res);
//		
//		JSONObject xx = new JSONObject();
//		JSONObject jo = construct(w_(xx), pe, 99).val();
//		
//		assertEquals("{\"e\":[null,{\"c\":{\"b1\":99}}]}", jo.toString());
//		
//		jo = construct(w_(xx), e_("e[0]"), 88).val();
//		
//		assertEquals("{\"e\":[88,{\"c\":{\"b1\":99}}]}", jo.toString());
//	}
	
//!!!	@Test
//	public void testPureTrans() throws Exception {
//		
//		JSONObject jo = new JSONObject(" {a0: {x: [11, {y:13}] }, a: {c: 88, d: 77}, b: 99 }  ")  ;
//		System.out.println(jo.toString(3));
//		Node source = w_(jo);		
//		Node result = w_(new JSONObject(), new Ctx().setConstruct(true));
//		
//		select(source,
//				or( //
//						path(e_("a0.**") , n -> {
//							
//							if (n.isLeaf()) {
//								System.out.println(PathExpr.buildPathExpr(n));
//								construct(result, n, n.val());
//							}
//							return empty;
//						}),
//						path(e_("a.c"), n -> {
//
//							if (n.isLeaf()) {
//								System.out.println(n.toString());
//							}
//							return empty;
//						})));
//		
//		System.out.println(((JSONObject) result.val()).toString());
//		assertEquals("{\"a0\":{\"x\":[11,{\"y\":13}]}}", result.val().toString());
//	}

	@Test
	public void testPropertyList() throws Exception {
		
		JSONObject jo = new JSONObject(" {a: [ {b: 99, b1: [{'b2 $$ $': 88, 'la la !': '77'}] }, {c: 'lala'  } ]}  ");
		Node n = w_(jo);
		
		PathList paths = PathList.create(n);
		for (Path p : paths.pathList) {
			
			System.out.println( Basics.encode_(p.pathText(true, false, true)));
		}
		
		assertEquals("TreeSet(, a, b, b1, b2 $$ $, c, la la !)", Paths.selectorNameSet(n).toString());
	}

//!!!	@Test
//	public void testToRoot() throws Exception {
//
//		Node n = w_(new JSONObject());
//		Tuple2<Node, Node> constructT = Japath.constructT(n, e_("a.`b`[0].c"), 88);
//		
//		System.out.println(constructT._2);
//		
//		Node x = constructT._2;
//
//		Ref<List<Node>> l = Ref.of(List.empty());
//		x.toRoot(y -> {l.r = l.r.append(y);});
//		System.out.println(l);
//		assertEquals("^List(`c`->88, `0`->{\"c\": 88}, `b`->[{\"c\": 88}], `a`->{\"b\": [{\"c\": 88}]})", l.toString());
//	}

}
