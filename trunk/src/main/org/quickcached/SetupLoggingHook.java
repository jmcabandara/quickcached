package org.quickcached;

import org.quickserver.net.server.*;
import org.quickserver.net.InitServerHook;

import java.io.*;

import org.quickserver.util.logging.*;
import java.util.logging.*;
import org.apache.log4j.xml.DOMConfigurator;

public class SetupLoggingHook implements InitServerHook {
	private QuickServer quickserver;

	public String info() {
		return "Init Server Hook to setup logging.";
	}

	public void handleInit(QuickServer quickserver) throws Exception {
		Logger logger = null;
		FileHandler txtLog = null;
		File log = new File("./log/");
		if(!log.canRead())
			log.mkdir();
		try	{
			logger = Logger.getLogger("");
			logger.setLevel(Level.FINEST);

			/*
			logger = Logger.getLogger("");
			txtLog = new FileHandler("log/QuickCached_Full%u%g.txt", 
				1024*1024, 5, true);
			txtLog.setFormatter(new SimpleTextFormatter());
			txtLog.setLevel(Level.FINEST);
			logger.addHandler(txtLog);
			

			logger = Logger.getLogger("org.quickserver");
			txtLog = new FileHandler("log/QuickCached_QuickServer%u%g.txt", 
				1024*1024, 20, true);
			txtLog.setFormatter(new SimpleTextFormatter());
			txtLog.setLevel(Level.INFO);
			logger.addHandler(txtLog);
			 */
			
			logger = Logger.getLogger("org.quickcached");
			txtLog = new FileHandler("log/QuickCached%u%g.txt", 
				1024*1024, 20, true);
			txtLog.setFormatter(new SimpleTextFormatter());
			txtLog.setLevel(Level.FINEST);
			logger.addHandler(txtLog);

			quickserver.setAppLogger(logger); //img

			//debug non-blocking mode 
			//quickserver.debugNonBlockingMode(false);

			DOMConfigurator.configure("conf/log4j.xml");
		} catch(IOException e){
			System.err.println("Could not create txtLog FileHandler : "+e);
			throw e;
		}
	}
}
