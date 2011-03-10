package org.quickcached;

import java.util.logging.Logger;

/**
 *
 * @author akshath
 */
public class DataCarrier implements java.io.Serializable {
	private static final Logger logger = Logger.getLogger(DataCarrier.class.getName());

	private byte data[];
	private String flags;
	private int cas;

	
	public void append(byte chunk[]) {
		int newlen = data.length + chunk.length;
		byte data_new[] = new byte[newlen];

		int i=0;
		for(int k=0;k<data.length;k++) {
			data_new[i++] = data[k];
		}
		for(int k=0;k<chunk.length;k++) {
			data_new[i++] = chunk[k];
		}

		data = data_new;
		data_new = null;

		setCas(getCas() + 1);
	}

	public void prepend(byte chunk[]) {
		int newlen = data.length + chunk.length;
		byte data_new[] = new byte[newlen];

		int i=0;
		for(int k=0;k<chunk.length;k++) {
			data_new[i++] = chunk[k];
		}
		for(int k=0;k<data.length;k++) {
			data_new[i++] = data[k];
		}

		data = data_new;
		data_new = null;

		setCas(getCas() + 1);
	}

	public String getFlags() {
		return flags;
	}

	public void setFlags(String flags) {
		this.flags = flags;
	}

	public DataCarrier(byte data[]) {
		setData(data);
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
		setCas(getCas() + 1);
	}

	public int getCas() {
		return cas;
	}

	public void setCas(int cas) {
		this.cas = cas;
	}

	public boolean checkCas(String newcas) {
		if(newcas==null || "0000000000000000".equals(newcas)) return true;
		
		StringBuilder sb = new StringBuilder();
		sb.append(cas);
		//0000 0000 0000 0001
		while(sb.length()<16) {
			sb.insert(0, "0");
		}

		return sb.toString().equals(newcas);
	}
}
