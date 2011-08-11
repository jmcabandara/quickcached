package org.quickcached.cache.impl.whirlycott;

import com.whirlycott.cache.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quickcached.CommandHandler;
import org.quickcached.QuickCached;
import org.quickcached.cache.CacheInterface;

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
	private long cmdDeletes;
	private long cmdFlushs;
	private long getHits;
	private long getMisses;
	private long deleteMisses;
	private long deleteHits;
	
	private double avgKeySize = -1;
	private double avgValueSize = -1;
	private double avgTtl = -1;

	
	public WhirlycottCacheImpl() {
		FileInputStream myInputStream = null;
		Properties config = null;
		String fileCfg = "./conf/whirlycache/default.ini";
		try {
			config = new Properties();
			myInputStream = new FileInputStream(fileCfg);
			config.load(myInputStream);
		} catch (Exception e) {
			logger.severe("Could not load["+fileCfg+"] "+e);
		} finally {
			if(myInputStream!=null) {
				try {
					myInputStream.close();
				} catch (IOException ex) {
					Logger.getLogger(WhirlycottCacheImpl.class.getName()).log(Level.SEVERE, "Error", ex);
				}
			}
		}
		
		try {
			if(config!=null) {
				Map map = CacheManager.getConfiguration();			
				CacheConfiguration cc = (CacheConfiguration) map.get("default");
			
				cc.setTunerSleepTime(Integer.parseInt(config.getProperty("tuner-sleeptime").trim()));
				cc.setPolicy(config.getProperty("policy").trim());
				cc.setMaxSize(Integer.parseInt(config.getProperty("maxsize").trim()));
				cc.setBackend(config.getProperty("backend").trim());
				
				CacheManager.getInstance().destroy("default");
				cache = CacheManager.getInstance().createCache(cc);
			}			
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

		return stats;
	}

	public void set(String key, Object value, int objectSize, long expInSec) {
		if(QuickCached.DEBUG) logger.log(Level.FINE, "set key: {0}; objectsize: {1};", new Object[]{key, objectSize});
		cache.store(key, value, expInSec*1000);
		totalItems++;
		cmdSets++;
		
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
	}
	
	public void update(String key, Object value, int objectSize) {
		if(QuickCached.DEBUG) logger.log(Level.FINE, "update key: {0}; objectsize: {1};", new Object[]{key, objectSize});
		cache.store(key, value);
	}

	public Object get(String key) {
		if(QuickCached.DEBUG) logger.log(Level.FINE, "get key: {0}", key);
		cmdGets++;
		Object obj = cache.retrieve(key);
		if(obj!=null) {
			getHits++;
		} else {
			if(QuickCached.DEBUG) logger.log(Level.FINE, "no value in db for key: {0}", key);
			getMisses++;
		}
		return obj;
	}

	public boolean delete(String key) {
		cmdDeletes++;
		if(QuickCached.DEBUG) logger.log(Level.FINE, "delete key: {0};", key);
		Object obj = cache.remove(key);
		if(obj!=null) {
			deleteHits++;
		} else {
			deleteMisses++;
		}
		return obj!=null;
	}

	public void flush() {
		if(QuickCached.DEBUG) logger.log(Level.FINE, "flush");
		cache.clear();
		cmdFlushs++;
	}
}
