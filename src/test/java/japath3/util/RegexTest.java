package japath3.util;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RegexTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void doNoTest() { 
		
//		String[] def = 

		String[] x = Regex.multiExtract(" 12.44. xxxx ", "((\\d|\\.|\\s)*)(.+)");
		System.out.println(Arrays.asList(x));
		
		x = Regex.multiExtract("12.44. xxxx ", "((\\d|\\.|\\s)*)(.*)");
		System.out.println(Arrays.asList(x));
		
		String y = Regex.extract("12.44. xxxx ", "(?:(?:\\d|\\.|\\s)*)(.*)", null);
		System.out.println(Arrays.asList(y));
		
		x = Regex.multiExtract("1 xxxx", "((\\d|\\.|\\s)*)(.*)");
		System.out.println(Arrays.asList(x));
		
	}

}
