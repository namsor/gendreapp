package com.namsor.api.samples.gendreapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class MyGenderView extends View {
	private static final String FEMALE_TITE = "♀";
	private static final String MALE_TITE = "♂";
	private static final float RULER_STROKE_WIDTH = 3f; 
	private static final float ROWS = 3f;
	private GenderInfo genderInfo;
	
	public GenderInfo getGenderInfo() {
		return genderInfo;
	}

	public void setGenderInfo(GenderInfo genderInfo) {
		this.genderInfo = genderInfo;
	}

	public MyGenderView(Context context) {
		super(context);
		init();
	}

	public MyGenderView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public MyGenderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public static final int maleColor = Color.parseColor("#278dc1");
	public static final int femaleColor = Color.parseColor("#be3a49");
	public static final int greyColor = Color.parseColor("#f7f7f7");

	Paint maleTitlePaint = new Paint();
	Paint femaleTitlePaint = new Paint();

	Paint maleCountPaint = new Paint();
	Paint femaleCountPaint = new Paint();

	Paint maleRulerPaint = new Paint();
	Paint femaleRulerPaint = new Paint();
	Paint neutralRulerPaint = new Paint();
	

	private void init() {
		maleTitlePaint.setColor(Color.DKGRAY);

		Typeface tf_regular = Typeface.createFromAsset(
				getContext().getAssets(), "fonts/Montserrat-Regular.ttf");
		Typeface tf_bold = Typeface.createFromAsset(getContext().getAssets(),
				"fonts/Montserrat-Bold.ttf");

		maleTitlePaint.setTypeface(tf_bold);
		femaleTitlePaint.setTypeface(tf_bold);
		maleTitlePaint.setColor(maleColor);
		femaleTitlePaint.setColor(femaleColor);

		maleCountPaint.setTypeface(tf_regular);
		femaleCountPaint.setTypeface(tf_regular);
		maleCountPaint.setColor(maleColor);
		femaleCountPaint.setColor(femaleColor);

		maleRulerPaint.setColor(maleColor);
		maleRulerPaint.setStrokeWidth(RULER_STROKE_WIDTH);
		femaleRulerPaint.setColor(femaleColor);
		femaleRulerPaint.setStrokeWidth(RULER_STROKE_WIDTH);
		neutralRulerPaint.setColor(greyColor);
		neutralRulerPaint.setStrokeWidth(RULER_STROKE_WIDTH);

		maleTitlePaint.setAntiAlias(MainActivity.ANTI_ALIASING);
		femaleTitlePaint.setAntiAlias(MainActivity.ANTI_ALIASING);
		maleCountPaint.setAntiAlias(MainActivity.ANTI_ALIASING);
		femaleCountPaint.setAntiAlias(MainActivity.ANTI_ALIASING);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		rowHeightDesired = h/ROWS;
		Rect bounds = TextUtils.setTextSizeForHeight(femaleTitlePaint, rowHeightDesired, "F", -1f);
		rowHeightActual = bounds.height();
	}

	float rowHeightDesired;
	float rowHeightActual;

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Try for a width based on our minimum
		int minw = 100;
		int w = resolveSizeAndState(minw, widthMeasureSpec, 1);

		// Whatever the width ends up being, ask for a height that would let the
		// pie
		// get as big as it can
		int minh = (int) (rowHeightActual*ROWS);
		int h = resolveSizeAndState(minh, heightMeasureSpec, 0);

		setMeasuredDimension(w, h);
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		float r = .15f;
		float y = 0;
		
		Rect boundsF = TextUtils.setTextSizeForHeight(femaleTitlePaint, rowHeightActual, FEMALE_TITE, -1f);
		Rect boundsM = TextUtils.setTextSizeForHeight(maleTitlePaint, rowHeightActual, MALE_TITE, -1f);
		maleTitlePaint.setTextSize(femaleTitlePaint.getTextSize());

		float yH = Math.max(boundsF.height(), boundsM.height());
		y+=yH*1.1f;
		canvas.drawText(FEMALE_TITE, getWidth()*r -boundsF.width()/2, y, femaleTitlePaint);		
		canvas.drawText(MALE_TITE, getWidth()*(1-r)-boundsM.width()/2, y, maleTitlePaint);
		y+=yH*.3f;		
		canvas.drawLine(0, y, getWidth()*r*2, y, femaleRulerPaint);
		canvas.drawLine(getWidth()*r*2, y, getWidth()*(1-r*2), y, neutralRulerPaint);
		canvas.drawLine(getWidth()*(1-r*2), y, getWidth(), y, maleRulerPaint);
		y+=yH*1.1f;		
		
		femaleCountPaint.setTextSize(femaleTitlePaint.getTextSize()*4/5);
		maleCountPaint.setTextSize(femaleCountPaint.getTextSize());

		Rect boundsCountF = new Rect();
		femaleCountPaint.getTextBounds(countF, 0, countF.length(), boundsCountF);

		Rect boundsCountM = new Rect();
		maleCountPaint.getTextBounds(getCountM(), 0, getCountM().length(), boundsCountM);
		
		canvas.drawText(countF, getWidth()*r/2, y, femaleCountPaint);		
		canvas.drawText(getCountM(), getWidth()*(1-r/2)-boundsCountM.width(), y, maleCountPaint);
		y+=yH*.3;		
		canvas.drawLine(0, y, getWidth()*r*2, y, femaleRulerPaint);
		canvas.drawLine(getWidth()*r*2, y, getWidth()*(1-r*2), y, neutralRulerPaint);
		canvas.drawLine(getWidth()*(1-r*2), y, getWidth(), y, maleRulerPaint);
	}
	
	public synchronized String getCountM() {
		return countM;
	}

	public synchronized void setCountM(String countM) {
		this.countM = countM;
	}

	private String countM="0";
	private String countF="0";

	public synchronized String getCountF() {
		return countF;
	}

	public synchronized void setCountF(String countF) {
		this.countF = countF;
	}
	String countU="0";

	public synchronized String getCountU() {
		return countU;
	}

	public synchronized void setCountU(String countU) {
		this.countU = countU;
	}

}
