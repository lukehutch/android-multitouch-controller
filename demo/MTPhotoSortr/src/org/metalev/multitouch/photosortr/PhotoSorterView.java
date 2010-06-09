/**
 * PhotoSorterView.java
 * 
 * (c) Luke Hutchison (luke.hutch@mit.edu)
 * 
 * Released under the Apache License v2.
 */
package org.metalev.multitouch.photosorter;

import java.util.ArrayList;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

public class PhotoSorterView extends View implements MultiTouchObjectCanvas<PhotoSorterView.Img> {

	private static final int[] IMAGES = { R.drawable.m74hubble, R.drawable.catarina, R.drawable.tahiti,
			R.drawable.sunset, R.drawable.lake };

	private ArrayList<Img> mImages = new ArrayList<Img>();

	// --

	private MultiTouchController<Img> multiTouchController;

	// --

	private PointInfo debugTouchPoint;

	private boolean mShowDebugInfo = false;

	private Paint mLinePaintSingleTouch = new Paint();

	private Paint mLinePaintMultiTouchCoords = new Paint();

	private Paint mLinePaintMultiTouchCenter = new Paint();

	private Paint mLinePaintCrossHairs = new Paint();

	// ---------------------------------------------------------------------------------------------------

	public PhotoSorterView(Context context) {
		this(context, null);
	}

	public PhotoSorterView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PhotoSorterView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		Resources res = context.getResources();

		multiTouchController = new MultiTouchController<Img>(this);
		debugTouchPoint = new PointInfo();

		for (int i = 0; i < IMAGES.length; i++)
			mImages.add(new Img(IMAGES[i], res));

		mLinePaintSingleTouch.setColor(Color.GREEN);
		mLinePaintSingleTouch.setStrokeWidth(5);
		mLinePaintSingleTouch.setStyle(Style.STROKE);
		mLinePaintSingleTouch.setAntiAlias(true);
		mLinePaintMultiTouchCoords.setColor(Color.RED);
		mLinePaintMultiTouchCoords.setStrokeWidth(5);
		mLinePaintMultiTouchCoords.setStyle(Style.STROKE);
		mLinePaintMultiTouchCoords.setAntiAlias(true);
		mLinePaintMultiTouchCenter.setColor(Color.YELLOW);
		mLinePaintMultiTouchCenter.setStrokeWidth(5);
		mLinePaintMultiTouchCenter.setStyle(Style.STROKE);
		mLinePaintMultiTouchCenter.setAntiAlias(true);
		mLinePaintCrossHairs.setColor(Color.BLUE);
		mLinePaintCrossHairs.setStrokeWidth(5);
		mLinePaintCrossHairs.setStyle(Style.STROKE);
		mLinePaintCrossHairs.setAntiAlias(true);
		setBackgroundColor(Color.BLACK);
	}

	/** Called by activity's onResume() method to load the images */
	public void loadImages(Context context) {
		Resources res = context.getResources();
		int n = mImages.size();
		for (int i = 0; i < n; i++)
			mImages.get(i).load(res);
	}

	/** Called by activity's onPause() method to free memory used for loading the images */
	public void unloadImages() {
		int n = mImages.size();
		for (int i = 0; i < n; i++)
			mImages.get(i).unload();
	}

	// ---------------------------------------------------------------------------------------------------

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		int n = mImages.size();
		for (int i = 0; i < n; i++)
			mImages.get(i).draw(canvas);
		if (mShowDebugInfo)
			drawMultitouchDebugMarks(canvas);
	}

	// ---------------------------------------------------------------------------------------------------

	public void toggleShowDebugInfo() {
		mShowDebugInfo = !mShowDebugInfo;
		invalidate();
	}

	private void drawMultitouchDebugMarks(Canvas canvas) {
		if (debugTouchPoint.isDown()) {
			float cw = getWidth(), ch = getHeight();
			float x = debugTouchPoint.getX(), y = debugTouchPoint.getY(), pressure = debugTouchPoint.getPressure();
			boolean isMultiTouch = debugTouchPoint.isMultiTouch();
			canvas.drawLine(0, y, cw, y, mLinePaintCrossHairs);
			canvas.drawLine(x, 0, x, ch, mLinePaintCrossHairs);
			canvas.drawCircle(x, y, 70 + pressure * 120, (isMultiTouch ? mLinePaintMultiTouchCenter : mLinePaintSingleTouch));
			if (isMultiTouch) {
				float multiTouchDiameter = debugTouchPoint.getMultiTouchDiameter();
				float r = multiTouchDiameter / 2;
				canvas.drawCircle(x, y, r, mLinePaintMultiTouchCoords);
				float dx2 = debugTouchPoint.getMultiTouchWidth() / 2, dy2 = debugTouchPoint.getMultiTouchHeight() / 2;
				canvas.drawLine(x + dx2, 0, x + dx2, ch, mLinePaintMultiTouchCoords);
				canvas.drawLine(x - dx2, 0, x - dx2, ch, mLinePaintMultiTouchCoords);
				canvas.drawLine(0, y + dy2, cw, y + dy2, mLinePaintMultiTouchCoords);
				canvas.drawLine(0, y - dy2, cw, y - dy2, mLinePaintMultiTouchCoords);
				canvas.drawLine(x + dx2, y + dy2, x - dx2, y - dy2, mLinePaintMultiTouchCoords);
				canvas.drawLine(x + dx2, y - dy2, x - dx2, y + dy2, mLinePaintMultiTouchCoords);
			}
		}
	}

	// ---------------------------------------------------------------------------------------------------

	/** Pass touch events to the MT controller */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return multiTouchController.onTouchEvent(event);
	}
	
	/** Get the image that is under the single-touch point, or return null (canceling the drag op) if none */
	public Img getDraggableObjectAtPoint(PointInfo pt) {
		float x = pt.getX(), y = pt.getY();
		int n = mImages.size();
		for (int i = n - 1; i >= 0; i--) {
			Img im = mImages.get(i);
			if (im.containsPoint(x, y))
				return im;
		}
		return null;
	}

	/**
	 * Select an object for dragging. Called whenever an object is found to be under the point (non-null is returned by
	 * getDraggableObjectAtPoint()) and a drag operation is starting. Called with null when drag op ends.
	 */
	public void selectObject(Img img, PointInfo touchPoint) {
		debugTouchPoint.set(touchPoint);
		if (img != null) {
			// Move image to the top of the stack when selected
			mImages.remove(img);
			mImages.add(img);
		} else {
			// Called with img == null when drag stops.
		}
		invalidate();
	}

	/** Get the current position and scale of the selected image. Called whenever a drag starts or is reset. */
	public void getPositionAndScale(Img img, PositionAndScale objPosAndScaleOut) {
		objPosAndScaleOut.set(img.getCenterX(), img.getCenterY(), img.getScale());
	}

	/** Set the position and scale of the dragged/stretched image. */
	public boolean setPositionAndScale(Img img, PositionAndScale newImgPosAndScale, PointInfo touchPoint) {
		debugTouchPoint.set(touchPoint);
		float x = newImgPosAndScale.getXOff();
		float y = newImgPosAndScale.getYOff();
		float scale = newImgPosAndScale.getScale();
		boolean ok = img.setPos(x, y, scale);
		if (ok)
			invalidate();
		return ok;
	}

	// ----------------------------------------------------------------------------------------------

	class Img {
		private int resId;

		private Drawable drawable;

		private boolean firstLoad;

		private int width, height, displayWidth, displayHeight;

		private float centerX, centerY, scale;

		private float minX, maxX, minY, maxY;

		private static final float SCREEN_MARGIN = 100;

		public Img(int resId, Resources res) {
			this.resId = resId;
			this.firstLoad = true;
			getMetrics(res);
		}

		private void getMetrics(Resources res) {
			DisplayMetrics metrics = res.getDisplayMetrics();
			// The DisplayMetrics don't seem to always be updated on screen rotate, so we hard code a portrait
			// screen orientation for the non-rotated screen here...
			//this.displayWidth = metrics.widthPixels;
			//this.displayHeight = metrics.heightPixels;
			this.displayWidth = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? Math.max(
					metrics.widthPixels, metrics.heightPixels) : Math.min(metrics.widthPixels, metrics.heightPixels);
			this.displayHeight = res.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? Math.min(
					metrics.widthPixels, metrics.heightPixels) : Math.max(metrics.widthPixels, metrics.heightPixels);
		}

		/** Called by activity's onResume() method to load the images */
		public void load(Resources res) {
			getMetrics(res);
			this.drawable = res.getDrawable(resId);
			this.width = drawable.getIntrinsicWidth();
			this.height = drawable.getIntrinsicHeight();
			float cx, cy, sc;
			if (firstLoad) {
				cx = SCREEN_MARGIN + (float) (Math.random() * (displayWidth - 2 * SCREEN_MARGIN));
				cy = SCREEN_MARGIN + (float) (Math.random() * (displayHeight - 2 * SCREEN_MARGIN));
				sc = (float) (Math.max(displayWidth, displayHeight) / (float) Math.max(width, height) * Math.random() * 0.3 + 0.2);
				firstLoad = false;
			} else {
				// Reuse position and scale information if it is available
				// FIXME this doesn't actually work because the whole activity is torn down and re-created on rotate
				cx = this.centerX;
				cy = this.centerY;
				sc = this.scale;
				// Make sure the image is not off the screen after a screen rotation
				if (this.maxX < SCREEN_MARGIN)
					cx = SCREEN_MARGIN;
				else if (this.minX > displayWidth - SCREEN_MARGIN)
					cx = displayWidth - SCREEN_MARGIN;
				if (this.maxY > SCREEN_MARGIN)
					cy = SCREEN_MARGIN;
				else if (this.minY > displayHeight - SCREEN_MARGIN)
					cy = displayHeight - SCREEN_MARGIN;
			}
			setPos(cx, cy, sc);
		}

		/** Called by activity's onPause() method to free memory used for loading the images */
		public void unload() {
			this.drawable = null;
		}

		/** Set the position and scale of an image in screen coordinates */
		private boolean setPos(float centerX, float centerY, float scale) {
			float ws = (width / 2) * scale, hs = (height / 2) * scale;
			float newMinX = centerX - ws, newMinY = centerY - hs, newMaxX = centerX + ws, newMaxY = centerY + hs;
			if (newMinX > displayWidth - SCREEN_MARGIN || newMaxX < SCREEN_MARGIN || newMinY > displayHeight - SCREEN_MARGIN
					|| newMaxY < SCREEN_MARGIN)
				return false;
			this.centerX = centerX;
			this.centerY = centerY;
			this.scale = scale;
			this.minX = newMinX;
			this.minY = newMinY;
			this.maxX = newMaxX;
			this.maxY = newMaxY;
			return true;
		}

		/** Return whether or not the given screen coords are inside this image */
		public boolean containsPoint(float scrnX, float scrnY) {
			return (scrnX >= minX && scrnX <= maxX && scrnY >= minY && scrnY <= maxY);
		}

		public void draw(Canvas canvas) {
			drawable.setBounds((int) minX, (int) minY, (int) maxX, (int) maxY);
			drawable.draw(canvas);
		}

		public Drawable getDrawable() {
			return drawable;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public float getCenterX() {
			return centerX;
		}

		public float getCenterY() {
			return centerY;
		}

		public float getScale() {
			return scale;
		}

		public float getMinX() {
			return minX;
		}

		public float getMaxX() {
			return maxX;
		}

		public float getMinY() {
			return minY;
		}

		public float getMaxY() {
			return maxY;
		}
	}
}
