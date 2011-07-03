package org.quickcached;

import java.net.*;
import java.io.*;
import org.quickserver.net.server.ClientHandler;
import org.quickserver.net.server.ClientEventHandler;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.*;


import org.quickcached.binary.BinaryPacket;
import org.quickcached.cache.CacheInterface;
import org.quickcached.mem.MemoryWarningSystem;
import org.quickserver.net.server.ClientBinaryHandler;
import org.quickserver.net.server.QuickServer;

public class CommandHandler implements ClientBinaryHandler, ClientEventHandler {
	private static final Logger logger = Logger.getLogger(CommandHandler.class.getName());

	private static CacheInterface cache = null;
	private static TextCommandProcessor textCommandProcessor = null;
	private static BinaryCommandProcessor binaryCommandProcessor = null;

	private static long totalConnections;
	private static long bytesRead;
	//private static long bytesWritten;

	public static Map getStats(QuickServer server) {
		Map stats = new LinkedHashMap();

		//pid
		String pid = QuickCached.getPID();
		stats.put("pid", pid);

		//uptime
		long uptimeSec = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
		stats.put("uptime", "" + uptimeSec);

		//time - TODO

		//version
		stats.put("version", QuickCached.version);

		//curr_connections
		stats.put("curr_connections", "" + server.getClientCount());

		//total_connections
		stats.put("total_connections", "" + totalConnections);

		//bytes_read    Total number of bytes read by this server from network
		stats.put("bytes_read", "" + bytesRead);

		//bytes_written     Total number of bytes sent by this server to network
		//todo

		//bytes - Current number of bytes used by this server to store items
		long usedMemory = Runtime.getRuntime().totalMemory() - 
				Runtime.getRuntime().freeMemory();			        
		stats.put("bytes", "" + usedMemory);

		//limit_maxbytes    Number of bytes this server is allowed to use for storage.
		long heapMaxSize = Runtime.getRuntime().maxMemory();
		stats.put("limit_maxbytes", "" + heapMaxSize);
		
		long mem_percent_used = (long) (100.0*usedMemory/heapMaxSize);
		stats.put("mem_percent_used", "" + mem_percent_used);
		
		//threads           Number of worker threads requested.
		//stats.put("threads", );

		Map implStats = cache.getStats();
		stats.putAll(implStats);

		return stats;
	}

	

	public CommandHandler() {
		
	}


	//--ClientEventHandler
	public void gotConnected(ClientHandler handler)
			throws SocketTimeoutException, IOException {
		totalConnections++;
		if(QuickCached.DEBUG) logger.fine("Connection opened: "+handler.getHostAddress());
	}

	public void lostConnection(ClientHandler handler) 
			throws IOException {
		if(QuickCached.DEBUG) logger.fine("Connection Lost: "+handler.getSocket().getInetAddress());
	}
	public void closingConnection(ClientHandler handler) 
			throws IOException {
		if(QuickCached.DEBUG) logger.fine("Connection closed: "+handler.getSocket().getInetAddress());
	}
	//--ClientEventHandler

	

	public void handleBinary(ClientHandler handler, byte command[])
			throws SocketTimeoutException, IOException {
		if(QuickCached.DEBUG) logger.fine("C: "+new String(command));
		if(QuickCached.DEBUG) logger.fine("H: "+HexUtil.encode(new String(command)));

		Data data = (Data) handler.getClientData();
		data.addBytes(command);

		bytesRead = bytesRead + command.length;

		if(data.getDataRequiredLength()!=0) {//only used by text mode
			try {
				if(data.isAllDataIn()) {
					textCommandProcessor.processStorageCommands(handler);
					return;
				} else {
					return;
				}
			} catch(IllegalArgumentException e) {
				logger.warning("Error: "+e);
				textCommandProcessor.sendResponse(handler, "ERROR\r\n");
			} catch(Exception e) {
				logger.warning("Error: "+e);
				textCommandProcessor.sendResponse(handler, "ERROR\r\n");
			}
		}

		while(data.isMoreCommandToProcess()) {
			if(data.isBinaryCommand()) {
				if(QuickCached.DEBUG) logger.fine("BinaryCommand");
				BinaryPacket bp = null;
				try {
					bp = data.getBinaryCommandHeader();
				} catch (Exception ex) {
					Logger.getLogger(CommandHandler.class.getName()).log(Level.SEVERE, null, ex);
					throw new IOException(""+ex);
				}

				if(bp!=null) {
					if(QuickCached.DEBUG) logger.fine("BinaryCommand Start");
					binaryCommandProcessor.handleBinaryCommand(handler, bp);
					if(QuickCached.DEBUG) logger.fine("BinaryCommand End");
				} else {
					break;
				}
			} else {
				String cmd = data.getCommand();
				if(cmd!=null) {
					textCommandProcessor.handleTextCommand(handler, cmd);
				} else {
					break;
				}
			}
		}
	}

	private static boolean lowMemoryActionInit;
	private static MemoryWarningSystem mws = new MemoryWarningSystem();

	public static void init(Map config) {
		logger.fine("in init");
		String implClass = (String) config.get("CACHE_IMPL_CLASS");
		if(implClass==null) throw new NullPointerException("Cache impl class not specified!");
		try {
			cache = (CacheInterface) Class.forName(implClass).newInstance();
		} catch (Exception ex) {
			Logger.getLogger(CommandHandler.class.getName()).log(Level.SEVERE, null, ex);
		}

		textCommandProcessor = new TextCommandProcessor();
		textCommandProcessor.setCache(cache);

		binaryCommandProcessor = new BinaryCommandProcessor();
		binaryCommandProcessor.setCache(cache);

		String flushPercent = (String) config.get("FLUSH_ON_LOW_MEMORY_PERCENT");
		if(flushPercent!=null && flushPercent.trim().equals("")==false) {
			double fpercent = Double.parseDouble(flushPercent);
			MemoryWarningSystem.setPercentageUsageThreshold(fpercent);//9.5=95%
			logger.log(Level.INFO, "MemoryWarningSystem set to {0}; will flush if reached!", fpercent);

			if(lowMemoryActionInit==false) {
				lowMemoryActionInit = true;
				mws.addListener(new MemoryWarningSystem.Listener() {
					public void memoryUsageHigh(long usedMemory, long maxMemory) {
						logger.log(Level.INFO,
								"Memory usage high!: UsedMemory: {0};maxMemory:{1}",
								new Object[]{usedMemory, maxMemory});
						double percentageUsed = ((double) usedMemory) / maxMemory;
						logger.log(Level.SEVERE,
								"Memory usage high! Percentage of memory used: {0}",
								percentageUsed);
						logger.warning("Flushing cache to save JVM.");
						cache.flush();
						System.gc();
						logger.fine("Done");
					}
				});
			}
		} else {
			mws.removeAllListener();
			lowMemoryActionInit = false;
		}
	}
}
