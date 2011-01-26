package org.quickcached.protocol;

import java.io.IOException;
import java.util.logging.*;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.BinaryConnectionFactory;
/**
 *
 * @author akshath
 */
public class BinaryProtocolTest extends ProtocolTest {
	public BinaryProtocolTest(String name) {
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

    public static void main(String args[]) {
        junit.textui.TestRunner.run(BinaryProtocolTest.class);
    }
}
