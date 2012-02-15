package org.quickcached.protocol;

import java.io.IOException;
import java.util.logging.*;
import org.quickcached.client.MemcachedClient;
import java.util.Date;
import org.quickcached.client.TimeoutException;

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
	
	public void testTouch() throws TimeoutException {		
        Date value = new Date();

		c.set("someKeyT1", 50, value);
		Date readObject = (Date) c.get("someKeyT1");

		assertNotNull(readObject);
		assertEquals(value.getTime(),  readObject.getTime());
		
		c.touch("someKeyT1", 3600);
		
		readObject = (Date) c.get("someKeyT1");

		assertNotNull(readObject);
		assertEquals(value.getTime(),  readObject.getTime());
	}
	
	public void testTouch2() throws TimeoutException {		
		c.set("someKeyT2", 50, "World");
		String readObject = (String) c.get("someKeyT2");

		assertNotNull(readObject);
		assertEquals("World",  readObject);
		
		c.touch("someKeyT2", 3600);
		
		readObject = (String) c.get("someKeyT2");

		assertNotNull(readObject);
		assertEquals("World",  readObject);
	}
	
	public void testGat() throws TimeoutException {		
		c.set("someKeyGAT1", 50, "World");
		String readObject = (String) c.gat("someKeyGAT1", 3600);

		assertNotNull(readObject);
		assertEquals("World",  readObject);	
	}
	
	public void testGat2() throws TimeoutException {	
		Date value = new Date();
		c.set("someKeyGAT2", 50, value);
		Date readObject = (Date) c.gat("someKeyGAT2", 3600);

		assertNotNull(readObject);
		assertEquals(value.getTime(),  readObject.getTime());
	}
}
