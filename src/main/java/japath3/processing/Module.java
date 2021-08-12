package japath3.processing;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import static japath3.processing.Language.e_;

import io.vavr.control.Option;
import japath3.core.Japath;
import japath3.core.Japath.ParamExprDef;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.processing.Language.Env;

public class Module {

	private String name;
	private Env env;

	public Module(String name, String pathExprStr) {

		init(name, pathExprStr);
	}

	public Module(String name, InputStream pathExprStr) {
		try {
			init(name, IOUtils.toString(pathExprStr, "utf-8"));
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}
	
	private void init(String name, String pathExprStr) {

		this.name = name;

		env = new Env();
		e_(env, pathExprStr, false, true);
	}

	public Node select(Node n, String exprName) {

		Option<ParamExprDef> ped = env.defs.get(exprName);
		if (ped.isEmpty()) throw new JapathException("expression '" + exprName + "' not defined in module '" + name + "'");
		return Japath.select(n, ped.get().exprs[0]);
	}
}
