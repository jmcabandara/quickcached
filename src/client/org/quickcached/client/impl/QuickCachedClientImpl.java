package org.quickcached.client.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quickcached.client.MemcachedClient;
import org.quickcached.client.TimeoutException;
import org.quickserver.net.client.BlockingClient;
import org.quickserver.net.client.ClientInfo;
import org.quickserver.net.client.Host;
import org.quickserver.net.client.HostList;
import org.quickserver.net.client.SocketBasedHost;
import org.quickserver.net.client.loaddistribution.LoadDistributor;
import org.quickserver.net.client.loaddistribution.impl.HashedLoadPattern;
import org.quickserver.net.client.monitoring.HostMonitor;
import org.quickserver.net.client.monitoring.HostStateListener;
import org.quickserver.net.client.monitoring.impl.SocketMonitor;
import org.quickserver.net.client.pool.BlockingClientPool;
import org.quickserver.net.client.pool.PoolableBlockingClient;
import org.quickserver.net.client.pool.PooledBlockingClient;

/**
 *
 * @author akshath
 */
public class QuickCachedClientImpl extends MemcachedClient {
	private static final Logger logger = Logger.getLogger(QuickCachedClientImpl.class.getName());
	
	private static final int FLAGS_GENRIC_STRING = 0;
	private static final int FLAGS_GENRIC_OBJECT = 1;
	
	private static String charset = "ISO-8859-1";//"utf-8";
	
	private String hostList;
	private boolean binaryConnection = false;
	
	private int poolSize = 5;
	
	private int minPoolSize = 4;
	private int idlePoolSize = 8;
	private int maxPoolSize = 16;
	
	private long noOpTimeIntervalMiliSec = 1000*60;//60 sec
	private int hostMonitoringIntervalInSec = 15;//15sec
	private int maxIntervalForBorrowInSec = 4;//4 sec
	private int logPoolIntervalTimeMin = 10;//10min
	
	private BlockingClientPool blockingClientPool;
	private HostList hostListObj;
	private boolean debug;
	
	private void updatePoolSizes() {
		minPoolSize = poolSize/2;
		idlePoolSize = poolSize;
		maxPoolSize = poolSize*2;
	}

	public void setUseBinaryConnection(boolean flag) {
		binaryConnection = flag;
	}
	
	public void setConnectionPoolSize(int size) {
		poolSize = size;
		updatePoolSizes();
	}

	public void setAddresses(String list) {
		hostList = list;
	}

	public void addServer(String list) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void removeServer(String list) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void init() throws IOException {
		hostListObj = new HostList("memcached_"+hostList);
		updatePoolSizes();
		
		String servers[] = hostList.split(" ");
		String server[] = null;
		SocketBasedHost sbh = null;
		for(int i=0;i<servers.length;i++) {
			server = servers[i].split(":");
			try {
				sbh = new SocketBasedHost(server[0].trim(), 
					Integer.parseInt(server[1].trim()));
			} catch (Exception ex) {
				Logger.getLogger(QuickCachedClientImpl.class.getName()).log(
					Level.SEVERE, "Erro: "+ex, ex);
			}
			sbh.setTimeout((int)getDefaultTimeoutMiliSec());
			sbh.setRequestText("version\r\n");
			sbh.setResponseTextToExpect("VERSION ");
			
			hostListObj.add(sbh);
		}
		
		final SocketMonitor sm = new SocketMonitor();
		
		final LoadDistributor ld = new LoadDistributor(hostListObj);
		ld.setLoadPattern(new HashedLoadPattern());
		
		PoolableBlockingClient poolableBlockingClient = new PoolableBlockingClient() {
			public HostMonitor getHostMonitor() {
				return sm;
			}

			public LoadDistributor getLoadDistributor() {
				return ld;
			}

			public BlockingClient createBlockingClient(SocketBasedHost host) {
				BlockingClient bc = new BlockingClient();
				try {
					bc.connect(host.getInetAddress().getHostAddress(), host
							.getInetSocketAddress().getPort());
					bc.getSocket().setTcpNoDelay(true);
					bc.getSocket().setSoTimeout((int)getDefaultTimeoutMiliSec());
					bc.getSocket().setSoLinger(true, 10);
					return bc;
				} catch (Exception ex) {
					logger.log(Level.WARNING, "Error: "+ex, ex);
					return null;
				}
			}

			public boolean closeBlockingClient(BlockingClient blockingClient) {
				if (blockingClient == null)
					return false;
				try {
					blockingClient.close();
					return true;
				} catch (IOException ex) {
					logger.log(Level.WARNING, "Error: "+ex, ex);
				}
				return false;
			}

			public boolean sendNoOp(BlockingClient blockingClient) {
				if(blockingClient==null) return false;
				
				try {
					blockingClient.sendBytes("version\r\n", charset);
					String recData = blockingClient.readCRLFLine();
					if(recData==null) return false;

					if(recData.startsWith("VERSION ")) {
						return true;
					} else {
						return false;
					}
				} catch(IOException e) {
					logger.log(Level.WARNING, "Error: "+e, e);
					return false;
				}
			}

			public long getNoOpTimeIntervalMiliSec() {
				return noOpTimeIntervalMiliSec;
			}

			public int getHostMonitoringIntervalInSec() {
				return hostMonitoringIntervalInSec;
			}

			public boolean isBlockWhenEmpty() {
				return false;
			}

			public int getMaxIntervalForBorrowInSec() {
				return maxIntervalForBorrowInSec;
			}
		};
		
		blockingClientPool = new BlockingClientPool("memcached_"+hostList,
				poolableBlockingClient);
		blockingClientPool.setDebug(isDebug());

		blockingClientPool.setMinPoolSize(minPoolSize);
		blockingClientPool.setIdlePoolSize(idlePoolSize);
		blockingClientPool.setMaxPoolSize(maxPoolSize);

		HostStateListener hsl = new HostStateListener() {
			public void stateChanged(Host host, char oldstatus, char newstatus) {
				if (oldstatus != Host.UNKNOWN) {
					logger.log(Level.SEVERE, "State changed: {0}; old state: {1};new state: {2}", 
						new Object[]{host, oldstatus, newstatus});
				} else {
					logger.log(Level.INFO, "State changed: {0}; old state: {1};new state: {2}", 
						new Object[]{host, oldstatus, newstatus});
				}
			}
		};
		blockingClientPool.getHostMonitoringService().addHostStateListner(hsl);
		blockingClientPool.setLogPoolStatsTimeInMinute(logPoolIntervalTimeMin);
		blockingClientPool.init();
	}

	public void stop() throws IOException {
		blockingClientPool.close();
		blockingClientPool = null;
	}
	
	private String sendDataOut(String key, String data) throws TimeoutException {
		ClientInfo ci = new ClientInfo();
		ci.setClientKey(key);

		PooledBlockingClient pbc = null;

		try {
			pbc = blockingClientPool.getBlockingClient(ci);
			if(pbc==null) {
				throw new TimeoutException("sdo: we do not have any client[pbc] to connect to server!");
			}

			BlockingClient bc = pbc.getBlockingClient();
			if(bc==null) {
				throw new TimeoutException("we do not have any client[bc] to connect to server!");
			}

			bc.sendBytes(data, charset);
			
			return bc.readCRLFLine();
		} catch(IOException e) {
			if(pbc!=null) {
				logger.log(Level.WARNING, "We had an ioerror will close client! "+e, e);
				pbc.close();
			}
			throw new TimeoutException("We had ioerror "+e);
		} finally {
			if(pbc!=null) {
				blockingClientPool.returnBlockingClient(pbc);
			}
		}
	}
	
	private Object readDataOut(String key, String data) throws TimeoutException {
		ClientInfo ci = new ClientInfo();
		ci.setClientKey(key);

		PooledBlockingClient pbc = null;

		try {
			pbc = blockingClientPool.getBlockingClient(ci);
			if(pbc==null) {
				throw new TimeoutException("rdo: we do not have any client[pbc] to connect to server!");
			}

			BlockingClient bc = pbc.getBlockingClient();
			if(bc==null) {
				throw new TimeoutException("we do not have any client[bc] to connect to server!");
			}

			bc.sendBytes(data, charset);
			
			String resMain = bc.readCRLFLine();
			if(resMain==null) {
				throw new TimeoutException("we got null reply!");
			}
			
			/*
VALUE <key> <flags> <bytes> [<cas unique>]\r\n
<data block>\r\n
END\r\n
			 */
			
			if(resMain.startsWith("VALUE ")) {
				String cmdData[] = resMain.split(" ");
				if(cmdData.length<4) {
					return null;
				}
				int flag = Integer.parseInt(cmdData[2]);
				int bytes = Integer.parseInt(cmdData[3]);
				
				byte[] dataBuff = bc.readBytes(bytes);
				
				//read the footer 7 char extra \r\nEND\r\n
				bc.readBytes(7);
				
				if(dataBuff==null) {
					throw new TimeoutException("we don't have data!");
				}
				
				if(flag == FLAGS_GENRIC_STRING) {
					return new String(dataBuff, charset);
				} else {
					return retriveObject(dataBuff);
				}
			} else if(resMain.equals("END")){
				return null;
			} else {
				logger.log(Level.WARNING, "unknown res got! : {0}", resMain);
				throw new TimeoutException("unknown res got! : "+resMain);
			}
		} catch(IOException e) {
			if(pbc!=null) {
				logger.log(Level.WARNING, "We had an ioerror will close client! "+e, e);
				pbc.close();
			}
			throw new TimeoutException("We had ioerror "+e);
		} finally {
			if(pbc!=null) {
				blockingClientPool.returnBlockingClient(pbc);
			}
		}
	}

	public void set(String key, int ttlSec, Object value, long timeoutMiliSec) throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			
			byte valueBytes[] = null;
			int flag = -1;
			
			if(value instanceof String) {
				String arg = (String) value;
				valueBytes = arg.getBytes(charset);
				flag = FLAGS_GENRIC_STRING;
			} else {
				valueBytes = getObjectBytes(value);
				flag = FLAGS_GENRIC_OBJECT;
			}
			
			//<command name> <key> <flags> <exptime> <bytes> [noreply]\r\n
			sb.append("set ").append(key).append(" ").append(flag);
			sb.append(" ").append(ttlSec).append(" ").append(valueBytes.length);
			sb.append("\r\n");
			sb.append(new String(valueBytes, charset));
			sb.append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);

			String res = sendDataOut(key, sb.toString());
			if(res==null) {
				throw new TimeoutException("we got a null reply!");
			}

			if(res.equals("STORED")==false) {
				throw new TimeoutException(key+" was not stored["+res+"]");
			}
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		} 		
	}
	
	public boolean add(String key, int ttlSec, Object value, long timeoutMiliSec) 
			throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			
			byte valueBytes[] = null;
			int flag = -1;
			
			if(value instanceof String) {
				String arg = (String) value;
				valueBytes = arg.getBytes(charset);
				flag = FLAGS_GENRIC_STRING;
			} else {
				valueBytes = getObjectBytes(value);
				flag = FLAGS_GENRIC_OBJECT;
			}
			
			//<command name> <key> <flags> <exptime> <bytes> [noreply]\r\n
			sb.append("add ").append(key).append(" ").append(flag);
			sb.append(" ").append(ttlSec).append(" ").append(valueBytes.length);
			sb.append("\r\n");
			sb.append(new String(valueBytes, charset));
			sb.append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);
			
			String res = sendDataOut(key, sb.toString());
			if(res==null) {
				throw new TimeoutException("we got a null reply!");
			}

			if(res.equals("STORED")==false) {
				return false;
			} else {
				return true;
			}
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
	}
	
	public boolean replace(String key, int ttlSec, Object value, long timeoutMiliSec) 
			throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			
			byte valueBytes[] = null;
			int flag = -1;
			
			if(value instanceof String) {
				String arg = (String) value;
				valueBytes = arg.getBytes(charset);
				flag = FLAGS_GENRIC_STRING;
			} else {
				valueBytes = getObjectBytes(value);
				flag = FLAGS_GENRIC_OBJECT;
			}
			
			//<command name> <key> <flags> <exptime> <bytes> [noreply]\r\n
			sb.append("replace ").append(key).append(" ").append(flag);
			sb.append(" ").append(ttlSec).append(" ").append(valueBytes.length);
			sb.append("\r\n");
			sb.append(new String(valueBytes, charset));
			sb.append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);
			
			String res = sendDataOut(key, sb.toString());
			if(res==null) {
				throw new TimeoutException("we got a null reply!");
			}

			if(res.equals("STORED")==false) {
				return false;
			} else {
				return true;
			}
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
	}
	
	@Override
	public boolean append(String key, Object value, long timeoutMiliSec) throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			
			byte valueBytes[] = null;
			int flag = -1;
			
			if(value instanceof String) {
				String arg = (String) value;
				valueBytes = arg.getBytes(charset);
				flag = FLAGS_GENRIC_STRING;
			} else {
				valueBytes = getObjectBytes(value);
				flag = FLAGS_GENRIC_OBJECT;
			}
			
			//<command name> <key> <flags> <exptime> <bytes> [noreply]\r\n
			sb.append("append ").append(key).append(" ").append(flag);
			sb.append(" ").append("0").append(" ").append(valueBytes.length);
			sb.append("\r\n");
			sb.append(new String(valueBytes, charset));
			sb.append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);
			
			String res = sendDataOut(key, sb.toString());
			if(res==null) {
				throw new TimeoutException("we got a null reply!");
			}

			if(res.equals("STORED")==false) {
				return false;
			} else {
				return true;
			}
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
	}
	
	public boolean append(long cas, String key, Object value, long timeoutMiliSec) 
			throws TimeoutException {
		return append(key, value, timeoutMiliSec);
	}
	
	@Override
	public boolean prepend(String key, Object value, long timeoutMiliSec) throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			
			byte valueBytes[] = null;
			int flag = -1;
			
			if(value instanceof String) {
				String arg = (String) value;
				valueBytes = arg.getBytes(charset);
				flag = FLAGS_GENRIC_STRING;
			} else {
				valueBytes = getObjectBytes(value);
				flag = FLAGS_GENRIC_OBJECT;
			}
			
			//<command name> <key> <flags> <exptime> <bytes> [noreply]\r\n
			sb.append("prepend ").append(key).append(" ").append(flag);
			sb.append(" ").append("0").append(" ").append(valueBytes.length);
			sb.append("\r\n");
			sb.append(new String(valueBytes, charset));
			sb.append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);
			
			String res = sendDataOut(key, sb.toString());
			if(res==null) {
				throw new TimeoutException("we got a null reply!");
			}

			if(res.equals("STORED")==false) {
				return false;
			} else {
				return true;
			}
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
	}
	
	public boolean prepend(long cas, String key, Object value, long timeoutMiliSec) 
			throws TimeoutException {
		return prepend(key, value, timeoutMiliSec);
	}

	public Object get(String key, long timeoutMiliSec) throws TimeoutException {
		Object readObject = null;
		try {
			StringBuilder sb = new StringBuilder();
			//get <key>*\r\n
			sb.append("get ").append(key).append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);
			
			readObject = readDataOut(key, sb.toString());
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
		return readObject;
	}
	
	private String sendCmdOut(String key, String data) throws TimeoutException {
		ClientInfo ci = new ClientInfo();
		ci.setClientKey(key);

		PooledBlockingClient pbc = null;

		try {
			pbc = blockingClientPool.getBlockingClient(ci);
			if(pbc==null) {
				throw new TimeoutException("cmo: we do not have any client[pbc] to connect to server!");
			}

			BlockingClient bc = pbc.getBlockingClient();
			if(bc==null) {
				throw new TimeoutException("we do not have any client[bc] to connect to server!");
			}

			bc.sendBytes(data, charset);
			
			String resMain = bc.readCRLFLine();
			if(resMain==null) {
				throw new TimeoutException("we got null reply!");
			}
			
			return resMain;
		} catch(IOException e) {
			if(pbc!=null) {
				logger.log(Level.WARNING, "We had an ioerror will close client! "+e, e);
				pbc.close();
			}
			throw new TimeoutException("We had ioerror "+e);
		} finally {
			if(pbc!=null) {
				blockingClientPool.returnBlockingClient(pbc);
			}
		}
	}

	public boolean delete(String key, long timeoutMiliSec) throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			//delete <key> [noreply]\r\n
			sb.append("delete ").append(key).append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);
			
			String res = sendCmdOut(key, sb.toString());
			if(res==null) {
				throw new TimeoutException("we got a null reply!");
			}
			
			if(res.equals("DELETED")) {
				return true;
			} else {
				return false;
			}
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
	}
	
	private void sendCmdOutToAll(String data) throws TimeoutException {
		PooledBlockingClient pbc[] = blockingClientPool.getOneBlockingClientForAllActiveHosts();
		if(pbc==null) {
			throw new TimeoutException("we do not have any client array [pbc] to connect to server!");
		}

		for(int i=0;i<pbc.length;i++) {
			try {
				BlockingClient bc = pbc[i].getBlockingClient();
				if(bc==null) {
					throw new TimeoutException("we do not have any client[bc] to connect to server!");
				}

				bc.sendBytes(data, charset);
			} catch(IOException e) {
				if(pbc!=null) {
					logger.log(Level.WARNING, "We had an ioerror will close client! "+e, e);
					pbc[i].close();
				}
			} finally {
				blockingClientPool.returnBlockingClient(pbc[i]);
				pbc[i] = null;
			}
		}
		
	}

	public void flushAll() throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			//noreply [noreply]\r\n
			sb.append("flush_all noreply").append("\r\n");			
			
			sendCmdOutToAll(sb.toString());
			
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
	}
	
	public Object getBaseClient() {
		return null;
	}
        
	public Map getStats() throws Exception {
		Map<InetSocketAddress,Map<String,String>> map = new HashMap<InetSocketAddress,Map<String,String>>();
		
		PooledBlockingClient pbc[] = blockingClientPool.getOneBlockingClientForAllActiveHosts();
		if(pbc==null) {
			throw new TimeoutException("we do not have any client array [pbc] to connect to server!");
		}
		
		Map<String,String> inmap = null;
		for(int i=0;i<pbc.length;i++) {
			try {
				BlockingClient bc = pbc[i].getBlockingClient();
				if(bc==null) {
					throw new TimeoutException("we do not have any client[bc] to connect to server!");
				}
				
				inmap = getStats(bc);
				
				map.put(pbc[i].getSocketBasedHost().getInetSocketAddress(), inmap);
			} catch(IOException e) {
				if(pbc!=null) {
					logger.log(Level.WARNING, "We had an ioerror will close client! "+e, e);
					pbc[i].close();
				}
			} finally {
				blockingClientPool.returnBlockingClient(pbc[i]);
				pbc[i] = null;
			}
		}
		
		return map;
	}
	
	private Map<String,String> getStats(BlockingClient bc) throws Exception {
		Map<String,String> map = new HashMap<String,String>();
		try {
			StringBuilder sb = new StringBuilder();
			//stats\r\n
			sb.append("stats").append("\r\n");			
			
			bc.sendBytes(sb.toString(), charset);
			
			String line = null;
			String stats[] = null;
			while(true) {
				line = bc.readCRLFLine();
				if(line==null || line.equals("END")) {
					break;
				}
				//STAT <name> <value>\r\n
				if(line.startsWith("STAT ")==false) {
					throw new Exception("We had bad stats output!"+line);
				}
				stats = line.split(" ");
				map.put(stats[1], stats[2]);
			}
			
			return map;
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}		
	}
	
	public void increment(String key, int value, long timeoutMiliSec) 
			throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			//incr <key> <value> [noreply]\r\n
			sb.append("incr ").append(key).append(" ").append(value).append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);
			
			String res = sendCmdOut(key, sb.toString());
			if(res==null) {
				throw new TimeoutException("we got a null reply!");
			}
			
			if(res.equals("NOT_FOUND")) {
				throw new TimeoutException("key["+key+"] not found on server!");
			}
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
	}
	
	public void decrement(String key, int value, long timeoutMiliSec) 
			throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			//decr <key> <value> [noreply]\r\n
			sb.append("decr ").append(key).append(" ").append(value).append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);
			
			String res = sendCmdOut(key, sb.toString());
			if(res==null) {
				throw new TimeoutException("we got a null reply!");
			}
			
			if(res.equals("NOT_FOUND")) {
				throw new TimeoutException("key["+key+"] not found on server!");
			}
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
	}
	
	public Map getVersions() throws TimeoutException {
		Map<InetSocketAddress,String> map = new HashMap<InetSocketAddress,String>();
		
		PooledBlockingClient pbc[] = blockingClientPool.getOneBlockingClientForAllActiveHosts();
		if(pbc==null) {
			throw new TimeoutException("we do not have any client array [pbc] to connect to server!");
		}
		
		String version = null;
		for(int i=0;i<pbc.length;i++) {
			try {
				BlockingClient bc = pbc[i].getBlockingClient();
				if(bc==null) {
					throw new TimeoutException("we do not have any client[bc] to connect to server!");
				}
				
				version = getVersion(bc);
				
				map.put(pbc[i].getSocketBasedHost().getInetSocketAddress(), version);
			} catch(IOException e) {
				if(pbc!=null) {
					logger.log(Level.WARNING, "We had an ioerror will close client! "+e, e);
					pbc[i].close();
				}
			} catch(TimeoutException e) {
				if(pbc!=null) {
					logger.log(Level.WARNING, "We had an timeout will close client! "+e, e);
					pbc[i].close();
				}
			} finally {
				blockingClientPool.returnBlockingClient(pbc[i]);
				pbc[i] = null;
			}
		}
		
		return map;
	}
	
	private String getVersion(BlockingClient bc) throws IOException, TimeoutException  {
		Map<String,String> map = new HashMap<String,String>();
		try {
			StringBuilder sb = new StringBuilder();
			//version\r\n
			sb.append("version").append("\r\n");			
			
			bc.sendBytes(sb.toString(), charset);
			
			String line =  bc.readCRLFLine();
			if(line==null) {
				throw new TimeoutException("We had EOF");
			}
			//VERSION <version>\r\n
			
			return line.substring(8);
		} catch (TimeoutException ex) {
			throw ex;
		} catch (IOException ex) {
			throw ex;
		}		
	}

	/**
	 * @return the debug
	 */
	public boolean isDebug() {
		return debug;
	}

	/**
	 * @param debug the debug to set
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}
	
	protected static byte[] getObjectBytes(Object object) {
		ObjectOutputStream out = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			out = new ObjectOutputStream(bos);
			out.writeObject(object);
			return bos.toByteArray();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error: "+e, e);
		} finally {
			if(out!=null) {
				try {
					out.close();
				} catch (IOException ex) {
					logger.log(Level.WARNING, "Error: "+ex, ex);
				}
			}
		}
		return null;
	}

	protected static Object retriveObject(byte bytes[]) {
		ObjectInputStream in = null;
		try {
			Object object;
			in = new ObjectInputStream(new ByteArrayInputStream(bytes));
			object = in.readObject();
			return object;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error: "+e, e);
		} finally {
			if(in!=null) {
				try {
					in.close();
				} catch (IOException ex) {
					logger.log(Level.WARNING, "Error: "+ex, ex);
				}
			}
		}
		return null;
	}

	@Override
	public boolean touch(String key, int ttlSec, long timeoutMiliSec) throws TimeoutException {
		try {
			StringBuilder sb = new StringBuilder();
			//touch <key> <exptime> [noreply]\r\n
			sb.append("touch ").append(key).append(" ").append(ttlSec).append("\r\n");
			
			ClientInfo ci = new ClientInfo();
			ci.setClientKey(key);
			
			String res = sendCmdOut(key, sb.toString());
			if(res==null) {
				throw new TimeoutException("we got a null reply!");
			}
			
			if(res.equals("TOUCHED")) {
				return true;
			} else {
				return false;
			}
		} catch (TimeoutException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new TimeoutException("We had error "+ex);
		}
	}

	@Override
	public Object gat(String key, int ttlSec, long timeoutMiliSec) throws TimeoutException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	

	
}