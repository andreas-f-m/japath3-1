package japath3.processing;

import java.io.Reader;
import java.io.StringReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import japath3.core.JapathException;

public class EngineGraal {

	Context cx;

	public EngineGraal() {

		try {
			cx = Context.newBuilder("js").allowIO(true).allowAllAccess(true).build();
		} catch (Exception e) {
			throw new JapathException(e);
		}
	}
	
	public EngineGraal eval(Reader js, String name) {	
		try {
			cx.eval(Source.newBuilder("js", js, name).build());
//			cx.eval(Source.newBuilder("js", js, name + ".mjs").build());

		} catch (Exception e) {
			throw new JapathException(e);
		}
		return this;
	}
	
	public EngineGraal eval(String js, String name) {
		
		return eval(new StringReader(js), name);
	}
	
	public Value exec(String f, Object... args) {

		synchronized (this) { // TODO no shared context in graal possible
										// (->thread local)
			try {
				cx.enter();
				Value func = cx.getBindings("js").getMember(f);
				if (func == null) throw new JapathException("js func '" + f + "' does not exists");
				
				Value v = func.execute(args);
				
				return v;

			} catch (Exception e) {
				throw new JapathException(e);
			} finally {
				cx.leave();
			}
		}
	}
	
}
