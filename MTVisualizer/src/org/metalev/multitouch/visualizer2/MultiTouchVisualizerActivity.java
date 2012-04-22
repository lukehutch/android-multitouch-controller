/**
 * MultiTouchVisualizerActivity.java
 * 
 * (c) Luke Hutchison (luke.hutch@mit.edu)
 * 
 * Released under the Apache License v2.
 */
package org.metalev.multitouch.visualizer2;

import android.app.Activity;
import android.os.Bundle;

public class MultiTouchVisualizerActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.instructions);
		setContentView(new MultiTouchVisualizerView(this));
	}
}