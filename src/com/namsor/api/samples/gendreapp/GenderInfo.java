package com.namsor.api.samples.gendreapp;

public class GenderInfo {
	public static GenderInfo[] TEST_GenderInfo = {
		new GenderInfo("John Smith", -1),
		new GenderInfo("John Doe", -1),
		new GenderInfo("Mary Gender", +1),
		new GenderInfo("Mary Smiths", +1),		
		new GenderInfo("Il �tait une fois un petit chaperon rouge.", +1),		
		new GenderInfo("Alien Caressant", +1),		
	};
	
	final String fullName;

	public String getFullName() {
		return fullName;
	}


	public int getGender() {
		return gender;
	}

	final int gender;

	GenderInfo(String fullName, int gender) {
		this.fullName = fullName;
		this.gender = gender;
		
	}

}
