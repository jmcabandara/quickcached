package org.quickcached.cache.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quickcached.CommandHandler;
import org.quickcached.QuickCached;
import org.quickcached.cache.CacheInterface;

/**
 * BaseCacheImpl implementation
 * @author akshath
 */
public abstract class BaseCacheImpl implements CacheInterface {
	private static final Logger logger = Logger.getLogger(BaseCacheImpl.class.getName());	
	
	public abstract String getName();
	public abstract long getSize();
	public abstract void setToCache(String key, Object value, int objectSize, 
			long expInSec) throws Exception;
	public abstract void updateToCache(String key, Object value, int objectSize) throws Exception;
	public abstract Object getFromCache(String key) throws Exception;
	public abstract boolean deleteFromCache(String key) throws Exception;
	public abstract void flushCache() throws Exception;

	private long totalItems;
	private long cmdGets;
	private long cmdSets;
	private long cmdDeletes;
	private long cmdFlushs;
	private long getHits;
	private long getMisses;
	private long deleteMisses;
	private long deleteHits;
	
	private double avgKeySize = -1;
	private double avgValueSize = -1;
	private double avgTtl = -1;	

	
	public void saveStats(Map stats) {
		if(stats==null) stats = new LinkedHashMap();

		//curr_items - Current number of items stored by the server
		stats.put("curr_items", "" + getSize());

		//total_items - Total number of items stored by this server ever since it started
		stats.put("total_items", "" + totalItems);

		//cmd_get           Cumulative number of retrieval reqs
		stats.put("cmd_get", "" + cmdGets);

		//cmd_set           Cumulative number of storage reqs
		stats.put("cmd_set", "" + cmdSets);

		//cmd_delete
		stats.put("cmd_delete", "" + cmdDeletes);

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
		
		if(CommandHandler.isComputeAvgForSetCmd()) {
			stats.put("avg_key_size", "" + (long)(0.5+avgKeySize));
			stats.put("avg_value_size", "" + (long)(0.5+avgValueSize));
			stats.put("avg_ttl", "" + (long)(0.5+avgTtl));
		}
	}

	public void set(String key, Object value, int objectSize, long expInSec) {
		if(QuickCached.DEBUG) logger.log(Level.FINE, "set key: {0}; objectsize: {1};", 
				new Object[]{key, objectSize});
		
		cmdSets++;
		try {
			setToCache(key, value, objectSize, expInSec);
			
			totalItems++;
		
			if(CommandHandler.isComputeAvgForSetCmd()) {
				long cmdSetsCurrent = cmdSets;

				if(avgKeySize==-1) {
					avgKeySize = (avgKeySize*(cmdSetsCurrent-1) + key.length())/cmdSetsCurrent;
				} else {
					avgKeySize = key.length();
				}

				if(avgValueSize==-1) {
					avgValueSize = (avgValueSize*(cmdSetsCurrent-1) + objectSize)/cmdSetsCurrent;
				} else {
					avgValueSize = objectSize;
				}

				if(avgTtl==-1) {
					avgTtl = (avgTtl*(cmdSetsCurrent-1) + expInSec)/cmdSetsCurrent;
				} else {
					avgTtl = expInSec;
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
		}		
	}	
	
	public void update(String key, Object value, int objectSize) {
		if(QuickCached.DEBUG) logger.log(Level.FINE, "update key: {0}; objectsize: {1};", 
				new Object[]{key, objectSize});
		try {
			updateToCache(key, value, objectSize);
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
		}
	}

	public Object get(String key) {
		if(QuickCached.DEBUG) logger.log(Level.FINE, "get key: {0}", key);
		
		cmdGets++;
		Object obj = null;
		try {
			obj = getFromCache(key);			
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
		if(obj!=null) {
			getHits++;
		} else {
			if(QuickCached.DEBUG) logger.log(Level.FINE, "no value in db for key: {0}", 
					key);
			getMisses++;
		}
		return obj;
	}

	public boolean delete(String key) {		
		if(QuickCached.DEBUG) logger.log(Level.FINE, "delete key: {0};", key);
		cmdDeletes++;
		
		boolean flag = false;
		try {
			flag = deleteFromCache(key);
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
		}
		if(flag) {
			deleteHits++;
		} else {
			deleteMisses++;
		}
		return flag;
	}	

	public void flush() {
		if(QuickCached.DEBUG) logger.log(Level.FINE, "flush");
		try {
			flushCache();
			cmdFlushs++;
		} catch (Exception ex) {
			Logger.getLogger(BaseCacheImpl.class.getName()).log(Level.SEVERE, "Error: "+ex, ex);
		}		
	}
}
