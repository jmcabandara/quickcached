package org.quickcached.cache;

import com.whirlycott.cache.*;
import java.util.logging.Logger;

/**
 *
 * @author akshath
 */
public class WhirlycottCacheImpl implements CacheInterface {
	private static final Logger logger = Logger.getLogger(WhirlycottCacheImpl.class.getName());

	private Cache cache = null;

	public WhirlycottCacheImpl() {
		try {
			cache = CacheManager.getInstance().getCache();
		} catch(Exception e) {
			logger.severe("Error: "+e);
		}
	}

	public void set(Object key, Object value, long expInSec) {
		cache.store(key, value, expInSec*1000);
	}

	public Object get(Object key) {
		return cache.retrieve(key);
	}

	public Object delete(Object key) {
		return cache.remove(key);
	}

	public void flush() {
		cache.clear();
	}
}
