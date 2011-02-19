package org.quickcached.cache;

import com.whirlycott.cache.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * WhirlycottCache based implementation
 * @author akshath
 */
public class WhirlycottCacheImpl implements CacheInterface {
	private static final Logger logger = Logger.getLogger(WhirlycottCacheImpl.class.getName());

	private Cache cache = null;

	private long totalItems;
	private long cmdGets;
	private long cmdSets;
	private long cmdFlushs;
	private long getHits;
	private long getMisses;
	private long deleteMisses;
	private long deleteHits;

	
	public WhirlycottCacheImpl() {
		try {
			cache = CacheManager.getInstance().getCache();
		} catch(Exception e) {
			logger.severe("Error: "+e);
		}
	}

	public Map getStats() {
		Map stats = new LinkedHashMap();

		//curr_items - Current number of items stored by the server
		stats.put("curr_items", "" + cache.size());

		//total_items - Total number of items stored by this server ever since it started
		stats.put("total_items", "" + totalItems);

		//cmd_get           Cumulative number of retrieval reqs
		stats.put("cmd_get", "" + cmdGets);

		//cmd_set           Cumulative number of storage reqs
		stats.put("cmd_set", "" + cmdSets);

		//cmd_flush
		stats.put("cmd_flush", "" + cmdFlushs);

		//get_hits          Number of keys that have been requested and found present
		stats.put("get_hits", "" + getHits);
						  
		//get_misses        Number of items that have been requested and not found
		stats.put("get_misses", "" + getMisses);
		
		//delete_misses     Number of deletions reqs for missing keys
		stats.put("delete_misses", "" + deleteMisses);
		
		//delete_hits       Number of deletion reqs resulting in
		stats.put("delete_hits", "" + deleteHits);

		return stats;
	}

	public void set(Object key, Object value, long expInSec) {
		cache.store(key, value, expInSec*1000);
		totalItems++;
		cmdSets++;
	}

	public Object get(Object key) {
		cmdGets++;
		Object obj = cache.retrieve(key);
		if(obj!=null) {
			getHits++;
		} else {
			getMisses++;
		}
		return obj;
	}

	public Object delete(Object key) {
		Object obj = cache.remove(key);
		if(obj!=null) {
			deleteHits++;
		} else {
			deleteMisses++;
		}
		return obj;

	}

	public void flush() {
		cache.clear();
		cmdFlushs++;
	}
}
