package org.quickcached;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quickcached.cache.CacheInterface;
import org.quickserver.net.server.ClientHandler;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Akshathkumar Shetty
 */
public class TextCommandProcessor {
	private static final Logger logger = Logger.getLogger(TextCommandProcessor.class.getName());
	
	private static String versionOutput = null;
	
	static {
		versionOutput = "VERSION " + QuickCached.version + "\r\n";
	}
	
	private CacheInterface cache;

	public void setCache(CacheInterface cache) {
		this.cache = cache;
	}

	public void handleTextCommand(ClientHandler handler, String command)
			throws SocketTimeoutException, IOException {
		if (QuickCached.DEBUG) {
			logger.log(Level.FINE, "command: {0}", command);
		} 

		if (command.equals("version")) {
			sendResponse(handler, versionOutput);
		} else if (command.startsWith("set ") || command.startsWith("add ")
				|| command.startsWith("replace ") || command.startsWith("append ")
				|| command.startsWith("prepend ") || command.startsWith("cas ")) {
			try {
				handleStorageCommands(handler, command);
				Data data = (Data) handler.getClientData();
				if (data.isAllDataIn()) {
					processStorageCommands(handler);
					return;
				} else {
					return;
				}
			} catch (Exception e) {
				logger.warning("Error: " + e);
				sendResponse(handler, "SERVER_ERROR " + e + "\r\n");
			}
		} else if (command.startsWith("get ") || command.startsWith("gets ")) {
			try {
				handleGetCommands(handler, command);
			} catch (Exception e) {
				logger.warning("Error: " + e);
				sendResponse(handler, "SERVER_ERROR " + e + "\r\n");
			}
		} else if (command.startsWith("delete ")) {
			try {
				handleDeleteCommands(handler, command);
			} catch (Exception e) {
				logger.warning("Error: " + e);
				sendResponse(handler, "SERVER_ERROR " + e + "\r\n");
			}
		} else if (command.startsWith("flush_all")) {
			handleFlushAll(command);
			sendResponse(handler, "OK\r\n");
		} else if (command.equals("stats")) {
			Map stats = CommandHandler.getStats(handler.getServer());
			Set keySet = stats.keySet();
			Iterator iterator = keySet.iterator();
			String key = null;
			String value = null;
			while (iterator.hasNext()) {
				key = (String) iterator.next();
				value = (String) stats.get(key);
				sendResponse(handler, "STAT " + key + " " + value + "\r\n");
			}
			sendResponse(handler, "END\r\n");
		} else if (command.startsWith("stats ")) {
			//todo
			sendResponse(handler, "ERROR\r\n");
		} else if (command.equals("quit")) {
			handler.closeConnection();
		} else if (command.startsWith("incr ") || command.startsWith("decr ")) {
			try {
				handleIncrDecrCommands(handler, command);
			} catch (Exception e) {
				logger.warning("Error: " + e);
				sendResponse(handler, "SERVER_ERROR " + e + "\r\n");
			}
		} else {
			logger.warning("unknown command! "+command);
			sendResponse(handler, "ERROR\r\n");
		}
	}

	private void handleFlushAll(String command)
			throws SocketTimeoutException, IOException {
		/*
		flush_all [exptime] [noreply]\r\n
		 */
		String cmdData[] = command.split(" ");
		String cmd = cmdData[0];
		String exptime = null;
		
		if(QuickCached.DEBUG==false) {
			logger.log(Level.FINE, "cmd: {0}", new Object[]{cmd});
		}

		if (cmdData.length >= 2) {
			exptime = cmdData[1];
		}

		if (exptime == null) {
			cache.flush();
		} else {
			final int sleeptime = Integer.parseInt(exptime);
			Thread t = new Thread() {

				public void run() {
					try {
						sleep(1000 * sleeptime);
					} catch (InterruptedException ex) {
						Logger.getLogger(TextCommandProcessor.class.getName()).log(Level.SEVERE, null, ex);
					}
					cache.flush();
				}
			};
			t.start();
		}

		boolean noreplay = false;
		if (cmdData.length == 3) {
			if ("noreply".equals(cmdData[2])) {
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
		
		if(QuickCached.DEBUG==false) {
			logger.log(Level.FINE, "cmd: {0}, key: {1}", new Object[]{cmd, key});
		}

		boolean noreplay = false;
		if (cmdData.length == 3) {
			if ("noreply".equals(cmdData[2])) {
				noreplay = true;
			}
		}
		boolean flag = cache.delete(key);
		if (noreplay) {
			return;
		}

		if (flag == true) {
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

		for (int i = 1; i < cmdData.length; i++) {
			key = cmdData[i];
			if(QuickCached.DEBUG==false) {
				logger.log(Level.FINE, "cmd: {0}, key: {1}", new Object[]{cmd, key});
			}
			DataCarrier dc = (DataCarrier) cache.get(key);
			if (dc != null) {
				StringBuilder sb = new StringBuilder();
				sb.append("VALUE ");
				sb.append(key);
				sb.append(" ");
				sb.append(dc.getFlags());
				sb.append(" ");
				sb.append(dc.getData().length);
				sb.append(" ");
				sb.append(dc.getCas());
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

	private void handleIncrDecrCommands(ClientHandler handler, String command)
			throws SocketTimeoutException, IOException {
		/*
		incr <key> <value> [noreply]\r\n
		decr <key> <value> [noreply]\r\n
		 */
		String cmdData[] = command.split(" ");

		if (cmdData.length < 3) {
			sendResponse(handler, "CLIENT_ERROR Bad number of args passed\r\n");
			return;
		}

		String cmd = cmdData[0];
		String key = cmdData[1];
		String _value = cmdData[2];
		long value = 0;
		try {
			value = Long.parseLong(_value);
		} catch (Exception e) {
			sendResponse(handler, "CLIENT_ERROR parse of client value failed\r\n");
			return;
		}
		
		if(QuickCached.DEBUG==false) {
			logger.log(Level.FINE, "cmd: {0}, key: {1}", new Object[]{cmd, key});
		}

		boolean noreplay = false;
		if (cmdData.length >= 4) {
			if ("noreply".equals(cmdData[3])) {
				noreplay = true;
			}
		}


		DataCarrier dc = (DataCarrier) cache.get(key);
		if (dc == null) {
			if (noreplay == false) {
				sendResponse(handler, "NOT_FOUND\r\n");
			}
			return;
		}

		synchronized (key) {
			try {
				long oldvalue = Long.parseLong(new String(dc.getData()));
				if (cmd.equals("incr")) {
					value = oldvalue + value;
				} else if (cmd.equals("decr")) {
					value = oldvalue - value;
					if (value < 0) {
						value = 0;
					}
				}
				dc.setData(("" + value).getBytes("utf-8"));
			} catch (Exception e) {
				if (noreplay == false) {
					sendResponse(handler, "CLIENT_ERROR parse of server value failed\r\n");
				}
				return;
			}
		}
		
		cache.update(key, dc, dc.getSize());

		if (noreplay) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(value);
		sb.append("\r\n");
		sendResponse(handler, sb.toString());
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
		String casunique = null;

		boolean noreplay = false;
		if (cmdData.length >= 6) {
			if ("noreply".equals(cmdData[5])) {
				noreplay = true;
			} else {
				casunique = cmdData[5];
			}

			if (cmdData.length >= 7) {
				if ("noreply".equals(cmdData[6])) {
					noreplay = true;
				}
			}
		}

		data.setCmd(cmd);
		data.setKey(key);
		data.setFlags(flags);
		data.setExptime(exptime);
		data.setDataRequiredLength(bytes);
		data.setCasUnique(casunique);
		data.setNoreplay(noreplay);
	}

	public void processStorageCommands(ClientHandler handler)
			throws SocketTimeoutException, IOException {
		Data data = (Data) handler.getClientData();
		
		if(QuickCached.DEBUG==false) {
			logger.log(Level.FINE, "cmd: {0}, key: {1}", new Object[]{data.getCmd(), data.getKey()});
		}

		byte dataToStore[] = data.getDataByte();

		DataCarrier dc = new DataCarrier(dataToStore);
		dc.setFlags(data.getFlags());

		if (data.getCmd().equals("set")) {
			cache.set(data.getKey(), dc, dc.getSize(), data.getExptime());
			if (data.isNoreplay() == false) {
				sendResponse(handler, "STORED\r\n");
			}
		} else if (data.getCmd().equals("add")) {
			Object olddata = cache.get(data.getKey());
			if (olddata == null) {
				cache.set(data.getKey(), dc, dc.getSize(), data.getExptime());
				if (data.isNoreplay() == false) {
					sendResponse(handler, "STORED\r\n");
				}
			} else {
				if (data.isNoreplay() == false) {
					sendResponse(handler, "NOT_STORED\r\n");
				}
			}
		} else if (data.getCmd().equals("replace")) {
			Object olddata = cache.get(data.getKey());
			if (olddata != null) {
				cache.update(data.getKey(), dc, dc.getSize());
				if (data.isNoreplay() == false) {
					if (data.isNoreplay() == false) {
						sendResponse(handler, "STORED\r\n");
					}
				}
			} else {
				if (data.isNoreplay() == false) {
					sendResponse(handler, "NOT_STORED\r\n");
				}
			}
		} else if (data.getCmd().equals("append")) {
			DataCarrier olddata = (DataCarrier) cache.get(data.getKey());
			if (olddata != null) {
				olddata.append(dc.getData());
				cache.update(data.getKey(), olddata, olddata.getSize());
				
				dc.setData(null);
				dc = null;

				if (data.isNoreplay() == false) {
					if (data.isNoreplay() == false) {
						sendResponse(handler, "STORED\r\n");
					}
				}
			} else {
				if (data.isNoreplay() == false) {
					sendResponse(handler, "NOT_STORED\r\n");
				}
			}
		} else if (data.getCmd().equals("prepend")) {
			DataCarrier olddata = (DataCarrier) cache.get(data.getKey());
			if (olddata != null) {
				olddata.prepend(dc.getData());
				cache.update(data.getKey(), olddata, olddata.getSize());
				dc.setData(null);
				dc = null;

				if (data.isNoreplay() == false) {
					if (data.isNoreplay() == false) {
						sendResponse(handler, "STORED\r\n");
					}
				}
			} else {
				if (data.isNoreplay() == false) {
					sendResponse(handler, "NOT_STORED\r\n");
				}
			}
		} else if (data.getCmd().equals("cas")) {
			DataCarrier olddata = (DataCarrier) cache.get(data.getKey());
			if (olddata != null) {
				int oldcas = olddata.getCas();
				int passedcas = Integer.parseInt(data.getCasUnique());

				if (oldcas == passedcas) {
					cache.set(data.getKey(), dc, dc.getSize(), data.getExptime());
					if (data.isNoreplay() == false) {
						sendResponse(handler, "STORED\r\n");
					}
				} else {
					if (data.isNoreplay() == false) {
						sendResponse(handler, "EXISTS\r\n");
					}
				}
			} else {
				if (data.isNoreplay() == false) {
					sendResponse(handler, "NOT_FOUND\r\n");
				}
			}
		}

		data.clear();
	}

	public void sendResponse(ClientHandler handler, String data) throws SocketTimeoutException, IOException {
		sendResponse(handler, data.getBytes());
	}

	public void sendResponse(ClientHandler handler, byte data[]) throws SocketTimeoutException, IOException {
		if (QuickCached.DEBUG) {
			logger.log(Level.FINE, "S: {0}", new String(data));
		} else {
			logger.log(Level.FINE, "S: {0} bytes", data.length);
		}
		handler.sendClientBinary(data);
	}
}
