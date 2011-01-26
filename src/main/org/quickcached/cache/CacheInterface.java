/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.quickcached.cache;

/**
 *
 * @author akshath
 */
public interface CacheInterface {
	public void set(Object key, Object value, long expInSec);

	public Object get(Object key);
	
	public Object delete(Object key);

	public void flush();
}
