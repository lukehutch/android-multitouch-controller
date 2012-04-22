/**
 * MultiTouchVisualizerView.java
 * 
 * (c) Luke Hutchison (luke.hutch@mit.edu)
 * 
 * Released under the Apache License v2.
 */
package org.metalev.multitouch.visualizer2;

import org.metalev.multitouch.controller.MultiTouchController;
import org.metalev.multitouch.controller.MultiTouchController.MultiTouchObjectCanvas;
import org.metalev.multitouch.controller.MultiTouchController.PointInfo;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class MultiTouchVisualizerView extends View implements MultiTouchObjectCanvas<Object> {

	private MultiTouchController<Object> multiTouchController;

	private PointInfo mCurrTouchPoint;

	// --

	private static final int[] TOUCH_COLORS = { Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.BLUE, Color.WHITE,
			Color.GRAY, Color.LTGRAY, Color.DKGRAY };

	private Paint mLinePaintSingleTouch = new Paint();

	private Paint mLinePaintCoords = new Paint();

	private Paint mLinePaintSnapped = new Paint();

	private Paint mLinePaintSecondTouch = new Paint();

	private Paint mLinePaintMultiTouch = new Paint();

	private Paint mLinePaintCrossHairs = new Paint();

	private Paint mPointLabelPaint = new Paint();

	private Paint mTouchTheScreenLabelPaint = new Paint();

	private Paint mPointLabelBg = new Paint();

	private Paint mAngLabelPaint = new Paint();

	private Paint mAngLabelBg = new Paint();

	private int[] mTouchPointColors = new int[MultiTouchController.MAX_TOUCH_POINTS];

	// ------------------------------------------------------------------------------------

	public MultiTouchVisualizerView(Context context) {
		this(context, null);
	}

	public MultiTouchVisualizerView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MultiTouchVisualizerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		multiTouchController = new MultiTouchController<Object>(this);
		mCurrTouchPoint = new PointInfo();

		mLinePaintSingleTouch.setColor(TOUCH_COLORS[0]);
		mLinePaintSingleTouch.setStrokeWidth(5);
		mLinePaintSingleTouch.setStyle(Style.STROKE);
		mLinePaintSingleTouch.setAntiAlias(true);
		mLinePaintCoords.setColor(Color.RED);
		mLinePaintCoords.setStrokeWidth(5);
		mLinePaintCoords.setStyle(Style.STROKE);
		mLinePaintCoords.setAntiAlias(true);
		mLinePaintSnapped.setColor(Color.BLUE);
		mLinePaintSnapped.setAlpha(128);
		mLinePaintSnapped.setStyle(Style.STROKE);
		mLinePaintSnapped.setStrokeWidth(40);
		mLinePaintCoords.setAntiAlias(false);
		mLinePaintSecondTouch.setColor(TOUCH_COLORS[1]);
		mLinePaintSecondTouch.setStrokeWidth(5);
		mLinePaintSecondTouch.setStyle(Style.STROKE);
		mLinePaintSecondTouch.setAntiAlias(true);
		mLinePaintMultiTouch.setStrokeWidth(5);
		mLinePaintMultiTouch.setStyle(Style.STROKE);
		mLinePaintMultiTouch.setAntiAlias(true);
		mLinePaintCrossHairs.setColor(Color.BLUE);
		mLinePaintCrossHairs.setStrokeWidth(5);
		mLinePaintCrossHairs.setStyle(Style.STROKE);
		mLinePaintCrossHairs.setAntiAlias(true);
		mPointLabelPaint.setTextSize(82);
		mPointLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mPointLabelPaint.setAntiAlias(true);
		mTouchTheScreenLabelPaint.setColor(Color.GRAY);
		mTouchTheScreenLabelPaint.setTextSize(24);
		mTouchTheScreenLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);
		mTouchTheScreenLabelPaint.setAntiAlias(true);
		mPointLabelBg.set(mPointLabelPaint);
		mPointLabelBg.setColor(Color.BLACK);
		mPointLabelBg.setAlpha(180);
		mPointLabelBg.setStyle(Style.STROKE);
		mPointLabelBg.setStrokeWidth(15);
		mAngLabelPaint.setTextSize(32);
		mAngLabelPaint.setTypeface(Typeface.SANS_SERIF);
		mAngLabelPaint.setColor(mLinePaintCrossHairs.getColor());
		mAngLabelPaint.setTextAlign(Align.CENTER);
		mAngLabelPaint.setAntiAlias(true);
		mAngLabelBg.set(mAngLabelPaint);
		mAngLabelBg.setColor(Color.BLACK);
		mAngLabelBg.setAlpha(180);
		mAngLabelBg.setStyle(Style.STROKE);
		mAngLabelBg.setStrokeWidth(15);
		setBackgroundColor(Color.BLACK);

		for (int i = 0; i < MultiTouchController.MAX_TOUCH_POINTS; i++)
			mTouchPointColors[i] = i < TOUCH_COLORS.length ? TOUCH_COLORS[i] : (int) (Math.random() * 0xffffff) + 0xff000000;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Pass the event on to the controller
		return multiTouchController.onTouchEvent(event);
	}

	public Object getDraggableObjectAtPoint(PointInfo pt) {
		// IMPORTANT: to start a multitouch drag operation, this routine must return non-null
		return this;
	}

	public void getPositionAndScale(Object obj, PositionAndScale objPosAndScaleOut) {
		// We aren't dragging any objects, so this doesn't do anything in this app
	}

	public void selectObject(Object obj, PointInfo touchPoint) {
		// We aren't dragging any objects in this particular app, but this is called when the point goes up (obj == null) or down (obj != null),
		// save the touch point info
		touchPointChanged(touchPoint);
	}

	public boolean setPositionAndScale(Object obj, PositionAndScale newObjPosAndScale, PointInfo touchPoint) {
		// Called during a drag or stretch operation, update the touch point info
		touchPointChanged(touchPoint);
		return true;
	}

	/**
	 * Called when the touch point info changes, causes a redraw.
	 * 
	 * @param touchPoint
	 */
	private void touchPointChanged(PointInfo touchPoint) {
		// Take a snapshot of touch point info, the touch point is volatile
		mCurrTouchPoint.set(touchPoint);
		invalidate();
	}

	private void paintText(Canvas canvas, String msg, float vPos) {
		Rect bounds = new Rect();
		int msgLen = msg.length();
		mTouchTheScreenLabelPaint.getTextBounds(msg, 0, msgLen, bounds);
		canvas.drawText(msg, (canvas.getWidth() - bounds.width()) * .5f, vPos, mTouchTheScreenLabelPaint);
	}

	private static final String[] infoLines = {"Touch the screen", "with one or more", "fingers to test", "multitouch", "characteristics" };
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mCurrTouchPoint.isDown()) {
			int numPoints = mCurrTouchPoint.getNumTouchPoints();
			float[] xs = mCurrTouchPoint.getXs();
			float[] ys = mCurrTouchPoint.getYs();
			float[] pressures = mCurrTouchPoint.getPressures();
			int[] pointerIds = mCurrTouchPoint.getPointerIds();
			float x = mCurrTouchPoint.getX(), y = mCurrTouchPoint.getY();
			float wd = getWidth(), ht = getHeight();

			if (numPoints == 1) {
				// Draw ordinate lines for single touch point
				canvas.drawLine(0, y, wd, y, mLinePaintCoords);
				canvas.drawLine(x, 0, x, ht, mLinePaintCoords);

			} else if (numPoints == 2) {
				float dx2 = mCurrTouchPoint.getMultiTouchWidth() / 2;
				float dy2 = mCurrTouchPoint.getMultiTouchHeight() / 2;

				// Horiz/vert ordinate lines
				if (dx2 < 0.85f) {
					// Snapped (for some reason, it's not precise on the Nexus One due to event noise)
					canvas.drawLine(x, 0, x, ht, mLinePaintSnapped);
				} else {
					canvas.drawLine(x + dx2, 0, x + dx2, ht, mLinePaintCoords);
					canvas.drawLine(x - dx2, 0, x - dx2, ht, mLinePaintCoords);
				}
				if (dy2 < 0.85f) {
					// Snapped
					canvas.drawLine(0, y, wd, y, mLinePaintSnapped);
				} else {
					canvas.drawLine(0, y + dy2, wd, y + dy2, mLinePaintCoords);
					canvas.drawLine(0, y - dy2, wd, y - dy2, mLinePaintCoords);
				}

				// Diag lines
				canvas.drawLine(x + dx2, y + dy2, x - dx2, y - dy2, mLinePaintCrossHairs);
				canvas.drawLine(x + dx2, y - dy2, x - dx2, y + dy2, mLinePaintCrossHairs);

				// Crosshairs
				canvas.drawLine(0, y, wd, y, mLinePaintCrossHairs);
				canvas.drawLine(x, 0, x, ht, mLinePaintCrossHairs);

				// Circle
				canvas.drawCircle(x, y, mCurrTouchPoint.getMultiTouchDiameter() / 2, mLinePaintCrossHairs);
			}

			// Show touch circles
			for (int i = 0; i < numPoints; i++) {
				mLinePaintMultiTouch.setColor(mTouchPointColors[i]);
				float r = 70 + pressures[i] * 120;
				canvas.drawCircle(xs[i], ys[i], r, mLinePaintMultiTouch);
			}

			// Label pinch distance
			if (numPoints == 2) {
				float ang = mCurrTouchPoint.getMultiTouchAngle() * 180.0f / (float) Math.PI;
				// Keep text rightway up
				if (ang < -91.0f)
					ang += 180.0f;
				else if (ang > 91.0f)
					ang -= 180.0f;
				String angStr = "Pinch dist: " + Math.round(mCurrTouchPoint.getMultiTouchDiameter());
				canvas.save();
				canvas.translate(x, y);
				canvas.rotate(ang);
				canvas.drawText(angStr, 0, -10, mAngLabelBg);
				canvas.drawText(angStr, 0, -10, mAngLabelPaint);
				canvas.restore();
			}

			// Log touch point indices
			if (MultiTouchController.DEBUG) {
				StringBuilder buf = new StringBuilder();
				for (int i = 0; i < numPoints; i++)
					buf.append(" " + i + "->" + pointerIds[i]);
				Log.i("MultiTouchVisualizer", buf.toString());
			}

			// Label touch points on top of everything else
			for (int idx = 0; idx < numPoints; idx++) {
				int id = pointerIds[idx];
				mPointLabelPaint.setColor(mTouchPointColors[idx]);
				float r = 70 + pressures[idx] * 120, d = r * .71f;
				String label = (idx + 1) + (idx == id ? "" : "(id:" + (id + 1) + ")");
				canvas.drawText(label, xs[idx] + d, ys[idx] - d, mPointLabelBg);
				canvas.drawText(label, xs[idx] + d, ys[idx] - d, mPointLabelPaint);
			}
		} else {
			float spacing = mTouchTheScreenLabelPaint.getFontSpacing();
			float totHeight = spacing * infoLines.length;
			for (int i = 0; i < infoLines.length; i++)
				paintText(canvas, infoLines[i], (canvas.getHeight() - totHeight) * .5f + i * spacing);
		}
	}
}
