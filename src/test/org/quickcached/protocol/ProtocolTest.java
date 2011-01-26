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
