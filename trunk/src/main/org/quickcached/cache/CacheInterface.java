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
	
	public String getName();
	
	public void set(String key, Object value, int objectSize, long expInSec);
	public boolean touch(String key, long expInSec);
	public void update(String key, Object value, int objectSize);

	public Object get(String key);
	
	public boolean delete(String key);

	public void flush();

	/**
	 *
	 * @return Map with key as curr_items, total_items, cmd_get, cmd_set,
	 *  get_hits, get_misses, delete_misses, delete_hits, evictions, expired
	 */
	public void saveStats(Map map);
	
	public boolean saveToDisk();
	
	public boolean readFromDisk();
}
