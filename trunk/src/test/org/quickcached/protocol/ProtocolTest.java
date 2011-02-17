package org.quickcached.protocol;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;

import net.spy.memcached.MemcachedClient;

/**
 *
 * @author akshath
 */
public class ProtocolTest extends TestCase  {
	protected MemcachedClient c = null;

	public ProtocolTest(String name) {
        super(name);
    }

	public void testAccess() {
		c.checkAccess();
	}

	public void testAsyncGet() {
		c.set("Hello", 3600, "World");


		String readObject = null;
		Future <Object> f = c.asyncGet("Hello");
		try {
			readObject = (String) f.get(15, TimeUnit.SECONDS);
		} catch(Exception e) {
			f.cancel(false);
		}
		assertNotNull(readObject);
		assertEquals("World",  readObject);

    }
	
	public void testGet() {
        Date value = new Date();

		c.set("someKey", 3600, value);
		Date readObject = (Date) c.get("someKey");

		assertNotNull(readObject);
		assertEquals(value.getTime(),  readObject.getTime());
	}

	public void testDelete() {
		c.set("Hello", 3600, "World");
		String readObject = (String) c.get("Hello");

		assertNotNull(readObject);
		assertEquals("World",  readObject);

		c.delete("Hello");
		readObject = (String) c.get("Hello");
		assertNull(readObject);
	}

	public void testVersion() {
		Map ver = c.getVersions();
		assertNotNull(ver);
		System.out.println("ver: "+ver);
		Iterator iterator = ver.keySet().iterator();
		InetSocketAddress key = null;
		while(iterator.hasNext()) {
			key = (InetSocketAddress) iterator.next();
			assertNotNull(key);
			assertEquals("1.0.0",  (String) ver.get(key));
		}
	}


	public void testStats() {
		Map stats = c.getStats();
		assertNotNull(stats);

		Iterator iterator = stats.keySet().iterator();
		InetSocketAddress key = null;
		while(iterator.hasNext()) {
			key = (InetSocketAddress) iterator.next();
			assertNotNull(key);
			System.out.println("Stat for "+key+" " +stats.get(key));
		}
	}
	
	public void testFlush() {
		c.set("Hello", 3600, "World");
		String readObject = (String) c.get("Hello");

		assertNotNull(readObject);
		assertEquals("World",  readObject);

		c.flush();
		
		readObject = (String) c.get("Hello");
		assertNull(readObject);
	}
}
