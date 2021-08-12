package japath3.processing;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static japath3.wrapper.WJsonOrg.w_;
import static org.junit.Assert.assertEquals;

import japath3.core.Node;

public class ModuleTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void test() { 
		
		String s = "def(bcond, a.cond(b, b, c)) ";
		
		Module m = new Module("test", s);
		
		Node n = w_(" {a: {b: false, c: 'lala', d: 'lolo'} }  ");

		assertEquals("lala", m.select(n, "bcond").val());
		
		// deferred TODO
//		assertEquals("lala", m.select(n, "a.bcond(b, b, c)").val());
//		assertEquals("lolo", m.select(n, "a.bcond(not(b), d, c)").val());
	}
	
	// deferred
	
//	public static void main(String[] args) {
//		
//		String s = "def(bcond, cond(#0, #1, #2)) ";
//		
//		Module m = new Module(s);
//		
//		Node n = w_(" {a: {b: false, c: 'lala', d: 'lolo'} }  ");
//
//
//		
//		new Thread(new Runnable() {
//		    @Override
//		    public void run() {
//		   	 assertEquals("lala", m.select(n, "a.bcond(b, b, c)").val());
//		    }
//		}).start();
//		
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				assertEquals("lolo", m.select(n, "a.bcond(not(b), d, c)").val());
//			}
//		}).start();
//		
//	}

}
