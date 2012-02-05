package org.quickcached.cache.impl.concurrenthashmap;

import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quickcached.QuickCached;
import org.quickcached.cache.impl.BaseCacheImpl;

/**
 *
 * @author Akshathkumar Shetty
 */
public class ConcurrentHashMapImpl extends BaseCacheImpl {
	private static final Logger logger = Logger.getLogger(ConcurrentHashMapImpl.class.getName());
	
	private static Map map = new ConcurrentHashMap();
	private static Map mapTtl = new ConcurrentHashMap();
	
	private static int tunerSleeptime = 130;//in sec
	
	protected volatile static long expired;
		
	static {
		Thread t = new Thread("ConcurrentHashMap-PurgThread") {
			public void run() {
				long timespent = 0;
				long timeToSleep = 0;
				long stime = 0;
				long etime = 0;
				while(true) {
					timeToSleep = tunerSleeptime*1000 - timespent;
					if(timeToSleep>0) {
						try {
							Thread.sleep(timeToSleep);
						} catch (InterruptedException ex) {
							Logger.getLogger(ConcurrentHashMapImpl.class.getName()).log(
									Level.SEVERE, null, ex);
						}
					}
					
					stime = System.currentTimeMillis();
					try {
						purgeOperation();
					} catch (Exception ex) {
						Logger.getLogger(ConcurrentHashMapImpl.class.getName()).log(
								Level.SEVERE, null, ex);
					}
					etime = System.currentTimeMillis();
					timespent = etime - stime;
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}
	
	public static void purgeOperation() {
		try {
			Iterator iterator = mapTtl.keySet().iterator();
			String key = null;
			Date expTime;
			Date currentTime = new Date();
			while(iterator.hasNext()) {
				key = (String) iterator.next();
				expTime = (Date) mapTtl.get(key);
				
				if(expTime.before(currentTime)) {
					mapTtl.remove(key);
					map.remove(key);
					expired++;
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error: " + e, e);
		}
	}
	
	public void saveStats(Map stats) {
		if(stats==null) stats = new LinkedHashMap();
		super.saveStats(stats);
		
		//expired - Number of items that expired
		stats.put("expired", ""+expired);
	}
	
	public String getName() {
		return "ConcurrentHashMapImpl";
	}
	
	public long getSize() {
		return map.size();
	}
	
	public void setToCache(String key, Object value, int objectSize, 
			long expInSec) throws Exception {
		map.put(key, value);
		if (expInSec != 0) {
			mapTtl.put(key, new Date(System.currentTimeMillis()+expInSec*1000));
		}
	}
	
	public void updateToCache(String key, Object value, int objectSize) throws Exception {
		map.put(key, value);
	}
	
	public Object getFromCache(String key) throws Exception {
		Object object = map.get(key);
		if (object != null) {
			return object;
		} else {
			if(QuickCached.DEBUG) logger.log(Level.FINE, "no value in db for key: {0}", key);
			return null;
		}
	}
	
	public boolean deleteFromCache(String key) throws Exception {
		mapTtl.remove(key);
		Object obj = map.remove(key);
		return obj!=null;
	}
	
	public void flushCache() throws Exception {
		mapTtl.clear();
		map.clear();
	}

	private static String fileName = "./ConcurrentHashMapImpl.dat";
	public boolean saveToDisk() {
		System.out.println("Saving state to disk..");
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(fileName));
			oos.writeObject(map);
			oos.flush();
			return true;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error: {0}", e);
		} finally {
			if(oos!=null) {
				try {
					oos.close();
				} catch (IOException ex) {
					Logger.getLogger(ConcurrentHashMapImpl.class.getName()).log(
						Level.WARNING, "Error: "+ex, ex);
				}
			}
			System.out.println("Done");
		}		
		return false;
	}

	public boolean readFromDisk() {
		File file = new File(fileName);
		if(file.canRead()==false) return false;
		
		ObjectInputStream ois = null;
		try {
			ois = new ObjectInputStream(new FileInputStream(fileName));
			map = (Map) ois.readObject();
			return true;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error: {0}", e);
		} finally {
			if(ois!=null) {
				try {
					ois.close();
				} catch (IOException ex) {
					Logger.getLogger(ConcurrentHashMapImpl.class.getName()).log(
						Level.WARNING, "Error: "+ex, ex);
				}
			}
			file.delete();
		}
		return false;
	}
}
