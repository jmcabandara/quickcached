/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.quickcached;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.SocketTimeoutException;
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
				DataCarrier dc = new DataCarrier(command.getValue());
				dc.setFlags(command.getExtras().getFlags());
				cache.set(command.getKey(), dc, command.getExtras().getExpirationInSec());
				
				if("11".equals(opcode)==false) {
					rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
					rh.setCas(dc.getCas());

					sendResponse(handler, binaryPacket);
				}
			} else if("04".equals(opcode) || "14".equals(opcode)) {//Delete, DeleteQ				
				Object obj = cache.delete(command.getKey());
				if(obj==null) {
					rh.setStatus(ResponseHeader.KEY_NOT_FOUND);
					sendResponse(handler, binaryPacket);
				} else {
					if("14".equals(opcode)==false) {
						rh.setStatus(ResponseHeader.STATUS_NO_ERROR);
						sendResponse(handler, binaryPacket);
					}
				}
			} else if("00".equals(opcode) || "09".equals(opcode) ||
					"0C".equals(opcode) || "0D".equals(opcode)) {//Get,GetQ, GetK, GetKQ
				
				if("0C".equals(opcode) || "0D".equals(opcode)) {//GetK, GetKQ
					binaryPacket.setKey(command.getKey());
					rh.setKeyLength(binaryPacket.getEncodedKey().length());
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
				sendResponse(handler, binaryPacket);
			} else if("10".equals(opcode)) {//Stat
				rh.setStatus(ResponseHeader.STATUS_NO_ERROR);

				String pid = ManagementFactory.getRuntimeMXBean().getName();
				int i = pid.indexOf("@");
				pid = pid.substring(0, i);

				binaryPacket.setKey("pid");
				binaryPacket.setValue(pid.getBytes("utf-8"));
				sendResponse(handler, binaryPacket);

				binaryPacket.setKey(null);
				binaryPacket.setValue(null);
				sendResponse(handler, binaryPacket);
			} else {
				rh.setStatus(ResponseHeader.UNKNOWN_COMMAND);
				sendResponse(handler, binaryPacket);
			}			
		} catch(Exception e) {
			logger.warning("Error: "+e);
			e.printStackTrace();
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
