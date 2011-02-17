/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.quickcached;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;
import org.quickcached.cache.CacheInterface;
import org.quickserver.net.server.ClientHandler;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author akshath
 */
public class TextCommandProcessor {
	private static final Logger logger = Logger.getLogger(TextCommandProcessor.class.getName());

	private CacheInterface cache;
	public void setCache(CacheInterface cache) {
		this.cache = cache;
	}
	
	public void handleTextCommand(ClientHandler handler, String command)
			throws SocketTimeoutException, IOException {

		if(QuickCached.DEBUG) logger.fine("command: "+command);

		if(command.startsWith("set ") || command.startsWith("add ") ||
				command.startsWith("replace ") || command.startsWith("append ") ||
				command.startsWith("prepend ") || command.startsWith("cas ")) {
			try {
				handleStorageCommands(handler, command);
				Data data = (Data) handler.getClientData();
				if(data.isAllDataIn()) {
					processStorageCommands(handler);
					return;
				} else {
					return;
				}
			} catch(Exception e) {
				logger.warning("Error: "+e);
				sendResponse(handler, "ERROR\r\n");
			}
		} else if(command.startsWith("get ") || command.startsWith("gets ")) {
			try {
				handleGetCommands(handler, command);
			} catch(Exception e) {
				logger.warning("Error: "+e);
				sendResponse(handler, "ERROR\r\n");
			}
		} else if(command.startsWith("delete ")) {
			try {
				handleDeleteCommands(handler, command);
			} catch(Exception e) {
				logger.warning("Error: "+e);
				sendResponse(handler, "ERROR\r\n");
			}
		} else if(command.startsWith("flush_all")) {
			handleFlushAll(command);
			sendResponse(handler, "OK\r\n");
		} else if(command.equals("stats")) {			
			Map stats = CommandHandler.getStats(handler.getServer());
			Set keySet = stats.keySet();
			Iterator iterator = keySet.iterator();
			String key = null;
			String value = null;
			while(iterator.hasNext()) {
				key = (String) iterator.next();
				value = (String) stats.get(key);
				sendResponse(handler, "STAT " + key + " " + value + "\r\n");
			}
			sendResponse(handler, "END\r\n");
		} else if(command.startsWith("stats ")) {
			//todo
		} else if(command.equals("version")) {
			sendResponse(handler, "VERSION "+QuickCached.version+"\r\n");
		} else if(command.equals("quit")) {
			handler.closeConnection();
		} else {
			logger.warning("unknown command!");
			sendResponse(handler, "ERROR\r\n");
		}

		//Increment/Decrement
	}

	private void handleFlushAll(String command)
			throws SocketTimeoutException, IOException {
		/*
		flush_all [exptime] [noreply]\r\n
		*/
		String cmdData[] = command.split(" ");
		String cmd = cmdData[0];
		String exptime = null;

		if(cmdData.length>=2) exptime = cmdData[1];

		cache.flush();
		
		boolean noreplay = false;
		if(cmdData.length==3) {
			if("noreply".equals(cmdData[2])) {
				noreplay = true;
			}
		}
	}

	private void handleDeleteCommands(ClientHandler handler, String command)
			throws SocketTimeoutException, IOException {
		/*
		delete <key> [noreply]\r\n
		*/
		String cmdData[] = command.split(" ");

		String cmd = cmdData[0];
		String key = cmdData[1];

		boolean noreplay = false;
		if(cmdData.length==3) {
			if("noreply".equals(cmdData[2])) {
				noreplay = true;
			}
		}
		Object obj = cache.delete(key);
		if(obj!=null) {
			sendResponse(handler, "DELETED\r\n");
		} else {
			sendResponse(handler, "NOT_FOUND\r\n");
		}
	}

	private void handleGetCommands(ClientHandler handler, String command)
			throws SocketTimeoutException, IOException {
		/*
		get <key>*\r\n
		gets <key>*\r\n
		*/
		String cmdData[] = command.split(" ");

		String cmd = cmdData[0];
		String key = null;

		for(int i=1;i<cmdData.length;i++) {
			key = cmdData[i];
			DataCarrier dc = (DataCarrier) cache.get(key);
			if(dc!=null) {
				StringBuilder sb = new StringBuilder();
				sb.append("VALUE ");
				sb.append(key);
				sb.append(" ");
				sb.append(dc.getFlags());
				sb.append(" ");
				sb.append(dc.getData().length);
				sb.append("\r\n");
				sendResponse(handler, sb.toString());
				sendResponse(handler, dc.getData());
				sendResponse(handler, "\r\n");
			}
		}
		sendResponse(handler, "END\r\n");

		/*
		VALUE <key> <flags> <bytes> [<cas unique>]\r\n
		<data block>\r\n
		*/
	}

	private void handleStorageCommands(ClientHandler handler, String command)
			throws SocketTimeoutException, IOException {
		Data data = (Data) handler.getClientData();
		/*
		<command name> <key> <flags> <exptime> <bytes> [noreply]\r\n
		           cas <key> <flags> <exptime> <bytes> <cas unique> [noreply]\r\n
		*/
		String cmdData[] = command.split(" ");

		String cmd = cmdData[0];
		String key = cmdData[1];
		String flags = cmdData[2];
		long exptime = Integer.parseInt(cmdData[3]);
		long bytes = Integer.parseInt(cmdData[4]);

		boolean noreplay = false;
		if(cmdData.length==6) {
			if("noreply".equals(cmdData[5])) {
				noreplay = true;
			}
		}

		data.setCmd(cmd);
		data.setKey(key);
		data.setFlags(flags);
		data.setExptime(exptime);
		data.setDataRequiredLength(bytes);
		data.setNoreplay(noreplay);
	}

	public void processStorageCommands(ClientHandler handler)
			throws SocketTimeoutException, IOException {
		Data data = (Data) handler.getClientData();

		byte dataToStore[] = data.getDataByte();

		DataCarrier dc = new DataCarrier(dataToStore);
		dc.setFlags(data.getFlags());

		cache.set(data.getKey(), dc, data.getExptime());
		if(data.isNoreplay()==false) {
			sendResponse(handler, "STORED\r\n");
		}
		data.clear();
	}

	public void sendResponse(ClientHandler handler, String data) throws SocketTimeoutException, IOException {
		sendResponse(handler, data.getBytes());
	}

	public void sendResponse(ClientHandler handler, byte data[]) throws SocketTimeoutException, IOException {
		if(QuickCached.DEBUG) logger.fine("S: "+new String(data));
		handler.sendClientBinary(data);
	}
}
