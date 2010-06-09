/**
 * PhotoSorterActivity.java
 * 
 * (c) Luke Hutchison (luke.hutch@mit.edu)
 * 
 * Released under the Apache License v2.
 */
package org.metalev.multitouch.photosorter;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

public class PhotoSorterActivity extends Activity {
	
	PhotoSorterView photoSorter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setTitle(R.string.instructions);
		photoSorter = new PhotoSorterView(this);
		setContentView(photoSorter);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		photoSorter.loadImages(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		photoSorter.unloadImages();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			photoSorter.toggleShowDebugInfo();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}