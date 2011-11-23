/**
 * 
 */
package com.github.glue.httpq;

import com.github.glue.httpq.transport.Session;

/**
 * @author eric
 *
 */
public interface Context {
	
	public Session getSession(String sid);
	
}
