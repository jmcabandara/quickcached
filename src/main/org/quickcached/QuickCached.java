package org.quickcached;

import org.quickserver.net.*;
import org.quickserver.net.server.*;

import java.io.*;
import java.util.logging.*;
import org.quickserver.util.logging.*;

public class QuickCached {
	public static String version = "1.0";

	private static final int KEY_MAX_LENGTH = 250;

	public static void main(String s[])	{
		QuickServer quickcached;		
		String confFile = "conf"+File.separator+"QuickCached.xml";
		try	{
			quickcached = QuickServer.load(confFile);
		} catch(AppException e) {
			System.out.println("Error in server : "+e);
			e.printStackTrace();
		}
	}
}


