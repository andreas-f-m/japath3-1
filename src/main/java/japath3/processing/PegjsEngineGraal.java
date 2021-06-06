package japath3.processing;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.json.JSONException;
import org.json.JSONObject;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import japath3.core.JapathException;

public class PegjsEngineGraal {

	Context cx = Context.create();

	public PegjsEngineGraal() {

		try {
			cx.eval(
					Source.newBuilder("js", new InputStreamReader(Language.class.getResourceAsStream("stringify.js")), "str")
							.build());

		} catch (Exception e) {
			throw new JapathException(e);
		}
	}

	public PegjsEngineGraal(Reader js) {

		this();
		try {
			cx.eval(Source.newBuilder("js", js, "pjs").build());

		} catch (Exception e) {
			throw new JapathException(e);
		}
	}

	public PegjsEngineGraal(String js) {
		this(new StringReader(js));
	}

	public Tuple2<JSONObject, String> getAst(String text) {

		String astStr;
		try {
			Value func = cx.getBindings("js").getMember("peg$parse");
			if (func == null) throw new JapathException("fatal error eval pegjs");
			
			Value v = func.execute(text);
			astStr = v.toString();

		} catch (PolyglotException e) {

			try {
				JSONObject error = new JSONObject(e.getGuestObject().toString());
				JSONObject loc = error.getJSONObject("location");
				return Tuple.of(null,
						"japath syntax error at line " + loc.getJSONObject("start").getInt("line")
						+ ", column "
						+ loc.getJSONObject("start").getInt("column")
						+ ": "
						+ error.get("message"));
				
			} catch (JSONException e1) {
				throw new JapathException(e1);
			}
		} 
		return Tuple.of(new JSONObject(astStr), null);
	}
}
