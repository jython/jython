package org.python.antlr.adapter;

/**
 * AstAdapters turn Objects into Ast nodes.
 */
public interface AstAdapter {

	/**
	 * @return Ast node version of o.
	 */
	public abstract Object adapt(Object o);

}
