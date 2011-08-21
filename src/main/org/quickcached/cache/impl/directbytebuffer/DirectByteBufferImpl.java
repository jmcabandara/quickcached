package org.quickcached.cache.impl.directbytebuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;
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
public class DirectByteBufferImpl extends BaseCacheImpl {
	private static final Logger logger = Logger.getLogger(DirectByteBufferImpl.class.getName());
	
	private static Map map = new ConcurrentHashMap();
	private static Map mapTtl = new ConcurrentHashMap();
	
	private static int tunerSleeptime = 130;//in sec
	
	static {
		Thread t = new Thread("DirectByteBuffer-PurgThread") {
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
							Logger.getLogger(DirectByteBufferImpl.class.getName()).log(
									Level.SEVERE, null, ex);
						}
					}
					
					stime = System.currentTimeMillis();
					try {
						purgeOperation();
					} catch (Exception ex) {
						Logger.getLogger(DirectByteBufferImpl.class.getName()).log(
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
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error: " + e, e);
		}
	}
	
	public long getSize() {
		return map.size();
	}
	
	public void setToCache(String key, Object value, int objectSize, 
			long expInSec) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(baos);
		stream.writeObject(value);
		stream.close();
		byte b[] = baos.toByteArray();
		ByteBuffer buffer = ByteBuffer.allocateDirect(b.length);
		buffer.put(b);
		map.put(key, buffer);
		mapTtl.put(key, new Date(System.currentTimeMillis()+expInSec*1000));
	}
	
	public void updateToCache(String key, Object value, int objectSize) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(baos);
		stream.writeObject(value);
		stream.close();
		byte b[] = baos.toByteArray();
		ByteBuffer buffer = ByteBuffer.allocateDirect(b.length);
		buffer.put(b);
		map.put(key, buffer);
	}
	
	public Object getFromCache(String key) throws Exception {
		ByteBuffer buffer2 = (ByteBuffer) map.get(key);
		if (buffer2 != null) {
			byte buf[] = new byte[buffer2.capacity()];
			buffer2.rewind();
			buffer2.get(buf);
			ByteArrayInputStream bais = new ByteArrayInputStream(buf);
			ObjectInputStream oois = new ObjectInputStream(bais);
			Object object = oois.readObject();
			oois.close();
			return object;
		} else {
			if(QuickCached.DEBUG) logger.log(Level.FINE, "no value in db for key: {0}", key);
			return null;
		}
	}
	
	public boolean deleteFromCache(String key) throws Exception {
		Object obj = map.remove(key);
		return obj!=null;
	}
	
	public void flushCache() throws Exception {
		map.clear();
	}
}
