package com.sap.charging.util;

import java.text.DecimalFormat;

public class Util {
	
	/**
	 * Uses Thread.sleep to wait a while
	 */
	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {}
	}
	
	private static DecimalFormat doubleFormat = new DecimalFormat("#.##");
	public static String formatDouble(double d) {
		return doubleFormat.format(d).replaceAll(",", ".");
	}
	
	public static String formatDoubleArray(double[] array) {
		String result = "[";
		for (int i=0;i<array.length;i++) {
			result += formatDouble(array[i]);
			if (i!=array.length-1)
				result += ", ";
		}
		result += "]";
		return result;
	}
	
	public static synchronized String generateGUID() {
		return java.util.UUID.randomUUID().toString();
	}
	
	
	
}
