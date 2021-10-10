package japath3.processing;

import static japath3.processing.Language.e_;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import io.vavr.control.Option;
import japath3.core.Japath.Expr;
import japath3.core.Japath.ParamExprDef;
import japath3.core.JapathException;
import japath3.processing.Language.Env;

public class Module {

	private String name;
	private Env env;
	
	public Module(String name, String pathExprStr, boolean isSchemaModule) {
		init(name, pathExprStr, isSchemaModule);
	}

	public Module(String name, String pathExprStr) {

		init(name, pathExprStr, false);
	}

	public Module(String name, InputStream pathExprStr) {
		try {
			init(name, IOUtils.toString(pathExprStr, "utf-8"), false);
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}
	
	private Module init(String name, String pathExprStr, boolean isSchemaModule) {

		this.name = name;

		env = new Env();
		e_(env, pathExprStr, isSchemaModule, true);
		return this;
	}

	public Expr getExpr(String exprName) {
		
		Option<ParamExprDef> ped = env.defs.get(exprName);
		if (ped.isEmpty()) throw new JapathException("expression '" + exprName + "' not defined in module '" + name + "'");
		return ped.get().exprs[0];
	}
}
