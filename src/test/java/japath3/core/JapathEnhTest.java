package japath3.core;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static japath3.core.Japath.__;
import static japath3.core.Japath.assign;
import static japath3.core.Japath.bind_;
import static japath3.core.Japath.c_;
import static japath3.core.Japath.create;
import static japath3.core.Japath.empty;
import static japath3.core.Japath.p_;
import static japath3.core.Japath.select;
import static japath3.core.Japath.single;
import static japath3.core.Japath.subExpr;
import static japath3.core.Japath.varAppl;
import static japath3.core.JapathTest.assertIt;
import static japath3.core.JapathTest.cr;
import static japath3.processing.Language.e_;
import static japath3.wrapper.WJsonOrg.w_;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import io.vavr.Tuple;
import japath3.core.Japath.Expr;
import japath3.core.Japath.NodeIter;
import japath3.core.Japath.PathExpr;
import japath3.core.Japath.Selection;
import japath3.processing.Language;

public class JapathEnhTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void testNew() { 
		
		Node n = w_(new JSONObject(" {a: {b: false, c: 'lala'} }  "));
		
		assertIt(n, "[lala | (x, ^``->undef)]", "new$x.$.a.c", true);
		
		n.ctx.clearVars();
		try {
			assertIt(n, "[lala | (x, ^``->undef)]", 
					"   def(do, ?(#0)). "
							+ " do(new $x). a.c", true, true);
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}

		
		n.ctx.clearVars();
		assertIt(n, "[lala | (x, ^``->undef)]", 
				"   def(do, ?(#0)). "
						+ " do(new $x: true). a.c", true, true);
	}
	
	@Test public void testNull() {
		
		Node n = w_(new JSONObject(" {a: {b: null, c: [null]} }  "));
		
		try {
			assertIt(n, "", "a.b.type('hi')");
			fail();
		} catch (JapathException e) {
			// ok
		}
		
		assertIt(n, "[true]", "a.b.eq(null)");
		
		assertIt(n, "[false]", "a.b.neq(null)");
		
		assertIt(n, "[false]", "a.c[0].neq(null)");
		
		try {
			assertIt(n, "[false]", "a.c[0].gt(null)");
			fail();
		} catch (JapathException e) {
			// ok
		}
		
		assertIt(n, "[]", "a.b.x");
		
		assertIt(n, "[{\"a\":null}]", "{a: null}");
	}

	
	@Test 
//	@Ignore // TODO deferred
	public void testModify() { 
		
		Node n = w_(new JSONObject(" {a: {b: false, c: 'lala'} }  "));
		
		String exp = "[{\"a\":{\"b\":99,\"c\":\"lala\"}} | ]";
		
		assertIt(n, exp, "::modifiable.a{b:99}.$", true);
		assertIt(n, exp, "::modifiable{ cond(a, a).b:99}.$", true); // TODO

		try {
			assertIt(n, "", "_{a.::modifiable}.a{b:99}.$", true);
			fail();
		} catch (Exception e) {
			System.out.println(e);
		}
		
		n = w_(new JSONObject(" {a: {b: false, c: [2,3,4]} }  ")).setConstruct(true);
		assertIt(n, "[{\"a\":{\"b\":false,\"c\":[2,99,4]}} | ]", "a{c[1]:(99)}.$", true);
		
		n = w_(new JSONArray("[2,3,4]"));
		
		assertIt(n, "[[2,99,4] | ]", "::modifiable._{_[1]:(99)}.$", true);
		
		n = w_(new JSONObject(" {a: {b: {b1: 88}, c: 'lala'} }  "));
		
		n.ctx.setSalient(true);
		assertIt(n, "[{\"a\":{\"b\":{\"b1\":88},\"c\":{\"b1\":88}}} | ]", "::modifiable.a{c:(b)}.$", true);
		
		n = w_(new JSONObject(" {a: {b: {b1: 88}, c: 'lala'} }  "));
		
//		try {
//			// recursion
			assertIt(n, "[{\"a\":{\"b\":{\"b1\":88},\"c\":{\"b\":{\"b1\":88},\"c\":\"lala\"}}} | ]", "::modifiable.a{c:$.a}.$", true);
//			fail();
//		} catch (Exception e) {
//		}
		
		n = w_(new JSONObject(" {a: [null, null] }  "));
		
		assertIt(n, "[{\"a\":[99,99]} | ]", "::modifiable._{a.* : 99}.$", true);

		n = w_(new JSONObject(" {a: {b: false, c: 'lala'} }  "));
		
		assertIt(n, "[{\"b\":99,\"c\":\"lala\",\"d\":88} | ]", "::modifiable.a{b:99, d:88}", true);
	}
	
	@Test 
//	@Ignore // TODO deferred
	public void testModifyRaw() {
		
		Node n = w_(new JSONObject(" {a: {b: false, c: 'lala'} }  ")).setConstruct(false);
		
		// "a{b=(99)}.$"
		JapathTest.assertIt(n, "[{\"a\":{\"b\":99,\"c\":\"lala\"}} | ]", 
				
				p_(Japath.javaCall("directive", "", "modifiable", null),
				__("a"), subExpr(assign(p_(__("b")), c_(99))), varAppl("root")),
				
				true, false);
	}
	
	@Test public void testConstruct() {
		
		Node n = w_(new JSONObject(" {a: {b: false, c: 'lala'} }  "));
		
		assertIt(n, "[{\"a\":{\"b\":99}} | (x, ^``->{\"a\": {\"b\": 99}})]", "new $x :{a.b:99}", true);
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":[null,{\"b\":99}]} | (x, ^``->{\"a\": [\r\n"
				+ "   null,\r\n"
				+ "   {\"b\": 99}\r\n"
				+ "]})]", 
				
				"new $x{a[1].b:(99)}. $x", true);
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":{\"b\":[null,99]}} | (x, ^``->{\"a\": {\"b\": [\r\n"
				+ "   null,\r\n"
				+ "   99\r\n"
				+ "]}})]", 
				
				"new $x{a.b[1]:(99)}", true);
		
		n.ctx.clearVars();
		assertIt(n, "[[null,99] | (x, ^``->[\r\n"
				+ "   null,\r\n"
				+ "   99\r\n"
				+ "])]", 
				
				"new $x{_[1]:(99)}. $x", true);
		
		n.ctx.clearVars();
		String expr1 = "new { a : new{b : 99, b1 : {\"c1\" : 88}}, c : union(1, 2, new{\"d\" : \"lala\"}) }";
		Expr expr = e_(expr1);
		System.out.println(Language.stringify(expr, 1));
		assertIt(n, "[{\"a\":{\"b\":99,\"b1\":{\"c1\":88}},\"c\":[1,2,{\"d\":\"lala\"}]}]", 
				
				expr, false, false);
	}
	
	@Test public void testConstruct1() {
		
		Node n = w_(new JSONObject(" {a: {b: false, c: 'lala'} }  "));
		
		n.ctx.clearVars();

		assertIt(n, "[{\"a\":{\"b\":{\"c\":\"lala\"},\"d\":99}}]", 
				
				"new $x:{"
				+ "  a: {"
				+ "   b: {c:'lala'},"
				+ "   d : 99"
				+ "}"
				+ "}");
		
		n.ctx.clearVars();

		assertIt(n, "[{\"a\":{\"b\":{\"c\":\"lala\"},\"d\":[null,88,77]}}]", 
				
				"new $x:{"
				+ "  a: {"
				+ "   b: {c:'lala'},"
				+ "   d[1] : 88,"
				+ "   d[2] : 77"
				+ "}"
				+ "}");
		
		n.ctx.clearVars();
		
		assertIt(n, "[{\"a\":[1,2,3]}]", "new $x{a:union(1, 2, 3)}");
		
		n = w_(new JSONObject(" {a: {b: [7, 8, 9], c: 'lala'} }  "));
		
		assertIt(n, "[{\"a\":[7,8,9]}]", "new $x{a:$.a.b}");
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":[7,8,9]} | (x, ^``->{\"a\": [\r\n"
				+ "   7,\r\n"
				+ "   8,\r\n"
				+ "   9\r\n"
				+ "]})]", "new $x:{a:a.b}", true);
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":[7,8,9]}]", "new $x{a:$.a.b.*}");
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":{\"b\":[7,8,9],\"c\":\"lala\"}}]", "new $x:{a:cond(true, a)}");
		
		n.ctx.clearVars();
		assertIt(n, "[{\"b\":88}]", "new $x{cond(true, b):88}");
		
		n.ctx.clearVars();
		try {
			assertIt(n, "", "new $x{cond(a.eq(1), b):88}");
			fail();
		} catch (JapathException e) {
			assertEquals("operation on undef node '`a`->undef' (assign value first)",
					e.getMessage());
		}

		n.ctx.clearVars();
		assertIt(n, "[{\"a\":1,\"b\":88}]", "new $x{a:1, cond(a.eq(1), b):88}"); // TODO really OK?
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":1,\"c\":88}]", "new $x{a:1, cond(a.eq(2), b, c):88}"); 

		n.ctx.clearVars();
		try {
			assertIt(n, "", "new $x{`.*`:88}");
			fail();
		} catch (JapathException e) {
			assertEquals("operation on undef node '``->undef' (assign value first)",
					e.getMessage());
		}
		
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":[99]}]", "new $x:{a:[99]}");
		
		n.ctx.clearVars();
		try {
			assertIt(n, "", "new $x:{cond(a, a):99}");
			fail();
		} catch (JapathException e) {
			assertEquals("operation on undef node '`a`->undef' (assign value first)",
					e.getMessage());
		}
		
		n.ctx.clearVars();
		try {
			assertIt(n, "", "new $x:{a:b}");
			fail();
		} catch (JapathException e) {
			assertEquals("rhs has undef value (selection: 'b')",
					e.getMessage());
		}
	}
	
	@Test public void testConstruct2() {
		
		Node n = w_(new JSONObject(" {a: {b: false, c: 'lala'} }  "));
		
		n.ctx.clearVars();

//		try { TODO check exists
			assertIt(n, "[{\"a\":2}]", "new : {a:1, a:2}");
//			fail();
//		} catch (JapathException e) {
//			assertEquals("'a' already set: ``->{\"a\": 1}",
//					e.getMessage());
//		}
			
		n.ctx.clearVars();
		try {
			assertIt(n, "", "new: {a:b}");
			fail();
		} catch (JapathException e) {
		}

		n.ctx.clearVars();
		try {
			assertIt(n, "", "new {a:b}");
			fail();
		} catch (JapathException e) {
		}
		
	}
	
	@Test public void testConstructJsonSynt() {
		
		Node n = w_(new JSONObject(" {a: {b: false, c: 'lala'} }  "));
		
		n.ctx.clearVars();
		
		assertIt(n, "[{\"a\":{\"b\":{\"c\":\"lala\"},\"d\":99,\"e\":[1,2,\"la\",{\"e1\":9},88]}}]", 
				
				"{"
						+ "  \"a\" : {"
						+ "   \"b\" : {c:'lala'},"
						+ "   \"d\" : 99,"
						+ "   e: [1, 2, \"la\", [{e1:9}, 88] ]"
						+ "  }"
						+ "}");
		
		n.ctx.clearVars();
		
		assertIt(n, "[{\"a\":{\"b\":{\"c\":\"lala\"},\"d\":[null,88,77]}}]", 
				
				"{"
						+ "  \"a\" : {"
						+ "   `b` :  { c:'lala'},"
						+ "   d[1] : 88,"
						+ "   d[2] : 77"
						+ "  }"
						+ "}");
		
		n.ctx.clearVars();
		
		assertIt(n, "[17]", 
				
				"{ \"pers\": {\r\n"
				+ "    \"name\": \"Miller\",\r\n"
				+ "    \"age\": 17\r\n"
				+ "}}.pers.age");
		
		
		n.ctx.clearVars();
		try {
			assertIt(n, "[17]", 
					
					"{ \"pers\": {\r\n"
							+ "    \"name\": \"Miller\",\r\n"
							+ "    \"age\": 17\r\n"
							+ "}}.x");
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}

		
	}
	
	@Test public void testConstructMix() {
		
		Node n = w_(new JSONObject("{\r\n"
				+ "    \"text\": \"lolo lili lolo lili \",\r\n"
				+ "    \"name\": {\r\n"
				+ "        \"first\": \"john\",\r\n"
				+ "        \"last\": \"miller\"\r\n"
				+ "    },\r\n"
				+ "    \"age\": 99\r\n"
				+ "}"));
		
		PathExpr e = e_("new $x{text:$.text, name:$.name, age:$.age}");
		
		System.out.println(e);
		
		@SuppressWarnings("unused")
		Var x = Var.of();
		PathExpr path = p_(create,
				bind_("x"),
				subExpr(assign(p_(__("text")), p_(varAppl("root"), __("text"))),
						assign(p_(__("name")), 
								select(n, e_("name.last")).val() + ", " + select(n, e_("name.first")).val() 
								),
						assign(p_(__("age")), p_(varAppl("root"), __("age")))));
		
		select(n, path);
//		select(n, e);
//		System.out.println(n.ctx.getVars());
		assertEquals( cr("(x, ^``->{\r\n"
				+ "   \"name\": \"miller, john\",\r\n"
				+ "   \"text\": \"lolo lili lolo lili \",\r\n"
				+ "   \"age\": 99\r\n"
				+ "})") , n.ctx.getVars().toString());
		
		
	}
	
	@Test public void testJavaCall() {
		
		final class Xxx {
			@SuppressWarnings("unused") // here in scope
			public NodeIter f(Node ctxNode, NodeIter[] nits) {
				
				return nits[0].hasNext() ? single(nits[0].next()) : empty;
				
			}
			@SuppressWarnings("unused") // here in scope
			public NodeIter g(Node ctxNode) {
				
				return single(ctxNode);
				
			}
			@SuppressWarnings("unused")
			public String conc(String s1, String s2) {
				return s1 + s2;
			}
		}
		
		Node n = w_(new JSONObject(" {a: {b: false, c: 'lala', d: 'lolo'} }  "));		
		
		Ctx.putJInst("m", new Xxx());
		assertIt(n, "[lala]", "j::m::f(a.c)");
		assertIt(n, "[false]", "j::m::f(a.b)");

		assertIt(n, "[lala]", "a.c.j::m::g()"); 

		assertIt(n, "[lalalolo]", "j::m::conc(a.c, a.d)");

		// predef std func
		assertIt(n, "[lalalolo]", "str::conc(a.c, a.d)");

		assertIt(n, "[]", "j::m::conc(a.c, a.x)");
		
		
		Ctx.putJInst(Tuple.of("x", (Expr) x -> {
			return single(x);
		}));
		
		assertIt(n, "[lala]", "a.c.j::x::eval()");
		
		try {
			assertIt(n, "[lala]", "a.type('hi')");
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}
		
		try {
			assertIt(n, "[lala]", "a.type()");
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}
	}
	
	@Test public void testLhsRhs() {
		
		String e = "_{x:y,z,new $x:{"
				+ "  a: {"
				+ "   b: {c:'lala'},"
				+ "   d : 99"
				+ "}"
				+ "}}";
		
		PathExpr expr = e_(e);
		
		StringBuilder sb = new StringBuilder();
		expr.visit(
				(x, pre) -> {
					if (x instanceof Selection) 
						sb.append(pre + ">>> " + ((Selection) x).scope + " >>> " + x + "\r\n");
				}
				);
		assertEquals("true>>> lhs >>> __(\"x\")\r\n"
				+ "true>>> rhs >>> __(\"y\")\r\n"
				+ "true>>> none >>> __(\"z\")\r\n"
				+ "true>>> lhs >>> __(\"a\")\r\n"
				+ "true>>> lhs >>> __(\"b\")\r\n"
				+ "true>>> lhs >>> __(\"c\")\r\n"
				+ "true>>> lhs >>> __(\"d\")\r\n"
				+ "", sb.toString());
		
	}
}
