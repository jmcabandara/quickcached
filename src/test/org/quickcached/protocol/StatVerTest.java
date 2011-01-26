package org.quickcached.protocol;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.*;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.BinaryConnectionFactory;
import junit.framework.TestCase;

/**
 *
 * @author akshath
 */
public class StatVerTest  extends TestCase  {
	protected MemcachedClient c = null;

	public StatVerTest(String name) {
        super(name);
    }

	public void setUp(){
		try {
			c = new MemcachedClient(new BinaryConnectionFactory(),
					AddrUtil.getAddresses("localhost:11211"));
		} catch (IOException ex) {
			Logger.getLogger(TextProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void tearDown(){
		if(c!=null) c.shutdown();
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
}
