package japath3.util;

import java.util.Iterator;

import japath3.core.JapathException;

public class Splitter<T> implements Iterable<T> {

	@FunctionalInterface
	public static interface F<T> {

		public Iterator<T> apply(int offset, int limit);
	}

	// SolrQuery query;
	private int pageSize;
	private int currOffset;
	private Iterator<T> currIter;

	F<T> pageFunc;

	public Splitter(int pageSize) { this.pageSize = pageSize; }

	private boolean forward(boolean ini) {

		if (!ini) currOffset += pageSize;
		try {
			currIter = pageFunc.apply(currOffset, pageSize);
			return currIter.hasNext();
		} catch (Exception e) {
			throw new JapathException(e);
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {

			@Override
			public boolean hasNext() {

				boolean hasNext = currIter.hasNext();
				if (!hasNext) hasNext = forward(false);
				return hasNext;
			}

			@Override
			public T next() { return currIter.next(); }

		};
	}

	public Splitter<T> setPageFunc(F<T> func) {
		this.pageFunc = func;
		currOffset = 0;
		forward(true);
		return this;
	}
}
