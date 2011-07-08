package org.quickcached;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.quickcached.binary.BinaryPacket;
import org.quickcached.binary.Extras;
import org.quickcached.binary.ResponseHeader;
import org.quickcached.cache.CacheInterface;
import org.quickserver.net.server.ClientHandler;

/**
 *
 * @author akshath
 */
public class BinaryCommandProcessor {
	private static final Logger logger = Logger.getLogger(BinaryCommandProcessor.class.getName());


	private CacheInterface cache;
	public void setCache(CacheInterface cache) {
		this.cache = cache;
	}

	public void handleBinaryCommand(ClientHandler handler, BinaryPacket command)
			throws SocketTimeoutException, IOException {
		if(QuickCached.DEBUG) logger.fine("command: "+command);

		String opcode = command.getHeader().getOpcode();

		ResponseHeader rh = new ResponseHeader();
		rh.setMagic("81");
		rh.setOpcode(opcode);
		//rh.setKeyLength(0);
		//rh.setExtrasLength(0);
		//rh.setDataType("00");
		//rh.setTotalBodyLength(0);
		rh.setOpaque(command.getHeader().getOpaque());

		BinaryPacket binaryPacket = new BinaryPacket();
		binaryPacket.setHeader(rh);

		opcode = opcode.toUpperCase();

		try {
			if("01".equals(opcode) || "11".equals(opcode)) {//Set,SetQ
				if(command.getHeader().getCas()!=null && 
						"0000000000000000".equals(command.getHeader().getCas())==false) {
					DataCarrier olddc = (DataCarrier) cache.get(command.getKey());
					if(olddc==null) {
						if("01".equals(opcode)) { //set
							rh.setStatus(ResponseHeader.KEY_NOT_FOUND);
							sendResponse(handler, binaryPacket);
						}
						return;
					}

					if(olddc!=null && command.getHeader().getCas()!=null &&
							olddc.checkCas(command.getHeader().getCas())==false) {
						if("01".equals(opcode)) { //set
							if(QuickCached.DEBUG) logger.fine(
									"Cas did not match! OldCas:"+olddc.getCas()+
									"NewCAS:"+command.getHeader().getCas());
							rh.setStatus(ResponseHeader.ITEM_NOT_STORED);
							sendResponse(handler, binaryPacket);
						}
						return;
					}
				}
				DataCarrier dc = new DataCarrier(command.getValue());
				
				dc.setFlags(command.getExtras().getFlags());
				cache.set(command.getKey(), dc, dc.getSize(), command.getExtras().getExpirationInSec());
				
				if("11".equals(opcode)==false) {
					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
					rh.setCas(dc.getCas());

					sendResponse(handler, binaryPacket);
				}
			} else if("02".equals(opcode) || "12".equals(opcode)) {//Add, AddQ
				DataCarrier olddc = (DataCarrier) cache.get(command.getKey());
				if(olddc!=null) {
					if("02".equals(opcode)) { //Add
						rh.setStatus(ResponseHeader.KEY_EXISTS);
						sendResponse(handler, binaryPacket);
					}
					return;
				}

				DataCarrier dc = new DataCarrier(command.getValue());

				if(command.getExtras().getFlags()!=null) dc.setFlags(command.getExtras().getFlags());
				cache.set(command.getKey(), dc, dc.getSize(), command.getExtras().getExpirationInSec());
				
				if("12".equals(opcode)==false) {//AddQ
					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
					sendResponse(handler, binaryPacket);
				}
			} else if("03".equals(opcode) || "13".equals(opcode)) {//Replace, ReplaceQ
				DataCarrier olddc = (DataCarrier) cache.get(command.getKey());
				if(olddc==null) {
					if("03".equals(opcode)) { //Replace
						rh.setStatus(ResponseHeader.KEY_NOT_FOUND);
						sendResponse(handler, binaryPacket);
					}
					return;
				}

				if(olddc!=null && command.getHeader().getCas()!=null &&
						olddc.checkCas(command.getHeader().getCas())==false) {
					if("03".equals(opcode)) { //Replace
						if(QuickCached.DEBUG) logger.fine(
								"Cas did not match! OldCas:"+olddc.getCas()+
								"NewCAS:"+command.getHeader().getCas());
						rh.setStatus(ResponseHeader.ITEM_NOT_STORED);
						sendResponse(handler, binaryPacket);
					}
					return;
				}

				if(command.getExtras().getFlags()!=null) olddc.setFlags(command.getExtras().getFlags());
				olddc.setData(command.getValue());
				
				if("13".equals(opcode)==false) {//AddQ
					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
					rh.setCas(olddc.getCas());

					sendResponse(handler, binaryPacket);
				}
			} else if("0E".equals(opcode) || "19".equals(opcode)) {//Append, AppendQ
				DataCarrier olddc = (DataCarrier) cache.get(command.getKey());
				if(olddc==null) {
					if("0E".equals(opcode)) { //Append
						rh.setStatus(ResponseHeader.ITEM_NOT_STORED);
						sendResponse(handler, binaryPacket);
					}
					return;
				}

				if(olddc!=null && command.getHeader().getCas()!=null &&
						olddc.checkCas(command.getHeader().getCas())==false) {
					if("0E".equals(opcode)) { //Append
						if(QuickCached.DEBUG) logger.fine(
								"Cas did not match! OldCas:"+olddc.getCas()+
								"NewCAS:"+command.getHeader().getCas());
						rh.setStatus(ResponseHeader.ITEM_NOT_STORED);
						sendResponse(handler, binaryPacket);
					}
					return;
				}

				olddc.append(command.getValue());
				
				cache.update(command.getKey(), olddc, olddc.getSize());

				if("19".equals(opcode)==false) {//AppendQ
					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
					rh.setCas(olddc.getCas());

					sendResponse(handler, binaryPacket);
				}
			} else if("0F".equals(opcode) || "1A".equals(opcode)) {//Prepend, PrependQ
				DataCarrier olddc = (DataCarrier) cache.get(command.getKey());
				if(olddc==null) {
					if("0F".equals(opcode)) { //Prepend
						rh.setStatus(ResponseHeader.ITEM_NOT_STORED);
						sendResponse(handler, binaryPacket);
					}
					return;
				}

				if(olddc!=null && command.getHeader().getCas()!=null &&
						olddc.checkCas(command.getHeader().getCas())==false) {
					if("0F".equals(opcode)) { //Prepend
						if(QuickCached.DEBUG) logger.fine(
								"Cas did not match! OldCas:"+olddc.getCas()+
								"NewCAS:"+command.getHeader().getCas());
						rh.setStatus(ResponseHeader.ITEM_NOT_STORED);
						sendResponse(handler, binaryPacket);
					}
					return;
				}

				olddc.prepend(command.getValue());
				
				cache.update(command.getKey(), olddc, olddc.getSize());

				if("1A".equals(opcode)==false) {//PrependQ
					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
					rh.setCas(olddc.getCas());

					sendResponse(handler, binaryPacket);
				}
			} else if ("04".equals(opcode) || "14".equals(opcode)) {//Delete, DeleteQ
				boolean falg = cache.delete(command.getKey());
				if(falg==false) {
					rh.setStatus(ResponseHeader.KEY_NOT_FOUND);
					sendResponse(handler, binaryPacket);
				} else {
					if("14".equals(opcode)==false) {
						rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
						sendResponse(handler, binaryPacket);
					}
				}
			} else if ("05".equals(opcode) || "06".equals(opcode) ||//Increment, Decrement
					"15".equals(opcode) || "16".equals(opcode)) {//IncrementQ, DecrementQ
				Extras extras = command.getExtras();

				DataCarrier olddc = (DataCarrier) cache.get(command.getKey());
				if(olddc==null) {
					if(extras.getExpiration().equals("ffffffff")==false) {
						if("05".equals(opcode) || "06".equals(opcode)) { //Increment, Decrement
							rh.setStatus(ResponseHeader.KEY_NOT_FOUND);
							sendResponse(handler, binaryPacket);
						}
						return;
					} else {
						String value = ""+extras.getInitalValueInDec();
						olddc = new DataCarrier(value.getBytes("utf-8"));

						if(extras.getFlags()!=null) olddc.setFlags(extras.getFlags());
						cache.set(command.getKey(), olddc, olddc.getSize(), extras.getExpirationInSec());
						
						if("05".equals(opcode) || "06".equals(opcode)) {
							binaryPacket.setValue(olddc.getData());
							rh.setTotalBodyLength(rh.getKeyLength()+
									rh.getExtrasLength()+binaryPacket.getValue().length);
							rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
							rh.setCas(olddc.getCas());
	
							sendResponse(handler, binaryPacket);
						}
						return;
					}
				}

				
				long value = 0;
				try {
					value = extras.getDeltaInDec();
				} catch (Exception e) {
					if(QuickCached.DEBUG) logger.warning("Error: "+e);
					rh.setStatus(ResponseHeader.INVALID_ARGUMENTS);
					sendResponse(handler, binaryPacket);
					return;
				}

				synchronized (command.getKey()) {
					try {
						long oldvalue = Long.parseLong(new String(olddc.getData()));
						if ("05".equals(opcode) || "15".equals(opcode)) {
							value = oldvalue + value;
						} else if ("06".equals(opcode) || "16".equals(opcode)) {
							value = oldvalue - value;
							if (value < 0) {
								value = 0;
							}
						}
						olddc.setData(("" + value).getBytes("utf-8"));
					} catch (Exception e) {
						if ("05".equals(opcode) || "06".equals(opcode)) {
							rh.setStatus(ResponseHeader.INTERNAL_ERROR);
							sendResponse(handler, binaryPacket);
							return;
						}
						return;
					}
				}
				
				cache.update(command.getKey(), olddc, olddc.getSize());
				
				if ("15".equals(opcode) || "16".equals(opcode)) {
					return;
				}

				
				binaryPacket.setValue(Util.prefixZerros(
						new String(olddc.getData(), "utf-8"), 8).getBytes("utf-8"));

				rh.setTotalBodyLength(rh.getKeyLength()+
						rh.getExtrasLength()+binaryPacket.getValue().length);
				rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
				rh.setCas(olddc.getCas());

				sendResponse(handler, binaryPacket);

			} else if ("00".equals(opcode) || "09".equals(opcode) ||
					"0C".equals(opcode) || "0D".equals(opcode)) {//Get,GetQ, GetK, GetKQ
				
				if("0C".equals(opcode) || "0D".equals(opcode)) {//GetK, GetKQ
					binaryPacket.setKey(command.getKey());
					rh.setKeyLength(binaryPacket.getKey().length());
				}

				DataCarrier dc = (DataCarrier) cache.get(command.getKey());
				if(dc==null) {
					if("09".equals(opcode)==false && "0D".equals(opcode)==false) { //GetQ, GetKQ
						rh.setStatus(ResponseHeader.KEY_NOT_FOUND);
						sendResponse(handler, binaryPacket);
					}
				} else {
					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);

					Extras extras = new Extras();
					extras.setFlags(dc.getFlags());
					binaryPacket.setExtras(extras);
					rh.setExtrasLength(4);

					rh.setCas(dc.getCas());
					binaryPacket.setValue(dc.getData());

					rh.setTotalBodyLength(rh.getKeyLength()+
							rh.getExtrasLength()+binaryPacket.getValue().length);

					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
					rh.setCas(dc.getCas());

					sendResponse(handler, binaryPacket);					
				}				
			} else if("07".equals(opcode) || "17".equals(opcode)) {//Quit,QuitQ
				if("17".equals(opcode)==false) {
					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
					sendResponse(handler, binaryPacket);
				}
				handler.closeConnection();
			} else if("08".equals(opcode) || "18".equals(opcode)) {//Flush, FlushQ
				cache.flush();
				if("18".equals(opcode)==false) {
					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
					sendResponse(handler, binaryPacket);
				}
			} else if("0A".equals(opcode)) {//noop
				rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
				sendResponse(handler, binaryPacket);
			} else if("0B".equals(opcode)) {//version
				rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
				binaryPacket.setValue(QuickCached.version.getBytes("utf-8"));
				rh.setTotalBodyLength(rh.getKeyLength()+
					rh.getExtrasLength()+binaryPacket.getValue().length);
				sendResponse(handler, binaryPacket);
			} else if("10".equals(opcode)) {//Stat
				rh.setStatus(ResponseHeader.STATUS_NO_ERROR);

				Map stats = CommandHandler.getStats(handler.getServer());
				Set keySet = stats.keySet();
				Iterator iterator = keySet.iterator();
				String key = null;
				String value = null;
				while(iterator.hasNext()) {
					key = (String) iterator.next();
					value = (String) stats.get(key);
					
					binaryPacket.setKey(key);
					rh.setKeyLength(binaryPacket.getKey().length());

					binaryPacket.setValue(value.getBytes("utf-8"));

					rh.setTotalBodyLength(rh.getKeyLength()+
						rh.getExtrasLength()+binaryPacket.getValue().length);
					
					sendResponse(handler, binaryPacket);
				}

				binaryPacket.setKey(null);
				rh.setKeyLength(0);

				binaryPacket.setValue(null);
				rh.setTotalBodyLength(0);
				sendResponse(handler, binaryPacket);
			} else {
				logger.warning("unknown binary command! "+opcode);
				rh.setStatus(ResponseHeader.UNKNOWN_COMMAND);
				sendResponse(handler, binaryPacket);
			}			
		} catch(Exception e) {
			logger.warning("Error: "+e);
			if(QuickCached.DEBUG) e.printStackTrace();
			rh.setStatus(ResponseHeader.INTERNAL_ERROR);
			sendResponse(handler, binaryPacket);
		}		
	}

	public void sendResponse(ClientHandler handler, BinaryPacket binaryPacket)
			throws SocketTimeoutException, IOException {
		if(QuickCached.DEBUG) logger.fine("Res BinaryPacket: "+binaryPacket);
		byte data[] = binaryPacket.toBinaryByte();
		if(QuickCached.DEBUG) logger.fine("S: "+new String(data));
		handler.sendClientBinary(data);
	}
}
