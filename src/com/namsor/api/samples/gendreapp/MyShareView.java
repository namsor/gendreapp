package com.namsor.api.samples.gendreapp;

import com.androidplot.ui.widget.Widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class MyShareView extends View {

	private static final float ROWS = 5f;

	public MyShareView(Context context) {
		super(context);
		init();
	}

	public MyShareView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public MyShareView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	Paint titlePaint = new Paint();
	Paint subTitlePaint = new Paint();


	private void init() {
		titlePaint.setColor(Color.DKGRAY);
		subTitlePaint.setColor(Color.GRAY);
		titlePaint.setAntiAlias(MainActivity.ANTI_ALIASING);
		subTitlePaint.setAntiAlias(MainActivity.ANTI_ALIASING);

		Typeface tf_regular = Typeface.createFromAsset(
				getContext().getAssets(), "fonts/Montserrat-Regular.ttf");
		Typeface tf_bold = Typeface.createFromAsset(getContext().getAssets(),
				"fonts/Montserrat-Bold.ttf");

		titlePaint.setTypeface(tf_bold);
		subTitlePaint.setTypeface(tf_regular);

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		rowHeightDesired = h / ROWS;
		Rect bounds = TextUtils.setTextSizeForHeight(titlePaint, rowHeightDesired, "F",
				-1f);
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
		int minh = (int) (rowHeightActual * ROWS);
		int h = resolveSizeAndState(minh, heightMeasureSpec, 0);

		setMeasuredDimension(w, h);
	}

	private String title = "Thanks for using GendRe";
	private String subTitle = "Share your results!";

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getSubTitle() {
		return subTitle;
	}

	public void setSubTitle(String subTitle) {
		this.subTitle = subTitle;
	}

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		float r = .15f;
		float y = 0;

		Rect boundsT = TextUtils.setTextSizeForWidth(titlePaint, getWidth()*2/3,
				getTitle(), -1f);
		Rect boundsST = TextUtils.setTextSizeForWidth(subTitlePaint, getWidth()*2/3,
				getSubTitle(), titlePaint.getTextSize());
		if( boundsT.height()+boundsST.height()>getHeight()/2) {
			 boundsT = TextUtils.setTextSizeForHeight(titlePaint, getHeight()*1/5,
					getTitle(), -1f);
			 boundsST = TextUtils.setTextSizeForHeight(subTitlePaint, getHeight()*1/5,
					getSubTitle(), titlePaint.getTextSize());			
		}
			
		y += boundsT.height() * 1.1f;
		canvas.drawText(getTitle(), getWidth() / 2 - boundsT.width() / 2, y,
				titlePaint);
		y += boundsST.height() * 1.1f;
		canvas.drawText(getSubTitle(), getWidth() / 2 - boundsST.width() / 2, y,
				subTitlePaint);
		y += boundsST.height() * 0.1f;
		int imgHeight = (int)(.8f*(getHeight() - y));
		
		Bitmap[] icons = new Bitmap[SHARE_ICONS.length];
		Paint iconPaint = new Paint();
		for (int i = 0; i < icons.length; i++) {
			Bitmap icon = Bitmap
					.createScaledBitmap(BitmapFactory.decodeResource(
							getResources(), SHARE_ICONS[i]),
							imgHeight,
							imgHeight,
							false);
			
			canvas.drawBitmap(icon, getWidth() * (i+1) / (icons.length+1) - icon.getWidth() / 2,
					y + icon.getHeight()*.2f, iconPaint);
			
			RectF rect = new RectF(getWidth() * (i+1) / (icons.length+1) - icon.getWidth() / 2,
					y + icon.getHeight()*.2f, 
					getWidth() * (i+1) / (icons.length+1) - icon.getWidth() / 2+imgHeight,
					y + icon.getHeight()*.2f+imgHeight
					);
			shareButtons[i] = rect;
		}

	}

	RectF[] shareButtons = new RectF[SHARE_ICONS.length]; 
	static final int[] SHARE_ICONS = {
		R.drawable.btn_facebook,
		R.drawable.btn_twitter,
		R.drawable.btn_googleplus
	};
	
	public int touched(MotionEvent event) {
		for (int i = 0; i < shareButtons.length; i++) {
			if( shareButtons[i] != null && shareButtons[i].contains(event.getX(), event.getY())) {
				return i;
			}
		}
		return -1;
	}

}
