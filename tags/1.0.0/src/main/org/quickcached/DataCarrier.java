/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.quickcached;

/**
 *
 * @author akshath
 */
public class DataCarrier implements java.io.Serializable {
	private byte data[];
	private String flags;
	private int cas;

	

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
	
}
