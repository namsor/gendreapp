package com.namsor.api.samples.gendreapp;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String BUNDLE_NAME_MESSAGES = "com.namsor.api.samples.gendreapp.messages"; //$NON-NLS-1$
	private static final String BUNDLE_NAME_ABOUT = "com.namsor.api.samples.gendreapp.about"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE_MESSAGES = ResourceBundle
			.getBundle(BUNDLE_NAME_MESSAGES);

	private static final ResourceBundle RESOURCE_BUNDLE_ABOUT = ResourceBundle
			.getBundle(BUNDLE_NAME_ABOUT);

	private Messages() {
	}

	public static String getMessageString(String key) {
		try {
			return RESOURCE_BUNDLE_MESSAGES.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	public static String getAboutString(String key) {
		try {
			return RESOURCE_BUNDLE_ABOUT.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
}
