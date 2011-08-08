package org.quickcached.client.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.quickcached.client.MemcachedClient;
import org.quickcached.client.TimeoutException;
import java.util.Map;

/**
 *
 * @author Akshathkumar Shetty
 */
public class XMemcachedImpl extends MemcachedClient {
	private net.rubyeye.xmemcached.MemcachedClient c = null;
	private String hostList;
	private boolean binaryConnection = true;
	private int poolSize = 10;

	public void setUseBinaryConnection(boolean flag) {
		binaryConnection = flag;
	}
	
	public void setConnectionPoolSize(int size) {
		poolSize = size;
	}

	public void setAddresses(String list) {
		hostList = list;
	}

	public void addServer(String list) throws IOException {
		c.addServer(list);
	}

	public void removeServer(String list) {
		c.removeServer(list);
	}

	public void init() throws IOException {
		if(c!=null) stop();
		
		MemcachedClientBuilder builder = new XMemcachedClientBuilder(
				AddrUtil.getAddresses(hostList));
		if(binaryConnection) {
			builder.setCommandFactory(new BinaryCommandFactory());
		}
		builder.setConnectionPoolSize(poolSize);
		c = builder.build();
	}

	public void stop() throws IOException {
		c.shutdown();
		c = null;
	}

	public void set(String key, int ttlSec, Object value, long timeoutMiliSec) throws TimeoutException {
		try {
			c.set(key, ttlSec, value, timeoutMiliSec);
		} catch (java.util.concurrent.TimeoutException ex) {
			throw new TimeoutException("Timeout "+ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(XMemcachedImpl.class.getName()).log(Level.SEVERE, 
					"InterruptedException:", ex);
		} catch (MemcachedException ex) {
			Logger.getLogger(XMemcachedImpl.class.getName()).log(Level.SEVERE, 
					"MemcachedException", ex);
		}
		
	}

	public Object get(String key, long timeoutMiliSec) throws TimeoutException {
		Object readObject = null;
		try {
			readObject = c.get(key, timeoutMiliSec);			
		} catch(java.util.concurrent.TimeoutException ex) {
			throw new TimeoutException("Timeout "+ex);
		} catch(InterruptedException ex) {
			Logger.getLogger(XMemcachedImpl.class.getName()).log(Level.SEVERE, 
					"InterruptedException:", ex);
		} catch(MemcachedException ex) {
			Logger.getLogger(XMemcachedImpl.class.getName()).log(Level.SEVERE, 
					"MemcachedException", ex);
		}
		return readObject;
	}

	public boolean delete(String key, long timeoutMiliSec) throws TimeoutException {
		try {
			return c.delete(key, timeoutMiliSec);
		} catch(java.util.concurrent.TimeoutException ex) {
			throw new TimeoutException("Timeout "+ex);
		} catch(InterruptedException ex) {
			Logger.getLogger(XMemcachedImpl.class.getName()).log(Level.SEVERE, 
					"InterruptedException:", ex);
		} catch(MemcachedException ex) {
			Logger.getLogger(XMemcachedImpl.class.getName()).log(Level.SEVERE, 
					"MemcachedException", ex);
		}
		return false;
	}

	public void flushAll() throws TimeoutException {
		try {
			c.flushAll();
		} catch (java.util.concurrent.TimeoutException ex) {
			throw new TimeoutException("Timeout "+ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(XMemcachedImpl.class.getName()).log(Level.SEVERE, null, ex);
		} catch (MemcachedException ex) {
			Logger.getLogger(XMemcachedImpl.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public Object getBaseClient() {
		return c;
	}
        
	public Map getStats() throws Exception {
		return c.getStats();
	}
	
}
