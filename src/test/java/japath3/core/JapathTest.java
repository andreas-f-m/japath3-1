package japath3.core;

import static japath3.core.Japath.__;
import static japath3.core.Japath.all;
import static japath3.core.Japath.fail;
import static japath3.core.Japath.rex;
import static japath3.core.Japath.singleNode;
import static japath3.core.Japath.walk;
import static japath3.core.Japath.BoolExpr.and;
import static japath3.core.Japath.BoolExpr.or;
import static japath3.core.Japath.Idx.__;
import static japath3.core.Japath.Path.__;
import static japath3.core.Japath.Property.__;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import japath3.core.Japath.Node;
import japath3.core.Japath.Var;
import japath3.wrapper.WJsonOrg;


public class JapathTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		fail("Not yet implemented");
	}

	@Test
	public void testBasics() {

		WJsonOrg wjo = new WJsonOrg(new JSONObject(" {a: [ {b: 99, b1: {b2: 88} }, {c: 'lala'  } ]}  "));
		
//		NodeIter nit = Path.__(y -> wjo.get("a"), __(1), __("c")).eval(wjo);
//
//		nit.hasNext();
//		System.out.println(nit.next());

		Node x = walk(wjo, y -> wjo.get("a"), __(1), __("c"));

		assertEquals("lala", x.val());

		x = walk(wjo, y -> wjo.get("a"), all, __("c"));

		assertEquals("lala", x.val());

		x = walk(wjo, __("a"), __(0), __("b1", "b2"));

		assertEquals((Integer) 88, x.val());

		Var<Object> h = Var.of();
		Var h1 = Var.of();
		Var<Integer> h2 = Var.of();

		x = walk(wjo,
				__("a"), //
				__(0), //
				y -> {
					h.bindVal(y.get("b").val());
					Integer i = y.get("b").val();
					return i == 99 ? singleNode(y) : fail;
				}, //
				h1,
				__("b1"), //
				__("b2"));

		Integer val = x.val();
		assertEquals((Integer) 88, val);
		assertEquals((Integer) 99, h.val());
		assertEquals((Integer) 99, h1.getNode().get("b").val());

		h.clear().preventClearing(true);
		h2.clear().preventClearing(true);

		walk(wjo,
				__("a"), //
				all,
				rex("0"),
				and(//
						__("b", h),
						__(__("b1", "b2"), h2)));

		assertEquals((Integer) 99, h.val());
		assertEquals((Integer) 88, h2.val());
		
		WJsonOrg wjo1 = new WJsonOrg(new JSONObject(" {a: [ {b: 99, b1: {b2: 88} }, {c: 'lala'  } ]}  "));

		h.clear().preventClearing(false);
		h1.clear().preventClearing(false);
		h2.clear().preventClearing(false);
		
		walk(wjo1,
				__("a"),
				all,
				or(//
						and(//
								__("b", h),
								__(__("b1", "b2"), h2)),
						__("c", h)),

				y -> {
					assertTrue(h.val().toString().equals("99") || h.val().toString().equals("lala"));
					System.out.println("" + h + h2);
					return singleNode(y);
				});
		
//		String s = "{" + "	'expr': {"
//				+ "		'times': ["
//				+ "			{'const': 1},"
//				+ "			{'plus': ["
//				+ "				{'const': 2},"
//				+ "				{'const': 3}"
//				+ "			]}"
//				+ "		]}"
//				+ "}";
//
//		WJsonOrg c = new WJsonOrg(new JSONObject(s));
//
//		h.clear();
//		h1.clear();
//		h2.clear();
//
//		x = walk(c,
//				__("expr", "times"),
//				and(__(0, h),
//						__(__(1),
//								__("plus"),
//								and(__(0, h1), //
//										__(1, h2)))),
//				y -> {
//					Object o = //
//							c.jo(p("expr",
//									c.jo(p("plus",
//											c.ja(c.jo(p("times", c.ja(h.val(), h1.val()))),
//													c.jo(p("times", c.ja(h.val(), h2.val()))))))));
//
//					return new WJsonOrg(o);
//				});
//		
//		
//		assertEquals("->{\"expr\":{\"plus\":[{\"times\":[{\"const\":1},{\"const\":2}]},{\"times\":[{\"const\":1},{\"const\":3}]}]}}", x.toString());
//		
//		assertEquals("^->{\"const\":1}^->{\"const\":2}^->{\"const\":3}", "" + h + h1 + h2);

	}

	@Test
	public void testDesc() {
		
//		WJsonOrg wjo = new WJsonOrg(new JSONObject(" {a: [ {b: 99, b1: {b2: 88} }, {c: 'lala'  } ]}  "));
//
//		StringBuffer sb = new StringBuffer();
//		walk(wjo, y -> wjo.get("a"), desc, y -> {
//			sb.append(y.toString());
//			return ok;
//		});
//
//		assertEquals(
//				"->[{\"b\":99,\"b1\":{\"b2\":88}},{\"c\":\"lala\"}]->{\"b\":99,\"b1\":{\"b2\":88}}->99->{\"b2\":88}->88->{\"c\":\"lala\"}->lala",
//				sb.toString());
//		
//		sb.setLength(0);
//		walk(wjo, y -> wjo.get("a"), desc, rex("b.*"), y -> {
//			sb.append(y.toString());
//			return ok;
//		});
//		
//		assertEquals(
//				"->99->{\"b2\":88}->88",
//				sb.toString());
//		
//		sb.setLength(0);
//		walk(wjo, y -> wjo.get("a"), desc, y -> {
//			sb.append("-" + y.selector());
//			System.out.println("-" + y.selector());
//			return ok;
//		});

	}
}
