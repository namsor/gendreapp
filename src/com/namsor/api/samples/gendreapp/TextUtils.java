package com.namsor.api.samples.gendreapp;

import android.graphics.Paint;
import android.graphics.Rect;

public class TextUtils {

	public static Rect setTextSizeForWidth(Paint paint, float desiredWidth, String text, float maxTextSize) {

	    // Pick a reasonably large value for the test. Larger values produce
	    // more accurate results, but may cause problems with hardware
	    // acceleration. But there are workarounds for that, too; refer to
	    // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
	    final float testTextSize = 48f;

	    // Get the bounds of the text, using our testTextSize.
	    paint.setTextSize(testTextSize);
	    Rect bounds = new Rect();
	    paint.getTextBounds(text, 0, text.length(), bounds);

	    // Calculate the desired size as a proportion of our testTextSize.
	    float desiredTextSize = testTextSize * desiredWidth / bounds.width();
	    if(maxTextSize>0 && desiredTextSize>maxTextSize) {
	    	desiredTextSize = maxTextSize;
	    }

	    // Set the paint for that size.
	    paint.setTextSize(desiredTextSize);
	    
	    paint.getTextBounds(text, 0, text.length(), bounds);
	    return bounds;
	}	
	

	public static Rect setTextSizeForHeight(Paint paint, float desiredHeight,
			String text, float maxTextSize) {

		// Pick a reasonably large value for the test. Larger values produce
		// more accurate results, but may cause problems with hardware
		// acceleration. But there are workarounds for that, too; refer to
		// http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
		final float testTextSize = 48f;

		// Get the bounds of the text, using our testTextSize.
		paint.setTextSize(testTextSize);
		Rect bounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);

		// Calculate the desired size as a proportion of our testTextSize.
		float desiredTextSize = testTextSize * desiredHeight / bounds.height();
		if (maxTextSize > 0 && desiredTextSize > maxTextSize) {
			desiredTextSize = maxTextSize;
		}

		// Set the paint for that size.
		paint.setTextSize(desiredTextSize);

		paint.getTextBounds(text, 0, text.length(), bounds);
		return bounds;
	}


}
