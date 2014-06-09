package com.namsor.api.samples.gendreapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import com.namsor.api.samples.gendreapp.MainActivity.ResponseReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class LiquidPhysicsWallpaper extends WallpaperService {
	
	// radius of particles
	private static final float RADIUS = 3;
	// polarity effect
	private static final float POLARITY_EFFECT = 0.05F;
	// sensor effect
	private static final float SENSOR_EFFECT_X = 0.05F;
	private static final float SENSOR_EFFECT_Y = 0.10F;

	private SensorManager mSensorManager;
	private Sensor mSensor;
	private WindowManager mWindowManager;
	private Display mDisplay;

	private final Handler mHandler = new Handler();

	
	@Override
	public Engine onCreateEngine() {
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		mDisplay = mWindowManager.getDefaultDisplay();

		mSensorManager.registerListener(new MySensorEventListener(), mSensor,
				SensorManager.SENSOR_DELAY_UI);
		
		WallpaperEngine engine = new WallpaperEngine();

		return engine;
	}

	private synchronized float getSensorX() {
		return sensorX;
	}

	private synchronized void setSensorX(float sensorX) {
		this.sensorX = sensorX;
	}

	private synchronized float getSensorY() {
		return sensorY;
	}

	private synchronized void setSensorY(float sensorY) {
		this.sensorY = sensorY;
	}

	private synchronized float getSensorZ() {
		return sensorZ;
	}

	private synchronized void setSensorZ(float sensorZ) {
		this.sensorZ = sensorZ;
	}

	private synchronized long getSensorTimeStamp() {
		return sensorTimeStamp;
	}

	private synchronized void setSensorTimeStamp(long sensorTimeStamp) {
		this.sensorTimeStamp = sensorTimeStamp;
	}

	private float sensorX;
	private float sensorY;
	private float sensorZ;
	private long sensorTimeStamp;

	public class MySensorEventListener implements SensorEventListener {

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
				return;

			switch (mDisplay.getRotation()) {
			case Surface.ROTATION_0:
				setSensorX(event.values[0]);
				setSensorY(event.values[1]);
				break;
			case Surface.ROTATION_90:
				setSensorX(-event.values[1]);
				setSensorY(event.values[0]);
				break;
			case Surface.ROTATION_180:
				setSensorX(-event.values[0]);
				setSensorY(-event.values[1]);
				break;
			case Surface.ROTATION_270:
				setSensorX(event.values[1]);
				setSensorY(-event.values[0]);
				break;
			}
			setSensorZ(event.values[2]);
			setSensorTimeStamp(event.timestamp);

			// dump sensor info
			// Logger.getLogger(getClass().getName()).info(
			// "sensor :" + getSensorX() + "," + getSensorY() + "," +
			// getSensorZ());
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub

		}

	}

	class WallpaperEngine extends Engine implements
			SharedPreferences.OnSharedPreferenceChangeListener {

		private final int UPDATE_PARTICLES_FREQ = 100;
		private int framesBeforeParticlesUpdate = UPDATE_PARTICLES_FREQ;
		
		private boolean mVisible;
		private Paint linePaintPlus;
		private Paint linePaintMinus;

		List<Particle> particles = new LinkedList();

		public List<Particle> getParticles() {
			return particles;
		}

		int gsizeX = 60;
		int gsizeY = 90;

		Node[][] grid = new Node[this.gsizeX][this.gsizeY];
		ArrayList<Node> active = new ArrayList<Node>();
		Thread animationThread;
		Material water = new Material(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
		boolean pressed;
		boolean pressedprev;
		int mx;
		int my;
		float mTouchX;
		float mTouchY;
		int mxprev;
		int myprev;

		private long currentTime;

		// FPS display
		private int mFrameCounter;
		private String mFrameRate = new String();
		private Paint mFpsPaint = new Paint();
		private final Runnable mTimerRunnable = new Runnable() {

			// Updates frame rate data from 2 second long sample.
			public void run() {

				mFrameRate = new String("GendRE"+" ♀:"+GenderStats.getFemaleCount()+" ♂:"+GenderStats.getMaleCount());

				mFrameCounter = 0;

				mHandler.removeCallbacks(mTimerRunnable);
				mHandler.postDelayed(mTimerRunnable, 2000);

			}
		};

		private final Runnable mRunnable = new Runnable() {
			public void run() {
				drawFrame();
			}
		};

		void drawFrame() {
			currentTime = System.currentTimeMillis();
			final SurfaceHolder holder = getSurfaceHolder();
			mx = Math.round(mTouchX / 2f);
			my = Math.round(mTouchY / 2f);
			simulate();
			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					c.drawARGB(255, 0, 0, 0);
				}

				for (Particle p : getParticles()) {
					Paint paint = (p.polarityPlus ? linePaintPlus
							: linePaintMinus);
					c.drawCircle((float) (8.0F * p.x), (float) (8.0F * p.y),
							RADIUS, paint);
					if (!p.polarityPlus) {
						c.drawLine((float) (8.0F * p.x), (float) (8.0F * p.y),
								(float) (8.0F * (p.x - p.u)),
								(float) (8.0F * (p.y - p.v)), paint);
					}
				}

				mFrameCounter++;
				c.drawText(mFrameRate, 50.0f, 60.0f, mFpsPaint);

			} finally {
				if (c != null)
					holder.unlockCanvasAndPost(c);
			}

			// Reschedule the next redraw
			mHandler.removeCallbacks(mRunnable);
			if (mVisible) {

				// TODO changed post delay for mRunnable here
				mHandler.postDelayed(mRunnable,
						17 - (System.currentTimeMillis() - currentTime));
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			drawFrame();
		}

		@Override
		public void onTouchEvent(MotionEvent event) {

			mTouchX = event.getX();
			mTouchY = event.getY();

			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				this.pressed = true;
			}
			if (event.getAction() == MotionEvent.ACTION_UP) {
				this.pressed = false;
			}
			super.onTouchEvent(event);
		}

		private int oldCount = 0;

		public synchronized void createParticles(int newCountPositive,
				int newCountNegative) {
			if ((newCountPositive + newCountNegative) == oldCount) {
				return;
			} else {
				oldCount = (newCountPositive + newCountNegative);
			}
			if( newCountPositive == 0 && newCountNegative == 0) {
				// avoid a div/0 crash
				newCountPositive=1;
				newCountNegative=1;
			}
			int cP = 0;
			int cN = 0;
			for (Particle p : getParticles()) {
				if (p.polarityPlus) {
					cP++;
				} else {
					cN++;
				}
			}
			float r = (newCountPositive * 1.0F / (newCountPositive + newCountNegative));
			int nP = (int) Math.round(10F * r
					+ Math.log(newCountPositive + newCountNegative) * 50 * r);
			int nN = (int) Math.round(10F * (1 - r)
					+ Math.log(newCountPositive + newCountNegative) * 50
					* (1 - r));

			for (int j = 0; (cN < nN || cP < nP) && j < 50; j++) {
				for (int i = 0; cN < nN && i < 10; i++) {
					Particle p = new Particle(this.water, i + 4, j + 4, 0.0F,
							0.0F, false);
					getParticles().add(p);
					cN++;
				}
				for (int i = 10; cP < nP && i < 20; i++) {
					Particle p = new Particle(this.water, i + 4, j + 4, 0.0F,
							0.0F, true);
					getParticles().add(p);
					cP++;
				}
			}
		}

		private void updateParticles() {
			createParticles(GenderStats.getFemaleCount(), GenderStats.getMaleCount());			
		}
		
		WallpaperEngine() {
			for (int i = 0; i < this.gsizeX; i++) {
				for (int j = 0; j < this.gsizeY; j++) {
					this.grid[i][j] = new Node();
				}
			}
			
			//createParticles(GenderStats.getFemaleCount(), GenderStats.getMaleCount());
			updateParticles();
			
			/*
			 * for (int i = 0; i < 20; i++) { for (int j = 0; j < 20; j++) {
			 * Particle p = new Particle(this.water, i + 4, j + 4, 0.0F, 0.0F, i
			 * > 10); getParticles().add(p); } }
			 */
			linePaintPlus = new Paint();
			linePaintPlus.setColor(Color.parseColor("#be3a49"));
			// linePaintPlus.setAntiAlias(false);
			// linePaintPlus.setStrokeCap(Cap.BUTT);
			// linePaintPlus.setStrokeWidth(0);

			linePaintMinus = new Paint();
			linePaintMinus.setColor(Color.parseColor("#278dc1"));
			// linePaintMinus.setAntiAlias(false);
			// linePaintMinus.setStrokeCap(Cap.BUTT);
			// linePaintMinus.setStrokeWidth(0);

			mFpsPaint.setColor(Color.WHITE);
			mFpsPaint.setAntiAlias(true);
			mFpsPaint.setTextSize(18.0f);
			mHandler.post(mTimerRunnable);
			
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);

			// By default we don't get touch events, so enable them.
			setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			mHandler.removeCallbacks(mRunnable);
			mHandler.removeCallbacks(mTimerRunnable);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible) {
				drawFrame();
				mHandler.post(mTimerRunnable);
			} else {
				mHandler.removeCallbacks(mRunnable);
				mHandler.removeCallbacks(mTimerRunnable);
			}
		}

		public void simulate() {
			
			framesBeforeParticlesUpdate--;
			if( framesBeforeParticlesUpdate < 0 ) {
				framesBeforeParticlesUpdate = UPDATE_PARTICLES_FREQ;
				// update particles
				updateParticles();
			}
			
			boolean drag = false;
			float mdx = 0.0F;
			float mdy = 0.0F;
			if ((this.pressed) && (this.pressedprev)) {
				drag = true;
				mdx = 0.25F * (this.mx - this.mxprev);
				mdy = 0.25F * (this.my - this.myprev);
			}

			this.pressedprev = this.pressed;
			this.mxprev = this.mx;
			this.myprev = this.my;

			for (Node n : this.active) {
				n.m = (n.d = n.gx = n.gy = n.u = n.v = n.ax = n.ay = 0.0F);
				n.active = false;
			}
			this.active.clear();

			for (Particle p : getParticles()) {
				p.cx = (int) (p.x - 0.5F);
				p.cy = (int) (p.y - 0.5F);

				float x = p.cx - p.x;
				p.px[0] = (0.5F * x * x + 1.5F * x + 1.125F);
				p.gx[0] = (x + 1.5F);
				x += 1.0F;
				p.px[1] = (-x * x + 0.75F);
				p.gx[1] = (-2.0F * x);
				x += 1.0F;
				p.px[2] = (0.5F * x * x - 1.5F * x + 1.125F);
				p.gx[2] = (x - 1.5F);

				float y = p.cy - p.y;
				p.py[0] = (0.5F * y * y + 1.5F * y + 1.125F);
				p.gy[0] = (y + 1.5F);
				y += 1.0F;
				p.py[1] = (-y * y + 0.75F);
				p.gy[1] = (-2.0F * y);
				y += 1.0F;
				p.py[2] = (0.5F * y * y - 1.5F * y + 1.125F);
				p.gy[2] = (y - 1.5F);

				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						int cxi = p.cx + i;
						int cyj = p.cy + j;
						Node n = this.grid[cxi][cyj];
						if (!n.active) {
							this.active.add(n);
							n.active = true;
						}
						float phi = p.px[i] * p.py[j];
						n.m += phi * p.mat.m;
						n.d += phi;
						float dx = p.gx[i] * p.py[j];
						float dy = p.px[i] * p.gy[j];
						n.gx += dx;
						n.gy += dy;
					}
				}
			}

			for (Particle p : getParticles()) {
				int cx = (int) p.x;
				int cy = (int) p.y;
				int cxi = cx + 1;
				int cyi = cy + 1;

				float p00 = this.grid[cx][cy].d;
				float x00 = this.grid[cx][cy].gx;
				float y00 = this.grid[cx][cy].gy;
				float p01 = this.grid[cx][cyi].d;
				float x01 = this.grid[cx][cyi].gx;
				float y01 = this.grid[cx][cyi].gy;
				float p10 = this.grid[cxi][cy].d;
				float x10 = this.grid[cxi][cy].gx;
				float y10 = this.grid[cxi][cy].gy;
				float p11 = this.grid[cxi][cyi].d;
				float x11 = this.grid[cxi][cyi].gx;
				float y11 = this.grid[cxi][cyi].gy;

				float pdx = p10 - p00;
				float pdy = p01 - p00;
				float C20 = 3.0F * pdx - x10 - 2.0F * x00;
				float C02 = 3.0F * pdy - y01 - 2.0F * y00;
				float C30 = -2.0F * pdx + x10 + x00;
				float C03 = -2.0F * pdy + y01 + y00;
				float csum1 = p00 + y00 + C02 + C03;
				float csum2 = p00 + x00 + C20 + C30;
				float C21 = 3.0F * p11 - 2.0F * x01 - x11 - 3.0F * csum1 - C20;
				float C31 = -2.0F * p11 + x01 + x11 + 2.0F * csum1 - C30;
				float C12 = 3.0F * p11 - 2.0F * y10 - y11 - 3.0F * csum2 - C02;
				float C13 = -2.0F * p11 + y10 + y11 + 2.0F * csum2 - C03;
				float C11 = x01 - C13 - C12 - x00;

				float u = p.x - cx;
				float u2 = u * u;
				float u3 = u * u2;
				float v = p.y - cy;
				float v2 = v * v;
				float v3 = v * v2;
				float density = p00 + x00 * u + y00 * v + C20 * u2 + C02 * v2
						+ C30 * u3 + C03 * v3 + C21 * u2 * v + C31 * u3 * v
						+ C12 * u * v2 + C13 * u * v3 + C11 * u * v;

				float pressure = density - 1.0F;
				if (pressure > 2.0F) {
					pressure = 2.0F;
				}

				float fx = 0.0F;
				float fy = 0.0F;

				if (p.x < 4.0F)
					fx += p.mat.m * (4.0F - p.x);
				else if (p.x > this.gsizeX - 5) {
					fx += p.mat.m * (this.gsizeX - 5 - p.x);
				}
				if (p.y < 4.0F)
					fy += p.mat.m * (4.0F - p.y);
				else if (p.y > this.gsizeY - 5) {
					fy += p.mat.m * (this.gsizeY - 5 - p.y);
				}

				if (drag) {
					float vx = Math.abs(p.x - 0.25F * this.mx);
					float vy = Math.abs(p.y - 0.25F * this.my);
					if ((vx < 10.0F) && (vy < 10.0F)) {
						float weight = p.mat.m * (1.0F - vx / 10.0F)
								* (1.0F - vy / 10.0F);
						fx += weight * (mdx - p.u);
						fy += weight * (mdy - p.v);
					}
				}

				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						Node n = this.grid[(p.cx + i)][(p.cy + j)];
						float phi = p.px[i] * p.py[j];
						float gx = p.gx[i] * p.py[j];
						float gy = p.px[i] * p.gy[j];

						n.ax += -(gx * pressure) + fx * phi;
						n.ay += -(gy * pressure) + fy * phi;
					}
				}
			}

			for (Node n : this.active) {
				if (n.m > 0.0F) {
					n.ax /= n.m;
					n.ay /= n.m;
					n.ay += 0.03F;
				}
			}
			for (Particle p : getParticles()) {
				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						Node n = this.grid[(p.cx + i)][(p.cy + j)];
						float phi = p.px[i] * p.py[j];
						p.u += phi * n.ax;
						p.v += phi * n.ay;

					}
				}
				float mu = p.mat.m * p.u;
				float mv = p.mat.m * p.v;
				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						Node n = this.grid[(p.cx + i)][(p.cy + j)];
						float phi = p.px[i] * p.py[j];
						n.u += phi * mu;
						n.v += phi * mv;
					}
				}
			}
			for (Node n : this.active) {
				if (n.m > 0.0F) {
					n.u /= n.m;
					n.v /= n.m;
				}
			}
			for (Particle p : getParticles()) {
				float gu = 0.0F;
				float gv = 0.0F;
				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						Node n = this.grid[(p.cx + i)][(p.cy + j)];
						float phi = p.px[i] * p.py[j];
						gu += phi * n.u;
						gv += phi * n.v;
					}
				}
				p.x += gu;
				p.y += gv;
				p.u += 1.0F * (gu - p.u);
				p.v += 1.0F * (gv - p.v);

				// addition : polarity
				if (p.polarityPlus) {
					p.x += -(float) Math.random() * POLARITY_EFFECT;
				} else {
					p.x += (float) Math.random() * POLARITY_EFFECT;
				}
				// addition : sensor
				p.x += -(float) getSensorX() * SENSOR_EFFECT_X;
				p.y += -(float) getSensorY() * SENSOR_EFFECT_Y;

				if (p.x < 1.0F) {
					p.x = (1.0F + (float) Math.random() * 0.01F);
					p.u = 0.0F;
				} else if (p.x > this.gsizeX - 2) {
					p.x = (this.gsizeX - 2 - (float) Math.random() * 0.01F);
					p.u = 0.0F;
				}
				if (p.y < 1.0F) {
					p.y = (1.0F + (float) Math.random() * 0.01F);
					p.v = 0.0F;
				} else if (p.y > this.gsizeY - 2) {
					p.y = (this.gsizeY - 2 - (float) Math.random() * 0.01F);
					p.v = 0.0F;
				}
			}
		}

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
		}
	}

	
}
