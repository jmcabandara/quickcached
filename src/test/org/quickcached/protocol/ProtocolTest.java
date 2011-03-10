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

	public void testAppend() {
        String value = "ABCD";

		c.set("someKeyA", 3600, value);


		c.append(1, "someKeyA", "EFGH");
		String readObject = (String) c.get("someKeyA");

		assertNotNull(readObject);
		assertEquals(readObject,  "ABCDEFGH");
	}

	public void testPrepend() {
        String value = "ABCD";

		c.set("someKeyP", 3600, value);


		c.prepend(1, "someKeyP", "EFGH");
		String readObject = (String) c.get("someKeyP");

		assertNotNull(readObject);
		assertEquals(readObject,  "EFGHABCD");
	}

	public void testAdd() {
        String value = "ABCD";

		c.delete("someKeyAd");
		boolean flag = false;

		Future <Boolean> f = c.add("someKeyAd", 3600, value);
		try {
			flag = ((Boolean) f.get(15, TimeUnit.SECONDS)).booleanValue();
		} catch(Exception e) {
			f.cancel(false);
		}

		assertTrue(flag);

		f = c.add("someKeyAd", 3600, value);
		try {
			flag = ((Boolean) f.get(15, TimeUnit.SECONDS)).booleanValue();
		} catch(Exception e) {
			f.cancel(false);
		}
		assertFalse(flag);
	}

	public void testReplace() {
        String value = "ABCD";

		c.set("someKey", 3600, "World");

		boolean flag = false;

		Future <Boolean> f = c.replace("someKey", 3600, value);
		try {
			flag = ((Boolean) f.get(15, TimeUnit.SECONDS)).booleanValue();
		} catch(Exception e) {
			f.cancel(false);
		}

		assertTrue(flag);

		c.delete("someKey");
		f = c.replace("someKey", 3600, value);
		try {
			flag = ((Boolean) f.get(15, TimeUnit.SECONDS)).booleanValue();
		} catch(Exception e) {
			f.cancel(false);
		}
		assertFalse(flag);
	}

	public void testIncrement() {
        String value = "10";

		c.set("someKeyI", 3600, value);
		c.incr("someKeyI", 10);

		String readObject = (String) c.get("someKeyI");
		assertNotNull(readObject);
		assertEquals(readObject, "20");

		c.incr("someKeyI", 1);
		readObject = (String) c.get("someKeyI");
		assertNotNull(readObject);
		assertEquals(readObject, "21");
	}

	public void testDecrement() {
        String value = "10";

		c.set("someKeyD", 3600, value);
		c.decr("someKeyD", 7);

		String readObject = (String) c.get("someKeyD");
		readObject = readObject.trim();

		assertNotNull(readObject);
		assertEquals(readObject, "3");

		c.decr("someKeyD", 4);
		readObject = (String) c.get("someKeyD");
		readObject = readObject.trim();

		assertNotNull(readObject);
		assertEquals(readObject, "0");
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
			//assertEquals("1.4.5",  (String) ver.get(key));
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
