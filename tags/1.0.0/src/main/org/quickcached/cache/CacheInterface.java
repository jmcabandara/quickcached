/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.quickcached.cache;

import java.util.Map;

/**
 *
 * @author akshath
 */
public interface CacheInterface {
	public void set(Object key, Object value, long expInSec);

	public Object get(Object key);
	
	public Object delete(Object key);

	public void flush();

	/**
	 *
	 * @return Map with key as curr_items, total_items, cmd_get, cmd_set,
	 *  get_hits, get_misses, delete_misses, delete_hits
	 */
	public Map getStats();
}
