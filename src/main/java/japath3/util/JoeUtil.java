package japath3.util;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import japath3.core.JapathException;

/**
 * json order extension: enables insert-order and provides custom parsing for org.json.   
 *
 */
public class JoeUtil {

	public static class Joe extends JSONObject {

		private static Field dmap;
		
		static {
			try {
			dmap = JSONObject.class.getDeclaredField("map");
			dmap.setAccessible(true);
			
		} catch (IllegalArgumentException | NoSuchFieldException | SecurityException e) {
			throw new JapathException(e);
		}
			
		}

		public Joe() {

			try {
//				boolean accessible = dmap.isAccessible();
				dmap.set(this, new LinkedHashMap());
			} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
				throw new JapathException(e);
			}
		}

		public Joe(JSONTokener x) {
			this();
			if (x != null) parse(x);
		}

		// we copied fron org.json; here we have the chance to extend
		public Joe parse(JSONTokener x) {

			char c;
			String key;

			if (x.nextClean() != '{') {
				throw x.syntaxError("A JSONObject text must begin with '{'");
			}
			for (;;) {
				c = x.nextClean();
				switch (c) {
				case 0:
					throw x.syntaxError("A JSONObject text must end with '}'");
				case '}':
					return this;
				default:
					x.back();
					key = x.nextValue().toString();
				}

				// The key is followed by ':'.

				c = x.nextClean();
				if (c != ':') {
					throw x.syntaxError("Expected a ':' after a key");
				}

				// Use syntaxError(..) to include error location

				if (key != null) {
					// Check if key exists
					if (opt(key) != null) {
						// key already exists
						throw x.syntaxError("Duplicate key \"" + key + "\"");
					}
					// Only add value if non-null
					Object value = x.nextValue();
					if (value != null) {
						put(key, value);
					}
				}

				// Pairs are separated by ','.

				switch (x.nextClean()) {
				case ';':
				case ',':
					if (x.nextClean() == '}') {
						return this;
					}
					x.back();
					break;
				case '}':
					return this;
				default:
					throw x.syntaxError("Expected a ',' or '}'");
				}
			}
		}
	}

	private static Field eof;

	static class AdaptedTokener extends JSONTokener {
		
		

		public AdaptedTokener(InputStream inputStream) {
			super(inputStream);
			eof();
		}

		public AdaptedTokener(String s) {
			super(s);
			eof();
		}

		private void eof() {
			try {
				eof = JSONTokener.class.getDeclaredField("eof");
			} catch (NoSuchFieldException | SecurityException e) {
				throw new JapathException(e);
			}
			eof.setAccessible(true);
		}

		// we copied fron org.json; here we have the chance to extend
		@Override
		public Object nextValue() throws JSONException {

			char c = this.nextClean();
			String string;

			switch (c) {
			case '"':
			case '\'':
				return this.nextString(c);
			case '{':
				this.back();
				return new Joe(this);
			case '[':
				this.back();
				return new JSONArray(this);
			}

			/*
			 * Handle unquoted text. This could be the values true, false, or null,
			 * or it can be a number. An implementation (such as this one) is
			 * allowed to also accept non-standard forms.
			 *
			 * Accumulate characters until we reach the end of the text or a
			 * formatting character.
			 */

			StringBuilder sb = new StringBuilder();
			while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
				sb.append(c);
				c = this.next();
			}
			try {
				if (!eof.getBoolean(this)) {
					this.back();
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new JapathException(e);
//				e.printStackTrace();
			}

			string = sb.toString().trim();
			if ("".equals(string)) {
				throw this.syntaxError("Missing value");
			}
			return JSONObject.stringToValue(string);
		}
		
		@Override public char nextClean() throws JSONException { 
	        for (;;) {
	            char c = this.next();
	            if (c == '/') {
	            	c = this.next();
	            	if (c == '/') {
	            		while ((c = this.next()) != '\n');
	            	}
					}
	            if (c == 0 || c > ' ') {
	                return c;
	            }
	        }
		}

	}

	public static JSONObject createJoe() {
		return new Joe();
	}
	
	public static JSONObject createJoe(String txt) {
		return new Joe(new AdaptedTokener(txt));
	}
	
	public static JSONObject createJoe(InputStream inputStream) {
		return new Joe(new AdaptedTokener(inputStream));
	}
	
	public static Object copy(Object o) {
		
		return o instanceof JSONObject ? new JSONObject(o.toString())
				: o instanceof JSONArray ? new JSONArray(o.toString()) : o;
	}

	public static String prettyString(Object o, int indent) {

		return o instanceof JSONObject ? ((JSONObject) o).toString(indent)
				: o instanceof JSONArray ? ((JSONArray) o).toString(indent) : o.toString();
	}
}
