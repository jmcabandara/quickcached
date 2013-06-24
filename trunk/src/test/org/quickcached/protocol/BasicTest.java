package org.quickcached.protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.quickcached.client.CASResponse;
import org.quickcached.client.CASValue;
import org.quickcached.client.MemcachedClient;
import org.quickcached.client.TimeoutException;

/**
 *
 * @author akshath
 */
public class BasicTest  extends TestCase  {
	protected MemcachedClient c = null;

	public BasicTest(String name) {
        super(name);
    }
	
	public void setUp(){
		try {
			c = MemcachedClient.getInstance();
			c.setUseBinaryConnection(false);
			c.setAddresses("localhost:11211");
			c.setDefaultTimeoutMiliSec(3000);//3 sec
			c.setConnectionPoolSize(1);
			c.init();
		} catch (Exception ex) {
			Logger.getLogger(TextProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void tearDown(){
		if(c!=null) {
			try {
				c.stop();
			} catch (IOException ex) {
				Logger.getLogger(TextProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

    public static void main(String args[]) {
        junit.textui.TestRunner.run(BasicTest.class);
    }

	public void testReplace() throws TimeoutException, org.quickcached.client.MemcachedException {
		String key = "keygetstest";
		String value = "World";
		c.set(key, 3600, value);
		
		CASValue casResult = c.gets(key, 1000);
		System.out.println("val " + casResult.getValue());
		System.out.println("cas " + casResult.getCas());
		
		
	}
}
