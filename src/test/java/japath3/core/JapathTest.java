package japath3.core;

import static japath3.core.Japath.__;
import static japath3.core.Japath.all;
import static japath3.core.Japath.and;
import static japath3.core.Japath.desc;
import static japath3.core.Japath.empty;
import static japath3.core.Japath.eq;
import static japath3.core.Japath.filter;
import static japath3.core.Japath.not;
import static japath3.core.Japath.ok;
import static japath3.core.Japath.path;
import static japath3.core.Japath.select;
import static japath3.core.Japath.single;
import static japath3.core.Japath.srex;
import static japath3.core.Japath.union;
import static japath3.core.Japath.walks;
import static japath3.core.Node.nilo;
import static japath3.processing.Language.e_;
import static japath3.processing.Language.stringify;
import static japath3.wrapper.NodeFactory.w_;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import japath3.core.Japath.Expr;
import japath3.core.Japath.PathExpr;
import japath3.processing.Language.Env;
import japath3.util.Basics;
import japath3.wrapper.WJsoup;


public class JapathTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testBasics() {

		Node n = w_(" {a: [ {b: 99, b1: {b2: 88} }, {c: 'lala'  } ]}  ");
		

		Node x = select(n, y -> n.get("a"), __(1), __("c"));

		assertEquals("lala", x.val());

		x = select(n, y -> n.get("a"), __(0), __("cNop"));

		assertEquals(nilo, x.val());

		x = select(n, y -> n.get("a"), all, __("c"));
		
		assertEquals("lala", x.val());
		
		x = select(n, __("a"), __(0), __("b1", "b2"));
		x = select(n, e_("a[0].b1.b2") );

		assertEquals((Number) 88, ((Number) x.val()).intValue());

		Var<Object> h = Var.of();
		Var h1 = Var.of();
		Var<Integer> h2 = Var.of();

		x = select(n,
				__("a"), //
				__(0), //
				y -> {
					h.bindNode(y.get("b").node());
					Integer i = ((Number) y.get("b").val()).intValue();
					return i == 99 ? single(y) : empty;
				}, //
//				h1,
				Japath.bind(h1),
				__("b1"), //
				__("b2"));

		Integer val = ((Number) x.val()).intValue();
		assertEquals((Integer) 88, val);
		assertEquals((Number) 99, ((Number) h.val()).intValue());
		assertEquals((Number) 99, ((Number) h1.node().get("b").val()).intValue());

		h.clear().preventClearing(true);
		h2.clear().preventClearing(true);

		select(n,
				__("a"), //
				all,
				srex("0"),
				and(//
						path(__("b"), h),
						path(__("b1", "b2"), h2)));

		assertEquals((Number) 99, ((Number) h.val()).intValue());
		assertEquals((Number) 88, ((Number) h2.val()).intValue());
		
		Node n1 = w_(" {a: [ {b: 99, b1: {b2: 88} }, {c: 'lala'  } ]}  ");

		h.preventClearing(false).clear();
		h1.preventClearing(false).clear();
		h2.preventClearing(false).clear();
		
//		Vars vars1 = new Vars().add(h, "h").add(h1, "h1").add(h2, "h2");
		PathExpr pe = e_( //
				"a.*.or("
				+ "     and("
				+ "        b $h, "
				+ "        b1.b2 $h2),"
				+ "     c $h1)");
		
//		n1.ctx.setVars(vars1);
		Vars vars2 = n1.ctx.getVars();
		select(n1,
				pe,
				y -> {
					String s = "" + vars2.v("h") + vars2.v("h1") + vars2.v("h2");
					System.out.println(s);
					assertTrue( s.equals("^`b`->99^null^`b2`->88") || s.equals("^null^`c`->lala^null") );
					return single(y);
				});
		
//!!!		String s = "{" + "	'expr': {"
//				+ "		'times': ["
//				+ "			{'const': 1},"
//				+ "			{'plus': ["
//				+ "				{'const': 2},"
//				+ "				{'const': 3}"
//				+ "			]}"
//				+ "		]}"
//				+ "}";
//
//		Node c = w_(s);
//
//		h.clear();
//		h1.clear();
//		h2.clear();
//
//		x = select(c,
//				__("expr", "times"),
//				and(path(__(0), h),
//						path(__(1),
//								__("plus"),
//								and(path(__(0), h1), //
//										path(__(1), h2)))),
//				y -> {
//					Object o = //
//							c.prop(p("expr",
//									c.prop(p("plus",
//											c.array(c.prop(p("times", c.array(h.val(), h1.val()))),
//													c.prop(p("times", c.array(h.val(), h2.val()))))))));
//
//					return singleo(o);
//				});
//		
//		
//		assertEquals("``->{\"expr\": {\"plus\": [\n" + 
//				"   {\"times\": [\n" + 
//				"      {\"const\": 1},\n" + 
//				"      {\"const\": 2}\n" + 
//				"   ]},\n" + 
//				"   {\"times\": [\n" + 
//				"      {\"const\": 1},\n" + 
//				"      {\"const\": 3}\n" + 
//				"   ]}\n" + 
//				"]}}", x.toString());
//		
//		x = select(x, __("expr"), desc, __("const"), 
//
//				y -> {
//					return single(y);
//				},
//
//				filter( or( eq(1), eq(2)  )   ),
//				
//				y -> {
//					return single(y);
//				}
//				);
//		
//		assertEquals((Integer) 1, x.val());
//		assertEquals("^`0`->{\"const\": 1}^`0`->{\"const\": 2}^`1`->{\"const\": 3}", "" + h + h1 + h2);
		
		x = select(n1, e_("a[0].b"), eq(99));

		assertEquals(true, x.val());
		
		x = select(n1, __("a"), __(0), filter(path(__("b"), eq(99))), __("b1", "b2"));
		
		assertEquals((Number) 88, ((Number) x.val()).intValue());
		
	}

	@Test
	public void testDesc() {
		
		Node n = w_(" {a: [ {b: 99, b1: {b2: 88} }, {c: 'lala'  } ]}  ");
//
		StringBuilder sb = new StringBuilder();
		select(n, y -> n.get("a"), desc, y -> {
			sb.append(y.toString());
			return empty;
		});

		assertEquals(
				"`a`->[{\"b\":99,\"b1\":{\"b2\":88}},{\"c\":\"lala\"}]`0`->{\"b\":99,\"b1\":{\"b2\":88}}`b`->99`b1`->{\"b2\":88}`b2`->88`1`->{\"c\":\"lala\"}`c`->lala",
				sb.toString());
		
		sb.setLength(0);
		select(n, y -> n.get("a"), desc, srex("b.*"), y -> {
			sb.append(y.toString());
			return empty;
		});
		
		assertEquals(
				"`b`->99`b1`->{\"b2\":88}`b2`->88",
				sb.toString());
		
		sb.setLength(0);
		select(n, y -> n.get("a"), desc, y -> {
			sb.append("-" + y.selector);
			System.out.println("-" + y.selector);
			return empty;
		});
		System.out.println(sb.toString());

	}
	
	@Test
	public void testSimplePath() {
		
		Node n = w_(" {a: [ {b: 99, b1: {b2: 88} }, {c: 'lala'  } ]}  ");
		Expr[] expr = { e_("a[0].b1") };
		
		select(n, expr).set("b2", 77);
		
		assertEquals((Integer) 77, select(n, e_("a[0].b1.b2")).val());

		assertEquals((Integer) 77, select(n, e_("a.*"), e_("b1.b2")).val());
		
		n = w_(" {a: [ {'??$%&H.Hb': 99, b1: {'b`2': 88} }, {'p#\"a-b': 'lala'  } ]}  ");
		Expr[] expr1 = { e_("a[0].`??\\$%&H\\.Hb`") };
		
		Node x = select(n, expr1);
		assertEquals((Number) 99, ((Number) x.val()).intValue());
		
		x = select(n, e_("a[0]"), __("??\\$%&H\\.Hb") );
		assertEquals((Number) 99, ((Number) x.val()).intValue());
		
		x = select(n, e_("a[0].b1.`b\\`2`") );
		assertEquals((Number) 88, ((Number) x.val()).intValue());
		
		x = select(n, e_("a[1].`p#\"a-b`") );
		assertEquals("lala", x.val());
		
		x = select(n, e_("a[1].`p#\"a-b`.match('l(.*)a').eq('al')") );
		assertEquals(true, x.val());
		
	}
	
	@Test
	public void testBool() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		Node x = select(n, __("a"), filter(path(__("b"), eq(false))), __("c"));
		assertEquals("lala", x.val());
		x = select(n, __("a"), filter(not(__("b"))), __("c"));
		assertEquals("lala", x.val());
		
		n = w_(" {a: {b: 1, c: 'lala'} }  ");
		
		assertIt(n, "[false]", "a.xor(b.eq(1), c.eq('lala'))");
		assertIt(n, "[true]", "a.xor(b.eq(1), c.eq('lalax'))");
	}
	
	@Test
	public void testConst() {
		
		Node n = w_(" {}  ");

		assertIt(n, "[ ]", "' '");
		
	}
	
	@Test
	public void testCmp() {
		
		Node n = w_(" { v: 1, w: 1, a: true, b: \"lala\" }  ");

		assertTrue(select(n, e_("a.eq($.a)")).val());
		assertTrue(select(n, e_("a.eq(true)")).val());
		assertFalse(select(n, e_("a.eq(false)")).val());
		assertFalse(select(n, e_("false.eq($.a)")).val());

		assertTrue(select(n, e_("b.eq('lala')")).val());
		
		assertTrue(select(n, e_("v.lt(2)")).val());	
		assertTrue(select(n, e_("v.gt(0)")).val());
		assertTrue(select(n, e_("v.le(1)")).val());
		assertTrue(select(n, e_("v.ge(1)")).val());
		
		assertTrue(select(n, e_("v.ge(-1.4E2)")).val());
//		assertTrue(select(n, e_("v.ge(-1.88)")).val());

		assertFalse(select(n, e_("v.ge(v)")).val());
		assertTrue(select(n, e_("v.ge($.w)")).val());
	}

	
	@Test
	public void testOptional() {
		
		Node n = w_(" {a: {b: 99, c: 'lala'} }  ");
		
		Node x = select(n, e_("a.optional(b.type(Number))"));
		assertEquals(true, x.val());
		
		x = select(n, e_("a.and(opt(bb))"));
		assertEquals(true, x.val());
		
		x = select(n, e_("a.and(opt(bb).x)"));
		assertEquals(true, x.val());
		
		x = select(n, e_("opt(aa).b"));
		assertEquals(nilo, x.val());
		
		x = select(n, e_("opt(a).b"));
		assertEquals((Number) 99, ((Number) x.val()).intValue());
	}
	
	@Test
	public void testCond() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		Node x = select(n, e_("a.cond(b, b, c)"));
		assertEquals("lala", x.val());
		
		PathExpr e_ = e_("a.cond(not(b), b, c)");
		x = select(n, e_);
		assertEquals(false, x.val());
		
		System.out.println(stringify(e_, 1));

		x = select(n, e_("a.cond(b, b, true)"));
		assertEquals(true, x.val());
		
		x = select(n, e_("a.cond(b, c)"));
//		assertEquals("{\"b\":false,\"c\":\"lala\"}", x.val().toString());
		assertEquals(nilo, x.val());
	}
	
	@Test
	public void testImply() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		Node x = select(n, e_("a.imply(b, c)"));

		assertEquals(true, x.val());
		
		x = select(n, e_("a.imply(not(b), false)"));
		
		assertEquals(false, x.val());
	}
	
//!!!	@Test
//	public void testConstruct() {
//		
//		JSONObject jo = new JSONObject();
//		Ctx ctx = new Ctx().setConstruct(true);
//		
//		select(w_(jo, ctx), e_("a[0].b1.b2").prepConstruct());
//		
//		assertEquals("{\"a\":[{\"b1\":{\"b2\":\"nullo\"}}]}", jo.toString());
//		
//		jo = new JSONObject();
//		select(w_(jo, ctx), e_("a.b1.b2[3]").prepConstruct());
//		
//		assertEquals("{\"a\":{\"b1\":{\"b2\":[null,null,null,\"nullo\"]}}}", jo.toString());
//		
//		select(w_(jo, ctx), e_("a.b1.c").prepConstruct());
//		
//		assertEquals("{\"a\":{\"b1\":{\"b2\":[null,null,null,\"nullo\"],\"c\":\"nullo\"}}}", jo.toString());
//	}
	
	@Test
	public void testModify() {
		
		String jo = "{l: [{i: 0}, {i: 1}, {i: 2}]}";

		Node n = w_(jo);
		select(n, e_("l.*"), x -> {
			x.set("i", ((Number) x.val("i")).intValue() + 1);
			return ok;
		});
		
		assertEquals("{\"l\":[{\"i\":1},{\"i\":2},{\"i\":3}]}", n.val().toString());
	}

	
	@Test
	public void testUnion() {
		
		String jo = " {b: 99, c:'lala'}  ";
		Node n = w_(jo);
		
		assertEquals("[`b`->99, `c`->lala]", Basics.stream(Japath.walki(n, union(__("b"), __("c")))).collect(Collectors.toList()).toString());

	}

	@Test
	public void testUnion1() {
		
		StringBuilder sb = new StringBuilder();
		
		String jo = " { "
				+ "	root: [ "
				+ "		{ "
				+ "			a: 1 "
				+ "		}, "
				+ "		{ "
				+ "			b: 2 "
				+ "		} "
				+ "	] "
				+ "}  ";
		
		Node n = w_(jo);

		PathExpr e = e_("root.*.union( a $x, b $y )");
		Vars vars = new Vars();
//		Var<Integer> x = vars.of("x");
//		Var<Integer> y = vars.of("y");
		
		n.ctx.setVars(vars);
		
		Japath.walki(n, e).forEach(
				z -> {
					sb.append(vars.v("x") + ", " + vars.v("y") + "; ");
//					System.out.println(z);
//					System.out.println(x);
//					System.out.println(y);
				}
				);
//		System.out.println(sb.toString());

		assertEquals("^`a`->1, ^null; ^null, ^`b`->2; ", sb.toString());
		
		assertIt(n, "[1, 2, 3]", "union(1,2,3)");
		
		assertIt(n, "[1, 2, 3]", "union(1, union(2,3))");
		
		assertIt(n, "[[{\"a\":1},{\"b\":2}]]", "new : [root.*]");
	}
	
	@Test
	public void testQuantifier() {
		
		String jo = "{ a:{b: 99, c:'lala'}, d:{c:'lalax'}, e:[1, 2]  }";
		Node n = w_(jo);
		
		assertIt(n, "[true]", "e.some(*, eq(1))");
		assertIt(n, "[false]", "e.some(*, eq(11))");
		assertIt(n, "[false]", "e.some(d, eq(1))");
		
		StringBuilder sb = new StringBuilder();
		
		PathExpr e = e_("**.every(c, eq('lala'))");
		System.out.println(stringify(e, 1));
		select(n, e, x -> {
			sb.append(" " + x.val());
			return ok;
		}).val();
		
		assertEquals(" true true true true false true true true true", sb.toString());
//		assertEquals(" {\"a\":{\"b\":99,\"c\":\"lala\"},\"d\":{\"c\":\"lalax\"}} true 99 lala false lalax", sb.toString());
		
		e = e_("d.every(*, eq('lalax'))");
		System.out.println(stringify(e, 1));
		sb.setLength(0);
		select(n, e, x -> {
			sb.append(" " + (boolean) x.val());
			return ok;
		}).val();
		
		assertEquals(" true", sb.toString());
		
	}

	@Test
	public void testHasType() {
		
		String jo = "{ a:{b: 99, c:'lala'}, d:{c:'lalax'}  }";
		Node n = w_(jo);
		
		StringBuilder sb = new StringBuilder();
		select(n, e_("**.type(Number)"), x -> {
			sb.append(" " + (boolean) x.val());
			return ok;
		});
		assertEquals(" false false true false false false", sb.toString());

	}
	
	@Test
	@Ignore
	public void testCallFunc() {
		
		PathExpr e = e_("a.call(`p.f`)(x, y)");
		
		System.out.println(stringify(e, 1));
		
	}
	
	@Test
	public void testSelector() {

		String jo = "{ a:{b: 99, c:'lala'}, d:{c:'lalax'}  }";
		Node n = w_(jo);

		assertIt(n, "[b]", "a.b.??");

		assertIt(n, "[99, lala]", "a.*. filter( ??.match('b|c') )");

		
	}
	
	@Test
	@Ignore
	public void testVarsUnif() throws Exception {
		
		Node n = w_("{\n"
				+ "   \"name\": \"Miller\",\n"
				+ "   \"age\": 17,\n"
				+ "   \"driverLic\": 3,\n"
				+ "   \"status\": \"hidden\",\n"
				+ "   \"address\": {\n"
				+ "      \"postal-code\": 11111,\n"
				+ "      \"city\": \"PleasantVille\",\n"
				+ "      \"absent\": false\n"
				+ "   },\n"
				+ "   \"telecom\": [\n"
				+ "      {\n"
				+ "         \"use\": \"hidden\",\n"
				+ "         \"value\": \"(03) 5555 0000\"\n"
				+ "      },\n"
				+ "      {\n"
				+ "         \"use\": \"home\",\n"
				+ "         \"value\": \"(03) 5555 1111\",\n"
				+ "         \"kind\": \"smart\"\n"
				+ "      }\n"
				+ "   ],\n"
				+ "   \"shopping\": [\n"
				+ "      {\"p#123\": {\"status\": \"ordered\"}},\n"
				+ "      {\"p#456\": {\"status\": \"shipped\"}}\n"
				+ "   ]\n"
				+ "}");
		
		try {
			assertIt(n, "[LinkedHashMap((s, ^`status`->hidden))]", " ?(status $s).telecom.*.use $s ");
			
//		n.ctx.clearVars();
			assertIt(n, "[^`status`->hidden]", " ?(status$s).$s ");
			
//		n.ctx.clearVars();
			assertIt(n, "[hidden]", " ?(status $s).telecom.*.use $s ");
			
		} catch (JapathException e) {
			// so far no unif. allowed
			System.out.println(e);
		}
	}


	@Test
	public void testVars1() {

		String jo = "{\n"
				+ "	\"a\": [\n"
				+ "		{\n"
				+ "			\"x\": 1\n"
				+ "		},\n"
				+ "		{\n"
				+ "			\"x\": 1,\n"
				+ "			\"y\": 2\n"
				+ "		},\n"
				+ "		{\n"
				+ "			\"x\": 1\n"
				+ "		}\n"
				+ "	]\n"
				+ "}";
		
		Node n = w_(jo);
		
		assertIt(n, "[[{\"x\":1},{\"x\":1,\"y\":2},{\"x\":1}] | (y, ^`y`->2)]", "a ?(* ?(??.eq('1')) .y$_)", true);

	}
	
	@Test
	public void testArrays() {
		
		String jo = "{a: [ [ 1, 2, 3] ] }";
		
		Node n = w_(jo);
		
		assertIt(n, "[1]", "a[0][0]");
		
		assertIt(n, "[2, 3]", "a.*.*[#1..]");
		
		assertIt(n, "[1, 2]", "a.*.*[#0..1]");
		
		assertIt(n, "[3]", "a.*.*[#2..5]");
		
	}
	
	@Test
//	@Ignore
	public void testXml() {
		
		Element root = Jsoup.parse("<root>\n"
				+ "    <a c=\"lolo\">\n"
				+ "        <b>\n"
				+ "            lala\n"
				+ "        </b>\n"
				+ "        <c1>\n"
				+ "            lolo\n"
				+ "        </c1>\n"
				+ "        <b>\n"
				+ "            lili\n"
				+ "        </b>\n"
				+ "    </a>\n"
				+ "</root>", "", Parser.xmlParser()).root();
		
		Node n = WJsoup.w_(root);
		
		assertIt(n, "[lala, lili]", "root.a ?(and( c.eq('lolo'), c1.text().eq('lolo') )).b.text()");
		
		assertIt(n, "[<b> lala </b>]", "root.a[0]");
		
		assertIt(n, "[b, c1, b]", "root.a.*.??");
		
		assertIt(n, "[lolo]", "root.a.* ?(??.match('c.*')).text()");
		
		assertIt(n, "[lala, lili]", "root.**.a.b.text()");
		
		assertIt(n, "[lili]", "root.a.b[#1].text()");
		
	}

	@Test
//	@Ignore
	public void testPouHtml() throws Exception {
		
		Element root = Jsoup.parse(new File("src/test/resources/japath3/core/source.html"), "utf-8").root();
		Node n = WJsoup.w_(root);
		
		assertIt(n, usr, 
				"html.**.article ?(class.eq('account'))"
				+ ".dl.and("
				+ "   _[1].kbd.text() $username,"
				+ "   _[3].kbd.text() $passwd"
				+ ")"
				, true, false
				);

		n.ctx.clearVars();
		assertIt(n, usr, 
				"html.**.article ?(class.eq('account'))"
						+ ".dl.and("
						+ "   dd[#0].kbd.text() $username," // dd[0] instead of [1]
						+ "   dd[#1].kbd.text() $passwd"
						+ ")"
						, true, false
				);

	}
	
	@Test
//	@Ignore
	public void testParamExpr() throws Exception {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		assertIt(n, "[lala]", "def(bcond, cond(#0, #1, #2)) .a.bcond(b, b, c)");
		
		assertIt(n, "[false, lala]", "def(all, *) .a.all()");

		assertIt(n, "[99]", "def(g, #0) .def(f, g(#0)) .a.f(99)");
		
		// recursion
		try {
			assertIt(n, "", "def(f, f()) .a.f()");
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}

		// TODO not intended !
		assertIt(n, "[lala]", "def(bcond, a.cond(#0, #1, #2)) .bcond(b, b, c)");
		
		
	}
	
	@Test public void testModular() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		Env env = new Env();
//		PathExpr e = e_("def(bcond, cond(#0, #1, #2)) ");
		PathExpr e = e_(env, "def(bcond, cond(#0, #1, #2)) ", false, false);
				
		PathExpr e_ = e_(env, "a.bcond(b, b, c)", false, false);
		
		Object val = select(n, e, e_).val();
		
		assertEquals("lala", val.toString());
	}



	public static String cr(String s) { return s.replace("\r", ""); }
	
	public static void assertIt(Node n, String exp, String expr) { assertIt(n, exp, expr, false); }

	public static void assertIt(Node n, String exp, String expr, boolean vars) { assertIt(n, exp, expr, vars, false); }

	public static void assertIt(Node n, String exp, String expr, boolean vars, boolean printOnly) {
		
		assertIt(n, exp, e_(expr), vars, printOnly);
	}
	
	public static void assertIt(Node n, String exp, Expr expr, boolean vars, boolean printOnly) {
		String s = walks(n, expr).map(x -> {
			return x.val().toString() + (vars ? " | " + n.ctx.getVars() : "");
		}).collect(toList()).toString();
		
		if (printOnly) {
			System.out.println(s);
		} else {
			assertEquals(exp.replace("\r", ""), s);
		}
	}
	
	
	String usr = "[true "
			+ "| (username, ^ybzftxdeldjltyjgni@ttirv.net),(passwd, ^BugMeNot123), true "
			+ "| (username, ^bbc@mailnesia.com),(passwd, ^bbc@mailnesia.com)]";
}
