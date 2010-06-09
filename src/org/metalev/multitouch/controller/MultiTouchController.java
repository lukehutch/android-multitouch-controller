package org.metalev.multitouch.controller;

/**
 * MultiTouchController.java
 * 
 * Author: Luke Hutchison (luke.hutch@mit.edu)
 *   Please drop me an email if you use this code so I can list your project here!
 * 
 * Usage:
 *   Create a class that extends View and implements MultiTouchObjectCanvas<T>, where T is the type of object that you want the controller
 *   to be able to drag and scale.  See comments in the definition interface MultiTouchObjectCanvas<T> below for information on correctly
 *   implementing the interface.
 * 
 * Changelog:
 *   2010-06-09 v1.3.2  Another bugfix for Android-2.1 (LH)
 *   2010-06-09 v1.3.1  Bugfix for Android-2.1 (only got a single touch point on 2.1, should be fixed now) (LH)
 *   2010-06-09 v1.3    Ported to Android-2.2 (handle ACTION_POINTER_* actions); fixed several bugs; refactoring; documentation (LH) 
 *   2010-05-17 v1.2.1  Dual-licensed under Apache and GPL licenses
 *   2010-02-18 v1.2    Support for compilation under Android 1.5/1.6 using introspection (mmin, author of handyCalc)
 *   2010-01-08 v1.1.1  Bugfixes to Cyanogen's patch that only showed up in more complex uses of controller (LH) 
 *   2010-01-06 v1.1    Modified for official level 5 MT API (Cyanogen)
 *   2009-01-25 v1.0    Original MT controller, released for hacked G1 kernel (LH) 
 * 
 * Known usages:
 * - Yuan Chin's fork of ADW Launcher to support multitouch
 * - David Byrne's fractal viewing app Fractoid
 * - mmin's handyCalc calculator
 * - My own "MultiTouch Visualizer 2" in the Market
 * - Formerly: The browser in cyanogenmod (and before that, JesusFreke), and other firmwares like dwang5.  This usage has been
 *   replaced with official pinch/zoom in Maps, Browser and Gallery[3D] as of API level 5.
 * 
 * License:
 *   Dual-licensed under the Apache License v2 and the GPL v2.
 */

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.util.Log;
import android.view.MotionEvent;

/**
 * A class that simplifies the implementation of multitouch in applications. Subclass this and read the fields here as needed in subclasses.
 * 
 * @author Luke Hutchison
 */
public class MultiTouchController<T> {

	/**
	 * Time in ms required after a change in event status (e.g. putting down or lifting off the second finger) before events actually do anything --
	 * helps eliminate noisy jumps that happen on change of status
	 */
	private static final long EVENT_SETTLE_TIME_INTERVAL = 20;

	// The biggest possible abs val of the change in x or y between multitouch events
	// (larger dx/dy events are ignored) -- helps eliminate jumping on finger 2 up/down
	private static final float MAX_MULTITOUCH_POS_JUMP_SIZE = 30.0f;

	// The biggest possible abs val of the change in multitouchWidth or multitouchHeight between
	// multitouch events (larger-jump events are ignored) -- helps eliminate jumping on finger 2 up/down
	private static final float MAX_MULTITOUCH_DIM_JUMP_SIZE = 40.0f;

	// The smallest possible distance between multitouch points (used to avoid div-by-zero errors and display glitches)
	private static final float MIN_MULTITOUCH_SEPARATION = 30.0f;

	// The max number of touch points that can be present on the screen at once
	public static final int MAX_TOUCH_POINTS = 10;

	// --

	MultiTouchObjectCanvas<T> objectCanvas;

	private PointInfo currPt, prevPt;

	// --

	private T draggedObject = null;

	private long dragStartTime, dragSettleTime;

	// Conversion from object coords to screen coords, and from drag width to object scale
	private float objDraggedPointX, objDraggedPointY, objStartScale;

	private PositionAndScale objPosAndScale = new PositionAndScale();

	// --

	/** Whether to handle single-touch events/drags before multi-touch is initiated or not; if not, they are handled by subclasses */
	private boolean handleSingleTouchEvents;

	// --

	// Note: the mode number is also the number of fingers down on the screen (except 2 => 2+)

	private static final int MODE_NOTHING = 0;

	private static final int MODE_DRAG = 1;

	private static final int MODE_STRETCH = 2;

	private int dragMode = MODE_NOTHING;

	// ------------------------------------------------------------------------------------

	/** Constructor that sets handleSingleTouchEvents to true */
	public MultiTouchController(MultiTouchObjectCanvas<T> objectCanvas) {
		this(objectCanvas, true);
	}

	/** Full constructor */
	public MultiTouchController(MultiTouchObjectCanvas<T> objectCanvas, boolean handleSingleTouchEvents) {
		this.currPt = new PointInfo();
		this.prevPt = new PointInfo();
		this.handleSingleTouchEvents = handleSingleTouchEvents;
		this.objectCanvas = objectCanvas;
	}

	// ------------------------------------------------------------------------------------

	/**
	 * Whether to handle single-touch events/drags before multi-touch is initiated or not; if not, they are handled by subclasses. Default: true
	 */
	protected void setHandleSingleTouchEvents(boolean handleSingleTouchEvents) {
		this.handleSingleTouchEvents = handleSingleTouchEvents;
	}

	/**
	 * Whether to handle single-touch events/drags before multi-touch is initiated or not; if not, they are handled by subclasses. Default: true
	 */
	protected boolean getHandleSingleTouchEvents() {
		return handleSingleTouchEvents;
	}

	// ------------------------------------------------------------------------------------

	public static final boolean multiTouchSupported;
	private static Method m_getPointerCount;
	private static Method m_findPointerIndex;
	private static Method m_getPressure;
	private static Method m_getHistoricalX;
	private static Method m_getHistoricalY;
	private static Method m_getHistoricalPressure;
	private static Method m_getX;
	private static Method m_getY;
	private static int ACTION_POINTER_UP = 6;
	private static int ACTION_POINTER_INDEX_SHIFT = 8;

	static {
		boolean succeeded = false;
		try {
			// Android 2.0.1 stuff:
			m_getPointerCount = MotionEvent.class.getMethod("getPointerCount");
			m_findPointerIndex = MotionEvent.class.getMethod("findPointerIndex", Integer.TYPE);
			m_getPressure = MotionEvent.class.getMethod("getPressure", Integer.TYPE);
			m_getHistoricalX = MotionEvent.class.getMethod("getHistoricalX", Integer.TYPE, Integer.TYPE);
			m_getHistoricalY = MotionEvent.class.getMethod("getHistoricalY", Integer.TYPE, Integer.TYPE);
			m_getHistoricalPressure = MotionEvent.class.getMethod("getHistoricalPressure", Integer.TYPE, Integer.TYPE);
			m_getX = MotionEvent.class.getMethod("getX", Integer.TYPE);
			m_getY = MotionEvent.class.getMethod("getY", Integer.TYPE);
			succeeded = true;
		} catch (Exception e) {
			Log.e("MultiTouchController", "static initializer failed", e);
		}
		multiTouchSupported = succeeded;
		if (multiTouchSupported) {
			// Android 2.2+ stuff (the original Android 2.2 consts are declared above,
			// and these actions aren't used previous to Android 2.2):
			try {
				Field up = MotionEvent.class.getField("ACTION_POINTER_UP");
				if (up != null)
					ACTION_POINTER_UP = up.getInt(null);
			} catch (Exception e) {
			}
			try {
				Field shift = MotionEvent.class.getField("ACTION_POINTER_INDEX_SHIFT");
				if (shift != null)
					ACTION_POINTER_INDEX_SHIFT = shift.getInt(null);
			} catch (Exception e) {
			}
		}
	}

	// ------------------------------------------------------------------------------------

	private static final float[] xVals = new float[MAX_TOUCH_POINTS];
	private static final float[] yVals = new float[MAX_TOUCH_POINTS];
	private static final float[] pressureVals = new float[MAX_TOUCH_POINTS];
	private static final int[] pointerIdxs = new int[MAX_TOUCH_POINTS];

	/** Process incoming touch events */
	public boolean onTouchEvent(MotionEvent event) {
		try {
			int pointerCount = multiTouchSupported ? (Integer) m_getPointerCount.invoke(event) : 1;
			if (dragMode == MODE_NOTHING && !handleSingleTouchEvents && pointerCount == 1)
				// Not handling initial single touch events, just pass them on
				return false;

			// Handle history first (we sometimes get history with ACTION_MOVE events)
			int action = event.getAction();
			int histLen = event.getHistorySize() / pointerCount;
			for (int histIdx = 0; histIdx <= histLen; histIdx++) {
				// Read from history entries until histIdx == histLen, then read from current event
				boolean processingHist = histIdx < histLen;
				if (!multiTouchSupported || pointerCount == 1) {
					xVals[0] = processingHist ? event.getHistoricalX(histIdx) : event.getX();
					yVals[0] = processingHist ? event.getHistoricalY(histIdx) : event.getY();
					pressureVals[0] = processingHist ? event.getHistoricalPressure(histIdx) : event.getPressure();
				} else {
					// Read x, y and pressure of each pointer
					int numPointers = Math.min(pointerCount, MAX_TOUCH_POINTS);
					for (int i = 0; i < numPointers; i++) {
						int ptrIdx = (Integer) m_findPointerIndex.invoke(event, i);
						pointerIdxs[i] = ptrIdx;
						// N.B. if pointerCount == 1, then the following methods throw an array index out of range exception,
						// and the code above is therefore required not just for Android 1.5/1.6 but also for when there is
						// only one touch point on the screen -- pointlessly inconsistent :(
						xVals[i] = (Float) (processingHist ? m_getHistoricalX.invoke(event, ptrIdx, histIdx) : m_getX.invoke(event, ptrIdx));
						yVals[i] = (Float) (processingHist ? m_getHistoricalY.invoke(event, ptrIdx, histIdx) : m_getY.invoke(event, ptrIdx));
						pressureVals[i] = (Float) (processingHist ? m_getHistoricalPressure.invoke(event, ptrIdx, histIdx) : m_getPressure.invoke(
								event, ptrIdx));
					}
				}
				// Decode event
				decodeTouchEvent(pointerCount, xVals, yVals, pressureVals, pointerIdxs, //
						/* action = */processingHist ? MotionEvent.ACTION_MOVE : action, //
						/* down = */processingHist ? true : action != MotionEvent.ACTION_UP //
								&& (action & ((1 << ACTION_POINTER_INDEX_SHIFT) - 1)) != ACTION_POINTER_UP //
								&& action != MotionEvent.ACTION_CANCEL, //
						processingHist ? event.getHistoricalEventTime(histIdx) : event.getEventTime());
			}

			return true;
		} catch (Exception e) {
			// In case any of the introspection stuff fails (it shouldn't)
			Log.e("MultiTouchController", "onTouchEvent() failed", e);
			return false;
		}
	}

	private void decodeTouchEvent(int pointerCount, float[] x, float[] y, float[] pressure, int[] pointerIdxs, int action, boolean down,
			long eventTime) {
		// Swap curr/prev points
		PointInfo tmp = prevPt;
		prevPt = currPt;
		currPt = tmp;
		// Overwrite old prev point
		currPt.set(pointerCount, x, y, pressure, pointerIdxs, action, down, eventTime);
		multiTouchController();
	}

	// ------------------------------------------------------------------------------------

	/** Start dragging/stretching, or reset drag/stretch to current point if something goes out of range */
	private void resetDrag() {
		if (draggedObject == null)
			return;

		// Get dragged object position and scale
		objectCanvas.getPositionAndScale(draggedObject, objPosAndScale);

		// Figure out the object coords of the drag start point's screen coords.
		// All stretching should be around this point in object-coord-space.
		float scaleInv = (objPosAndScale.scale == 0.0f ? 1.0f : 1.0f / objPosAndScale.scale);
		objDraggedPointX = (currPt.getX() - objPosAndScale.xOff) * scaleInv;
		objDraggedPointY = (currPt.getY() - objPosAndScale.yOff) * scaleInv;

		// Figure out ratio between object scale factor and multitouch diameter (they are linearly correlated)
		float diam = currPt.getMultiTouchDiameter();
		objStartScale = objPosAndScale.scale / (diam == 0.0f ? 1.0f : diam);
	}

	/** Drag/stretch the dragged object to the current touch position and diameter */
	private void performDrag() {
		// Don't do anything if we're not dragging anything
		if (draggedObject == null)
			return;

		// Calc new position of dragged object
		float scale = (objPosAndScale.scale == 0.0f ? 1.0f : objPosAndScale.scale);
		float newObjPosX = currPt.getX() - objDraggedPointX * scale;
		float newObjPosY = currPt.getY() - objDraggedPointY * scale;

		// Get new drag diameter (avoiding divsion by zero), and calculate new drag scale
		float diam;
		if (!currPt.isMultiTouch) {
			// Single-touch, no change in scale
			diam = 1.0f;
		} else {
			diam = currPt.getMultiTouchDiameter();
			if (diam < MIN_MULTITOUCH_SEPARATION)
				diam = MIN_MULTITOUCH_SEPARATION;
		}
		float newScale = diam * objStartScale;

		// Get the new obj coords and scale, and set them (notifying the subclass of the change)
		objPosAndScale.set(newObjPosX, newObjPosY, newScale);
		boolean success = objectCanvas.setPositionAndScale(draggedObject, objPosAndScale, currPt);
		if (!success)
			; // If we could't set those params, do nothing currently
	}

	/** The main single-touch and multi-touch logic */
	private void multiTouchController() {

		switch (dragMode) {
		case MODE_NOTHING:
			// Not doing anything currently
			if (currPt.isDown()) {
				// Start a new single-point drag
				draggedObject = objectCanvas.getDraggableObjectAtPoint(currPt);
				if (draggedObject != null) {
					// Started a new single-point drag
					dragMode = MODE_DRAG;
					objectCanvas.selectObject(draggedObject, currPt);
					resetDrag();
					// Don't need any settling time if just placing one finger, there is no noise
					dragStartTime = dragSettleTime = currPt.getEventTime();
				}
			}
			break;

		case MODE_DRAG:
			// Currently in a single-point drag
			if (!currPt.isDown()) {
				// First finger was released, stop dragging
				dragMode = MODE_NOTHING;
				objectCanvas.selectObject((draggedObject = null), currPt);

			} else if (currPt.isMultiTouch()) {
				// Point 1 was already down and point 2 was just placed down
				dragMode = MODE_STRETCH;
				// Restart the drag with the new drag position (that is at the midpoint between the touchpoints)
				resetDrag();
				// Need to let events settle before moving things, to help with event noise on touchdown
				dragStartTime = currPt.getEventTime();
				dragSettleTime = dragStartTime + EVENT_SETTLE_TIME_INTERVAL;

			} else {
				// Point 1 is still down and point 2 did not change state, just do single-point drag to new location
				if (currPt.getEventTime() < dragSettleTime) {
					// Ignore the first few events if we just stopped stretching, because if finger 2 was kept down while
					// finger 1 is lifted, then point 1 gets mapped to finger 2. Restart the drag from the new position.
					resetDrag();
				} else {
					// Keep dragging, move to new point
					performDrag();
				}
			}
			break;

		case MODE_STRETCH:
			// Two-point stretch
			if (!currPt.isMultiTouch() || !currPt.isDown()) {
				// Dropped one or both points, stop stretching

				if (!currPt.isDown()) {
					// Dropped both points, go back to doing nothing
					dragMode = MODE_NOTHING;
					objectCanvas.selectObject((draggedObject = null), currPt);

				} else {
					// Just dropped point 2, downgrade to a single-point drag
					dragMode = MODE_DRAG;
					// Restart the drag with the single-finger position
					resetDrag();
					// Ignore the first few events after the drop, in case we dropped finger 1 and left finger 2 down
					dragStartTime = currPt.getEventTime();
					dragSettleTime = dragStartTime + EVENT_SETTLE_TIME_INTERVAL;
				}
			} else {
				// Keep stretching

				if (Math.abs(currPt.getX() - prevPt.getX()) > MAX_MULTITOUCH_POS_JUMP_SIZE
						|| Math.abs(currPt.getY() - prevPt.getY()) > MAX_MULTITOUCH_POS_JUMP_SIZE
						|| Math.abs(currPt.getMultiTouchWidth() - prevPt.getMultiTouchWidth()) * .5f > MAX_MULTITOUCH_DIM_JUMP_SIZE
						|| Math.abs(currPt.getMultiTouchHeight() - prevPt.getMultiTouchHeight()) * .5f > MAX_MULTITOUCH_DIM_JUMP_SIZE) {
					// Jumped too far, probably event noise, reset and ignore events for a bit
					resetDrag();
					dragStartTime = currPt.getEventTime();
					dragSettleTime = dragStartTime + EVENT_SETTLE_TIME_INTERVAL;

				} else if (currPt.eventTime < dragSettleTime) {
					// Events have not yet settled, reset
					resetDrag();
				} else {
					// Stretch to new position and size
					performDrag();
				}
			}
			break;
		}
	}

	// ------------------------------------------------------------------------------------

	/** A class that packages up all MotionEvent information with all derived multitouch information (if available) */
	public static class PointInfo {
		private float[] xs = new float[MAX_TOUCH_POINTS];
		private float[] ys = new float[MAX_TOUCH_POINTS];
		private float[] pressures = new float[MAX_TOUCH_POINTS];
		private int[] pointerIdxs = new int[MAX_TOUCH_POINTS];
		private int numPoints;

		private float xMid, yMid, pressureMid, dx, dy, diameter, diameterSq, angle;

		private boolean down, isMultiTouch, diameterSqIsCalculated, diameterIsCalculated, angleIsCalculated;

		private int action;

		private long eventTime;

		// -------------------------------------------------------------------------------------------------------------------------------------------

		private void set(int numPoints, float[] x, float[] y, float[] pressure, int[] pointerIdxs, int action, boolean down, long eventTime) {
			// Log.i("Multitouch", "x: " + x + " y: " + y + " pointerCount: " + pointerCount +
			// " x2: " + x2 + " y2: " + y2 + " action: " + action + " down: " + down);
			this.eventTime = eventTime;
			this.action = action;
			this.numPoints = numPoints;
			for (int i = 0; i < numPoints; i++) {
				this.xs[i] = x[i];
				this.ys[i] = y[i];
				this.pressures[i] = pressure[i];
				this.pointerIdxs[i] = pointerIdxs[i];
			}
			this.down = down;
			this.isMultiTouch = numPoints >= 2;

			if (isMultiTouch) {
				xMid = (x[0] + x[1]) * .5f;
				yMid = (y[0] + y[1]) * .5f;
				pressureMid = (pressure[0] + pressure[1]) * .5f;
				dx = Math.abs(x[1] - x[0]);
				dy = Math.abs(y[1] - y[0]);

			} else {
				// Single-touch event
				xMid = x[0];
				yMid = y[0];
				pressureMid = pressure[0];
				dx = dy = 0.0f;
			}
			// Need to re-calculate the expensive params if they're needed
			diameterSqIsCalculated = diameterIsCalculated = angleIsCalculated = false;
		}

		/** Copy all fields */
		public void set(PointInfo other) {
			this.numPoints = other.numPoints;
			for (int i = 0; i < numPoints; i++) {
				this.xs[i] = other.xs[i];
				this.ys[i] = other.ys[i];
				this.pressures[i] = other.pressures[i];
				this.pointerIdxs[i] = other.pointerIdxs[i];
			}
			this.xMid = other.xMid;
			this.yMid = other.yMid;
			this.pressureMid = other.pressureMid;
			this.dx = other.dx;
			this.dy = other.dy;
			this.diameter = other.diameter;
			this.diameterSq = other.diameterSq;
			this.angle = other.angle;
			this.down = other.down;
			this.action = other.action;
			this.isMultiTouch = other.isMultiTouch;
			this.diameterIsCalculated = other.diameterIsCalculated;
			this.diameterSqIsCalculated = other.diameterSqIsCalculated;
			this.angleIsCalculated = other.angleIsCalculated;
			this.eventTime = other.eventTime;
		}

		// -------------------------------------------------------------------------------------------------------------------------------------------

		public boolean isMultiTouch() {
			return isMultiTouch;
		}

		public float getMultiTouchWidth() {
			return dx;
		}

		public float getMultiTouchHeight() {
			return dy;
		}

		// Fast integer sqrt, by Jim Ulery. Should be faster than Math.sqrt()
		private int julery_isqrt(int val) {
			int temp, g = 0, b = 0x8000, bshft = 15;
			do {
				if (val >= (temp = (((g << 1) + b) << bshft--))) {
					g += b;
					val -= temp;
				}
			} while ((b >>= 1) > 0);
			return g;
		}

		/** Calculate the squared diameter of the multitouch event, and cache it. Use this if you don't need to perform the sqrt. */
		public float getMultiTouchDiameterSq() {
			if (!diameterSqIsCalculated) {
				diameterSq = (isMultiTouch ? dx * dx + dy * dy : 0.0f);
				diameterSqIsCalculated = true;
			}
			return diameterSq;
		}

		/** Calculate the diameter of the multitouch event, and cache it. Uses fast int sqrt but gives accuracy to 1/16px. */
		public float getMultiTouchDiameter() {
			if (!diameterIsCalculated) {
				// Get 1/16 pixel's worth of subpixel accuracy, works on screens up to 2048x2048
				// before we get overflow (at which point you can reduce or eliminate subpix
				// accuracy, or use longs in julery_isqrt())
				float diamSq = getMultiTouchDiameterSq();
				diameter = (diamSq == 0.0f ? 0.0f : (float) julery_isqrt((int) (256 * diamSq)) / 16.0f);
				// Make sure diameter is never less than dx or dy, for trig purposes
				if (diameter < dx)
					diameter = dx;
				if (diameter < dy)
					diameter = dy;
				diameterIsCalculated = true;
			}
			return diameter;
		}

		/**
		 * Calculate the angle of a multitouch event, and cache it. Actually gives the smaller of the two angles between the x axis and the line
		 * between the two touchpoints, so range is [0,Math.PI/2]. Uses Math.atan2().
		 */
		public float getMultiTouchAngle() {
			if (!angleIsCalculated) {
				if (!isMultiTouch)
					angle = 0.0f;
				else
					angle = (float) Math.atan2(ys[1] - ys[0], xs[1] - xs[0]);
				angleIsCalculated = true;
			}
			return angle;
		}

		// -------------------------------------------------------------------------------------------------------------------------------------------

		/** Return the total number of touch points */
		public int getNumTouchPoints() {
			return numPoints;
		}

		/** Return the X coord of the first touch point if there's only one, or the midpoint between first and second touch points if two or more. */
		public float getX() {
			return xMid;
		}

		/** Return the array of X coords -- only the first getNumTouchPoints() of these is defined. */
		public float[] getXs() {
			return xs;
		}

		/** Return the X coord of the first touch point if there's only one, or the midpoint between first and second touch points if two or more. */
		public float getY() {
			return yMid;
		}

		/** Return the array of Y coords -- only the first getNumTouchPoints() of these is defined. */
		public float[] getYs() {
			return ys;
		}

		/**
		 * Return the array of pointer indices -- only the first getNumTouchPoints() of these is defined. These don't have to be all the numbers from
		 * 0 to getNumTouchPoints()-1 inclusive, numbers can be skipped if a finger is lifted and the touch sensor is capable of detecting that that
		 * particular touch point is no longer down. Note that most sensors do not have this capability: when finger 1 is lifted up finger 2 becomes
		 * the new finger 1.
		 */
		public int[] getPointerIndices() {
			return pointerIdxs;
		}

		/** Return the pressure the first touch point if there's only one, or the average pressure of first and second touch points if two or more. */
		public float getPressure() {
			return pressureMid;
		}

		/** Return the array of pressures -- only the first getNumTouchPoints() of these is defined. */
		public float[] getPressures() {
			return pressures;
		}

		// -------------------------------------------------------------------------------------------------------------------------------------------

		public boolean isDown() {
			return down;
		}

		public int getAction() {
			return action;
		}

		public long getEventTime() {
			return eventTime;
		}
	}

	// ------------------------------------------------------------------------------------

	/**
	 * A class that is used to store scroll offsets and scale information for objects that are managed by the multitouch controller
	 */
	public static class PositionAndScale {
		private float xOff, yOff, scale;

		public PositionAndScale() {
		}

		public void set(float xOff, float yOff, float scale) {
			this.xOff = xOff;
			this.yOff = yOff;
			this.scale = scale;
		}

		public float getXOff() {
			return xOff;
		}

		public float getYOff() {
			return yOff;
		}

		public float getScale() {
			return scale;
		}
	}

	// ------------------------------------------------------------------------------------

	public static interface MultiTouchObjectCanvas<T> {

		/**
		 * See if there is a draggable object at the current point. Returns the object at the point, or null if nothing to drag. To start a multitouch
		 * drag/stretch operation, this routine must return some non-null reference to an object. This object is passed into the other methods in this
		 * interface when they are called.
		 * 
		 * @param touchPoint
		 *            The point being tested (in object coordinates). Return the topmost object under this point, or if dragging/stretching the whole
		 *            canvas, just return a reference to the canvas.
		 * @return a reference to the object under the point being tested, or null to cancel the drag operation. If dragging/stretching the whole
		 *         canvas (e.g. in a photo viewer), always return non-null, otherwise the stretch operation won't work.
		 */
		public T getDraggableObjectAtPoint(PointInfo touchPoint);

		/**
		 * Get the screen coords of the dragged object's origin, and scale multiplier to convert screen coords to obj coords. The job of this routine
		 * is to call the .set() method on the passed PositionAndScale object to record the initial position and scale of the object (in object
		 * coordinates) before any dragging/stretching takes place.
		 * 
		 * @param obj
		 *            The object being dragged/stretched.
		 * @param objPosAndScaleOut
		 *            Output parameter: You need to call objPosAndScaleOut.set() to record the current position and scale of obj.
		 */
		public void getPositionAndScale(T obj, PositionAndScale objPosAndScaleOut);

		/**
		 * Callback to update the position and scale (in object coords) of the currently-dragged object.
		 * 
		 * @param obj
		 *            The object being dragged/stretched.
		 * @param newObjPosAndScale
		 *            The new position and scale of the object, in object coordinates. Use this to move/resize the object before returning.
		 * @param touchPoint
		 *            Info about the current touch point, including multitouch information and utilities to calculate and cache multitouch pinch
		 *            diameter etc. (Note: touchPoint is volatile, if you want to keep any fields of touchPoint, you must copy them before the method
		 *            body exits.)
		 * @return true if setting the position and scale of the object was successful, or false if the position or scale parameters are out of range
		 *         for this object.
		 */
		public boolean setPositionAndScale(T obj, PositionAndScale newObjPosAndScale, PointInfo touchPoint);

		/**
		 * Select an object at the given point. Can be used to bring the object to top etc. Only called when first touchpoint goes down, not when
		 * multitouch is initiated. Also called with null on touch-up.
		 * 
		 * @param obj
		 *            The object being selected by single-touch, or null on touch-up.
		 * @param touchPoint
		 *            The current touch point.
		 */
		public void selectObject(T obj, PointInfo touchPoint);
	}
}
