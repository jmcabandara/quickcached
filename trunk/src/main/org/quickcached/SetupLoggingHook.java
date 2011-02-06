package org.quickcached;

import org.quickserver.net.server.*;
import org.quickserver.net.InitServerHook;

import java.io.*;

import org.quickserver.util.logging.*;
import java.util.logging.*;
import org.apache.log4j.xml.DOMConfigurator;

public class SetupLoggingHook implements InitServerHook {
	private QuickServer quickserver;

	private static boolean makeLogFile;

	public String info() {
		return "Init Server Hook to setup logging.";
	}

	public void handleInit(QuickServer quickserver) throws Exception {
		Logger logger = null;
		FileHandler txtLog = null;

		if(isMakeLogFile()) {
			File log = new File("./log/");
			if(!log.canRead())
				log.mkdir();
			DOMConfigurator.configure("conf/log4j_debug.xml");
		}
		
		try	{
			logger = Logger.getLogger("");
			logger.setLevel(Level.FINEST);

			logger = Logger.getLogger("org.quickcached");
			
			if(isMakeLogFile()) {
				txtLog = new FileHandler("log/QuickCached_"+quickserver.getPort()+"_%u%g.txt",
					1024*1024, 20, true);
				txtLog.setLevel(Level.FINEST);
				txtLog.setFormatter(new SimpleTextFormatter());				
				logger.addHandler(txtLog);
			} else {
				logger.setLevel(Level.WARNING);
			}

			quickserver.setAppLogger(logger); //img			
		} catch(IOException e){
			System.err.println("Could not create txtLog FileHandler : "+e);
			throw e;
		}
	}

	public static boolean isMakeLogFile() {
		return makeLogFile;
	}

	public static void setMakeLogFile(boolean flag) {
		makeLogFile = flag;
	}
}
