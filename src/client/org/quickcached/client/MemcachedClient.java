package org.quickcached.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Akshathkumar Shetty
 */
public abstract class MemcachedClient {
	public static final String SpyMemcachedImpl = "SpyMemcached";
	public static final String XMemcachedImpl = "XMemcached";
	
	private static String defaultImpl = XMemcachedImpl;	
	
	private static Map implMap = new HashMap();
	public static void registerImpl(String implName, String fullClassName) {
		implMap.put(implName, fullClassName);
	}
	
	static {
		registerImpl(SpyMemcachedImpl, "org.quickcached.client.impl.SpyMemcachedImpl");
		registerImpl(XMemcachedImpl, "org.quickcached.client.impl.XMemcachedImpl");
		
		String impl = System.getProperty("org.quickcached.client.defaultImpl");
		if(impl!=null) {
			defaultImpl = impl;
		}
	}
	
	public static MemcachedClient getInstance() 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return getInstance(null);
	}
	
	public static MemcachedClient getInstance(String implName) 
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String fullClassName = (String) implMap.get(implName);
		if(fullClassName==null) fullClassName = (String) implMap.get(defaultImpl);
		
		return (MemcachedClient) Class.forName(fullClassName).newInstance();
	}

	private long defaultTimeoutMiliSec = 1000;//1sec
	public long getDefaultTimeoutMiliSec() {
		return defaultTimeoutMiliSec;
	}

	public void setDefaultTimeoutMiliSec(int aDefaultTimeoutMiliSec) {
		defaultTimeoutMiliSec = aDefaultTimeoutMiliSec;
	}
	
	public abstract void setUseBinaryConnection(boolean flag);
	public abstract void setConnectionPoolSize(int size);

	public abstract void setAddresses(String list);
	public abstract void init() throws IOException;
	public abstract void stop() throws IOException ;
	
	public abstract void addServer(String list) throws IOException;
	public abstract void removeServer(String list);
	
	public abstract void set(String key, int ttlSec, Object value, long timeoutMiliSec) 
			throws TimeoutException;
	public abstract Object get(String key, long timeoutMiliSec) throws TimeoutException;
	public abstract boolean delete(String key, long timeoutMiliSec) throws TimeoutException;
	public abstract void flushAll() throws TimeoutException;
        
	public abstract Map getStats() throws Exception;
	public abstract Object getBaseClient();
	
	public void set(String key, int ttlSec, Object value) 
			throws TimeoutException {
		set(key, ttlSec, value, defaultTimeoutMiliSec);
	}
	public Object get(String key) throws TimeoutException {
		return get(key, defaultTimeoutMiliSec);
	}
	public boolean delete(String key) throws TimeoutException {
		return delete(key, defaultTimeoutMiliSec);
	}	
}
