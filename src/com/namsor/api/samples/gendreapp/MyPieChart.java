package com.namsor.api.samples.gendreapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Picture;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import com.androidplot.exception.PlotRenderException;
import com.androidplot.pie.PieChart;

public class MyPieChart extends PieChart {

	public enum VisualState { start, running, done, share}
	
	private static final VisualState[][] validTransitions = {
			 { VisualState.start, VisualState.running },
			 { VisualState.running, VisualState.start },
			 { VisualState.running, VisualState.done },
			 { VisualState.running, VisualState.share },
			 { VisualState.done, VisualState.share },
	};
	
	static class StateViz {
		final VisualState state;
		final Integer msg;
		final boolean border; 
		final Integer subMsg;
		StateViz(VisualState state_, Integer msg_, boolean border_, Integer subMsg_) {
			state = state_;
			msg = msg_;
			border = border_;
			subMsg = subMsg_;
		}
	};
	
	private static final StateViz[] stateViz = {
			 new StateViz( VisualState.start, R.string.btn_genderize_start, true, null),
			 new StateViz( VisualState.running,  R.string.btn_genderize_running, false, R.string.btn_stop ),			 
			 new StateViz( VisualState.done, R.string.btn_genderize_done, false, R.string.btn_genderize_likenshare ),			 
			 new StateViz( VisualState.share, R.string.btn_genderize_done, false, R.string.btn_genderize_likenshare ),			 
	};
	
	private StateViz getStateViz() {
		for (int i = 0; i < stateViz.length; i++) {
			if(stateViz[i].state==getVisualState()) {
				return stateViz[i];
			}
		}
		return null;
	}
	
	private synchronized boolean transit(VisualState from, VisualState to) {
		for (int i = 0; i < validTransitions.length; i++) {
			VisualState[] validTransition = validTransitions[i];
			if(validTransition[0]==from && validTransition[1]==to) {
				setVisualState(to);
				return true;
			}
		}
		return false;
	}
	
	public void start() {
		transit(getVisualState(), VisualState.running);
	}

	public void stopAndStart() {
		transit(getVisualState(), VisualState.start);
	}

	public void stopAndShare() {
		transit(getVisualState(), VisualState.share);
	}

	public void done() {
		transit(getVisualState(), VisualState.done);
	}

	public void share() {
		transit(getVisualState(), VisualState.share);
	}
	
	private static final String TAG = "MyPieChart";
	private VisualState visualState = VisualState.start;
	
	public MyPieChart(Context context, String title) {
		super(context, title);
		init();
	}

	public MyPieChart(Context context, AttributeSet attributes) {
		super(context, attributes);
		init();
	}

	public MyPieChart(Context context, String title,
			com.androidplot.Plot.RenderMode mode) {
		super(context, title, mode);
		init();
	}
	
	private void init() {
    	myPaintMsg.setColor(Color.DKGRAY);		
    	myPaintMsg.setAntiAlias(MainActivity.ANTI_ALIASING);
    	
    	Typeface tf_regular =Typeface.createFromAsset(getContext().getAssets(),
                "fonts/Montserrat-Regular.ttf");
    	Typeface tf_bold =Typeface.createFromAsset(getContext().getAssets(),
                "fonts/Montserrat-Bold.ttf");
    	myPaintMsg.setTypeface(tf_regular);

    	
    	myPaintWhiteCircle.setColor(Color.WHITE);
    	myPaintWhiteCircle.setAntiAlias(MainActivity.ANTI_ALIASING);
    	myPaintGreyCircle.setColor(Color.parseColor("#eaeaea"));
    	myPaintGreyCircle.setAntiAlias(MainActivity.ANTI_ALIASING);
    	myPaintBlackCircle.setColor(Color.DKGRAY);
    	myPaintBlackCircle.setAntiAlias(MainActivity.ANTI_ALIASING);
    	
    	myPaintSubMsg.setTypeface(tf_regular);
    	myPaintSubMsg.setColor(Color.parseColor("#eaeaea"));
    	myPaintSubMsg.setAntiAlias(MainActivity.ANTI_ALIASING);
	}

	
	
	Paint myPaintMsg = new Paint();
	Paint myPaintSubMsg = new Paint();
	Paint myPaintWhiteCircle = new Paint();
	Paint myPaintGreyCircle = new Paint();
	Paint myPaintBlackCircle = new Paint();
	
	
    protected synchronized void renderOnCanvas(Canvas canvas) {
    	
        try {
            // any series interested in synchronizing with plot should
            // implement PlotListener.onBeforeDraw(...) and do a read lock from within its
            // invocation.  This is the entry point into that call:
            notifyListenersBeforeDraw(canvas);
            try {
                // need to completely erase what was on the canvas before redrawing, otherwise
                // some odd aliasing artifacts begin to build up around the edges of aa'd entities
                // over time.
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if (getBackgroundPaint()!= null) {
                    drawBackground(canvas, getDisplayDimensions().marginatedRect);
                }
                // additional design stuff
                
                float cx = getPieWidget().getWidgetDimensions().paddedRect.centerX();
                float cy = getPieWidget().getWidgetDimensions().paddedRect.centerY();
                float rad = getRadius(getPieWidget().getWidgetDimensions().paddedRect);
                                
                canvas.drawCircle(cx, cy, rad*1.05f, myPaintWhiteCircle);
                float blackRad = rad*0.1f;
                canvas.drawCircle(cx, getPieWidget().getWidgetDimensions().paddedRect.top-blackRad/3, blackRad, myPaintBlackCircle);

                canvas.drawCircle(cx, cy, rad*.95f, myPaintGreyCircle);
                
                canvas.drawCircle(cx, cy, rad*.75f, myPaintWhiteCircle);
                
                getLayoutManager().draw(canvas);

                if (getBorderPaint() != null) {
                    drawBorder(canvas, getDisplayDimensions().marginatedRect);
                }
                
            } catch (PlotRenderException e) {
                Log.e(TAG, "Exception while rendering Plot.", e);
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "Exception while rendering Plot.", e);
            }
        } finally {
        	setIdle(true);
            // any series interested in synchronizing with plot should
            // implement PlotListener.onAfterDraw(...) and do a read unlock from within that
            // invocation. This is the entry point for that invocation.
            notifyListenersAfterDraw(canvas);
        }
    	    	        
        StateViz stateViz = getStateViz();
        if( stateViz != null ) {
            float rad = getRadius(getPieWidget().getWidgetDimensions().paddedRect);
        	
        	float width = (stateViz.border? rad * 3/4f : rad * 1f);
        	String msg = getResources().getString( stateViz.msg );
        	Rect bounds = setTextSizeForWidth(myPaintMsg, width, msg);
        	
        	if( stateViz.subMsg == null ) {
                canvas.drawText(msg, getDisplayDimensions().marginatedRect.centerX()-bounds.width()/2, getDisplayDimensions().marginatedRect.centerY()+bounds.height()/2, myPaintMsg);    	
                if( stateViz.border ) {
                	float borderRatioW = .75f;
                	float borderRatioH = 1.1f;
                    final Paint paint = new Paint();
                    final RectF rectF = new RectF(getDisplayDimensions().marginatedRect.centerX()-bounds.width()*borderRatioW, getDisplayDimensions().marginatedRect.centerY()-bounds.height()*borderRatioH, getDisplayDimensions().marginatedRect.centerX()+bounds.width()*borderRatioW, getDisplayDimensions().marginatedRect.centerY()+bounds.height()*borderRatioH);
                    final float roundPx = 20;

                    paint.setAntiAlias(true);
                    paint.setColor(Color.TRANSPARENT);
                    paint.setARGB(255, 0, 0,0);
                    paint.setStyle(Style.STROKE);
                    paint.setStrokeWidth(2);
                    paint.setPathEffect(new DashPathEffect(new float[] {10,5}, 0));
                    
                    canvas.drawRoundRect(rectF, roundPx, roundPx, paint);                	
                }
        	} else {
        		String subMsg = getResources().getString( stateViz.subMsg);
                canvas.drawText(msg, getDisplayDimensions().marginatedRect.centerX()-width/2, getDisplayDimensions().marginatedRect.centerY(), myPaintMsg);    	
            	float subWidth = width * 3/4f;
            	Rect subBounds = TextUtils.setTextSizeForWidth(myPaintSubMsg, subWidth, subMsg, myPaintMsg.getTextSize());
                canvas.drawText(subMsg, getDisplayDimensions().marginatedRect.centerX()-subBounds.width()/2, getDisplayDimensions().marginatedRect.centerY()+bounds.height()+subBounds.height()/2, myPaintSubMsg);    	        		
        	}
        }
    }
    
    private Rect setTextSizeForWidth(Paint myPaintMsg2, float width, String msg) {
    	return TextUtils.setTextSizeForWidth(myPaintMsg2, width, msg, -1);
	}

	public float getRadius(RectF rect) {
    	return  rect.width() < rect.height() ? rect.width() / 2 : rect.height() / 2;
    }
    
	public synchronized VisualState getVisualState() {
		return visualState;
	}

	public synchronized void setVisualState(VisualState visualState) {
		this.visualState = visualState;
	}


}
