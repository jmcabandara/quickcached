package org.quickcached.protocol;

import java.io.IOException;
import java.util.logging.*;
import org.quickcached.client.MemcachedClient;

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
			c = MemcachedClient.getInstance();
			c.setUseBinaryConnection(true);
			c.setAddresses("localhost:11211");
			c.setDefaultTimeoutMiliSec(3000);//3 sec
			c.setConnectionPoolSize(1);
			c.init();
		} catch (Exception ex) {
			Logger.getLogger(BinaryProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void tearDown(){
		if(c!=null) {
			try {
				c.stop();
			} catch (IOException ex) {
				Logger.getLogger(BinaryProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

    public static void main(String args[]) {
        junit.textui.TestRunner.run(BinaryProtocolTest.class);
    }
}
