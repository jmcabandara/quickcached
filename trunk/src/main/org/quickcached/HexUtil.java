/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.quickcached;

/**
 *
 * @author akshath
 */
public class HexUtil {
	//converts binary data to hex sting
	public static String encode(String sourceText) {
		return encode(sourceText.getBytes());
	}
	public static String encode(byte[] rawData) {
		StringBuilder hexText = new StringBuilder();
		String initialHex = null;
		int initHexLength = 0;

		for (int i = 0; i < rawData.length; i++) {
			int positiveValue = rawData[i] & 0x000000FF;
			initialHex = Integer.toHexString(positiveValue);
			initHexLength = initialHex.length();
			while (initHexLength++ < 2) {
				hexText.append("0");
			}
			hexText.append(initialHex);
		}
		return hexText.toString();
	}

	public static String encode(byte rawData) {
		StringBuilder hexText = new StringBuilder();
		String initialHex = null;
		int initHexLength = 0;

		int positiveValue = rawData & 0x000000FF;

		initialHex = Integer.toHexString(positiveValue);
		initHexLength = initialHex.length();
		while (initHexLength++ < 2) {
			hexText.append("0");
		}
		hexText.append(initialHex);

		return hexText.toString();
	}

	//converts hex sting to binary data
	public static String decodeToString(String hexText) {
		byte[] rawToByte = decodeToByte(hexText);
		return new String(rawToByte);
	}
	public static byte[] decodeToByte(String hexText) {
		String chunk = null;
		if (hexText != null && hexText.length() > 0) {
			int numBytes = hexText.length() / 2;

			byte[] rawToByte = new byte[numBytes];
			int offset = 0;
			for (int i = 0; i < numBytes; i++) {
				chunk = hexText.substring(offset, offset + 2);
				offset += 2;
				rawToByte[i] = (byte) (Integer.parseInt(chunk, 16) & 0x000000FF);
			}
			return rawToByte;
		}
		return null;
	}
}
