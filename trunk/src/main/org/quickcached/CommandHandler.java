package org.quickcached;

import java.net.*;
import java.io.*;
import org.quickserver.net.server.ClientHandler;
import org.quickserver.net.server.ClientEventHandler;
import java.util.logging.*;



import org.quickcached.binary.BinaryPacket;
import org.quickcached.cache.CacheInterface;
import org.quickcached.cache.WhirlycottCacheImpl;
import org.quickserver.net.server.ClientBinaryHandler;

public class CommandHandler implements ClientBinaryHandler, ClientEventHandler {
	private static final Logger logger = Logger.getLogger(CommandHandler.class.getName());
	

	private CacheInterface cache = null;
	private TextCommandProcessor textCommandProcessor = null;
	private BinaryCommandProcessor binaryCommandProcessor = null;

	public CommandHandler() {
		cache = new WhirlycottCacheImpl();

		textCommandProcessor = new TextCommandProcessor();
		textCommandProcessor.setCache(cache);

		binaryCommandProcessor = new BinaryCommandProcessor();
		binaryCommandProcessor.setCache(cache);
	}


	//--ClientEventHandler
	public void gotConnected(ClientHandler handler)
		throws SocketTimeoutException, IOException {
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

		if(data.getDataRequiredLength()!=0) {
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
}
