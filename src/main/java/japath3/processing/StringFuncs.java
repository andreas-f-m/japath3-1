package japath3.processing;

import io.vavr.collection.Iterator;
import io.vavr.collection.List;
import japath3.core.Japath;
import japath3.core.Japath.NodeIter;
import japath3.core.JapathException;
import japath3.core.Node;

public class StringFuncs {

	public String conc(String s1, String s2) { return s1 + s2; }

	public NodeIter split(Node ctxNode, NodeIter[] niRegex) {

		if (niRegex.length == 0 || niRegex.length > 1) throw new JapathException("split must have exactly one argument");

		String input = ctxNode.val().toString();
		String regex = niRegex[0].val().toString();
		
		List<String> filter = List.of(input.split(regex)).map(s -> {
			return s.trim();
		}).filter(s -> {
			return !s.equals("");
		});
		Iterator<String> it = filter.iterator();

		return new NodeIter() {
			@Override public boolean hasNext() { return it.hasNext(); }

			@Override public Node next() { return new Node.DefaultNode(it.next(), ctxNode.ctx); }
		};

	}

	public NodeIter clean(Node ctxNode, NodeIter[] niRegex) {
		
		if (niRegex.length == 0 || niRegex.length > 1) throw new JapathException("clean must have exactly one argument");
		
		String regex = niRegex[0].val().toString();
		String input = ctxNode.val().toString().trim();
		
		String cleaned = "";
		input =  input.replaceAll(regex, "\0");
		for (int i = 0; i < input.length(); i++) {
			if (input.charAt(i) != '\0') cleaned += input.charAt(i); 
		}
		
		return Japath.singleObject(cleaned, ctxNode);
	}
	
	public NodeIter replace(Node ctxNode, NodeIter[] nits) {
		
		if (nits.length == 0 || nits.length > 2) throw new JapathException("replace must have exactly two arguments");

		String val = ctxNode.val().toString();
		String regex = nits[0].val().toString();
		String repl = nits[1].val().toString();
		
		return Japath.singleObject(val.replaceAll(regex, repl),
				ctxNode);
	}
	
}
