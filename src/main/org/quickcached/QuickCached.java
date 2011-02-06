package org.quickcached;

import org.quickserver.net.*;
import org.quickserver.net.server.*;

import java.io.*;
import org.apache.log4j.xml.DOMConfigurator;

public class QuickCached {
	public static String version = "1.0.0";
	public static boolean DEBUG = false;
	
	private static final int KEY_MAX_LENGTH = 250;

	public static void main(String args[]) throws Exception {
		int i = 0;
        String arg;
		String value;
		while(i < args.length && args[i].startsWith("-")) {
            arg = args[i++];

			if(arg.equals("-v") || arg.equals("-vv")) {
				SetupLoggingHook.setMakeLogFile(true);
			}

			if(arg.equals("-vv")) {
				DEBUG = true;
			} else {
				DOMConfigurator.configure("conf/log4j.xml");
			}
		}

		String confFile = "conf" + File.separator + "QuickCached.xml";
		Object config[] = new Object[] {confFile};
		
		QuickServer quickcached = new QuickServer();
		quickcached.initService(config);

		//CLI
		//-l <ip_addr>
			//Listen on <ip_addr>; default to INDRR_ANY.
			//This is an important option to consider as there is no other way to secure the installation.
			//Binding to an internal or firewalled network interface is suggested
		//-c <num>
			//Use <num> max simultaneous connections; the default is 1024.
		//-p <num>
			//Listen on TCP port <num>, the default is port 11211.
		//-v
			//Be verbose during the event loop; print out errors and warnings.
		//-vv
			//Be even more verbose; same as -v but also print client commands and responses.

		i = 0;
		while(i < args.length && args[i].startsWith("-")) {
            arg = args[i++];

			if(arg.equals("-l")) {
				value = args[i++];
				quickcached.setBindAddr(value);
			} else if(arg.equals("-p")) {
				value = args[i++];
				quickcached.setPort(Integer.parseInt(value));
			} else if(arg.equals("-c")) {
				value = args[i++];
				quickcached.setMaxConnection(Integer.parseInt(value));
			} else if(arg.equals("-h")) {
				System.out.println("QuickCached CLI");
				return;
			} else {
				//print help - TODO
				System.out.println("Error: Bad argument passed - "+arg);
				return;
			}
		}

		try {
			if(quickcached!=null) quickcached.startServer();
		} catch (AppException e) {
			System.out.println("Error starting server : " + e);
			e.printStackTrace();
		}
	}
}
