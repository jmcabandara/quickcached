package org.quickcached.protocol;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.HashMap;
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
		c.set("HelloAG", 3600, "World");


		String readObject = null;
		Future <Object> f = c.asyncGet("HelloAG");
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

		c.set("someKeyG", 3600, value);
		Date readObject = (Date) c.get("someKeyG");

		assertNotNull(readObject);
		assertEquals(value.getTime(),  readObject.getTime());
	}

	public void testAppend() {
        String value = "ABCD";

		c.set("someKeyA", 3600, value);


		c.append(1, "someKeyA", "EFGH");
		String readObject = (String) c.get("someKeyA");

		assertNotNull(readObject);
		assertEquals("ABCDEFGH", readObject);
	}

	public void testPrepend() {
        String value = "ABCD";

		c.set("someKeyP", 3600, value);


		c.prepend(1, "someKeyP", "EFGH");
		String readObject = (String) c.get("someKeyP");

		assertNotNull(readObject);
		assertEquals("EFGHABCD", readObject);
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

		c.set("someKeyR", 3600, "World");

		boolean flag = false;

		Future <Boolean> f = c.replace("someKeyR", 3600, value);
		try {
			flag = ((Boolean) f.get(15, TimeUnit.SECONDS)).booleanValue();
		} catch(Exception e) {
			f.cancel(false);
		}

		assertTrue(flag);

		c.delete("someKeyR");
		f = c.replace("someKeyR", 3600, value);
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
		assertEquals("20", readObject);

		c.incr("someKeyI", 1);
		readObject = (String) c.get("someKeyI");
		assertNotNull(readObject);
		assertEquals("21", readObject);
	}

	public void testDecrement() {
        String value = "10";

		c.set("someKeyD", 3600, value);
		c.decr("someKeyD", 7);

		String readObject = (String) c.get("someKeyD");
		readObject = readObject.trim();

		assertNotNull(readObject);
		assertEquals("3", readObject);

		c.decr("someKeyD", 4);
		readObject = (String) c.get("someKeyD");
		readObject = readObject.trim();

		assertNotNull(readObject);
		assertEquals("0", readObject);
	}

	public void testDelete() {
		c.set("HelloD", 3600, "World");
		String readObject = (String) c.get("HelloD");

		assertNotNull(readObject);
		assertEquals("World",  readObject);

		c.delete("HelloD");
		readObject = (String) c.get("HelloD");
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
		c.set("HelloF", 3600, "World");
		String readObject = (String) c.get("HelloF");

		assertNotNull(readObject);
		assertEquals("World",  readObject);

		c.flush();

		readObject = (String) c.get("Hello");
		assertNull(readObject);
	}
	
	public void testDoubleSet1() {
        String value = "v1";
		c.set("someKeyDS1", 3600, value);
		String readObject = (String) c.get("someKeyDS1");

		assertNotNull(readObject);
		assertEquals("v1",  readObject);
		
		value = "v2";
		c.set("someKeyDS1", 3600, value);
		readObject = (String) c.get("someKeyDS1");

		assertNotNull(readObject);
		assertEquals("v2",  readObject);
	}
	
	public void testDoubleSet2() {
        Map value = new HashMap();
		value.put("key1", "v1");
		
		c.set("someKey", 3600, value);
		Map readObject = (Map) c.get("someKey");

		assertNotNull(readObject);
		assertEquals(value,  readObject);
		
		value.put("key2", "v2");
		c.set("someKey", 3600, value);
		readObject = (Map) c.get("someKey");

		assertNotNull(readObject);
		assertEquals(value,  readObject);
	}

}
