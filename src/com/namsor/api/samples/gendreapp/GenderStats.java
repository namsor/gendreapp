package com.namsor.api.samples.gendreapp;

public class GenderStats {
	private static int maleCount;
	private static int femaleCount;
	private static int unknownCount;
	public static synchronized int getFemaleCount() {
		return femaleCount;
	}
	public static synchronized void setFemaleCount(int femaleCount) {
		GenderStats.femaleCount = femaleCount;
	}
	public static synchronized int getMaleCount() {
		return maleCount;
	}
	public static synchronized void setMaleCount(int maleCount) {
		GenderStats.maleCount = maleCount;
	}
	public static synchronized int getUnknownCount() {
		return unknownCount;
	}
	public static synchronized void setUnknownCount(int unknownCount) {
		GenderStats.unknownCount = unknownCount;
	}
	public static synchronized void clear() {
		maleCount = 0;
		femaleCount = 0;
		unknownCount = 0;
	}
	public static synchronized int getCount() {
		return maleCount+femaleCount;
	}

}
