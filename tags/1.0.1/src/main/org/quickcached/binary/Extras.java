/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.quickcached.binary;

/**
 *
 * @author akshath
 */
public class Extras {
	private String flags;
	private String expiration;

	public String getFlags() {
		return flags;
	}

	public void setFlags(String flags) {
		this.flags = flags;
	}

	public String getExpiration() {
		return expiration;
	}

	public long getExpirationInSec() {
		if(expiration==null) {
			return 0;
		} else {
			 return Long.parseLong(expiration, 16);
		}
	}

	public void setExpiration(String expiration) {
		this.expiration = expiration;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[Extras {");
		sb.append("Falg:");
		sb.append(getFlags());
		sb.append(", Expiration:");
		sb.append(getExpiration());
		sb.append("}]");
		return sb.toString();
	}

	public String encodedString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getFlags());
		if(getExpiration()!=null) sb.append(getExpiration());
		return sb.toString();
	}
}
