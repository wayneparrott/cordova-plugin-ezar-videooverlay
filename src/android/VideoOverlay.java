/**
 *
 * Copyright 2015, ezAR Technologies
 * http://ezartech.com
 *
 * By @wayne_parrott, @vridosh, @kwparrott
 *
 * Licensed under a modified MIT license. 
 * Please see LICENSE or http://ezartech.com/ezarstartupkit-license for more information
 *
 */
package com.ezartech.ezar;

import java.io.IOException;
import java.util.List;
import org.apache.cordova.CallbackContext;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class VideoOverlay extends ViewGroup {
	private static final String TAG = "VideoOverlay";

	private static final int[] ROTATIONS = {
		android.view.Surface.ROTATION_0,
		android.view.Surface.ROTATION_90,
		android.view.Surface.ROTATION_180,
		android.view.Surface.ROTATION_270
	};

	private Camera camera = null;
	private int cameraId = Integer.MIN_VALUE;

	private Camera.Size currentSize;
	private boolean recording = false;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private boolean paused = false;
	private Callback callback;

	private Object mSupportedPreviewSizes;

	public VideoOverlay(final Context context) {
		super(context);

		this.surfaceView = new SurfaceView(context);

		this.surfaceHolder = surfaceView.getHolder();
		this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		this.callback = new Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder surfaceHolder) {
				Log.d(TAG, "surfaceCreated called");
				onSurfaceCreated();
			}

			@Override
			public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {
				Log.d(TAG, "surfaceChanged called");
				onSurfaceChanged(format, w, h);
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
				Log.d(TAG, "surfaceDestroyed called");
				onSurfaceDestroyed();
			}
		};

		Log.d(TAG, "surfaceHolder.addCallback");
		surfaceHolder.addCallback(callback);

		addView(surfaceView);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int numChildren = getChildCount();
		if (changed && numChildren > 0) {
			for (int i = 0; i < numChildren; i++) {
				View v = getChildAt(i);
				v.layout(0, 0, r - l, b - t);
			}
		}
	}

	public boolean isRecording() {
		return recording;
	}

	public void startRecording(final Facing facing, final double zoom, final double light, final Runnable onDone) {
		Log.d(TAG, "startRecording called " + facing + 
				" " + zoom + 
				" " + light + 
				" " + "isShown " + surfaceView.isShown() + 
				" " + surfaceView.getWidth() + ", " + surfaceView.getHeight());

		final int cameraFacing = facing.getCameraInfoFacing();

		// Find the total number of cameras available
		int mNumberOfCameras = Camera.getNumberOfCameras();

		Log.v(TAG, "Cameras:" + mNumberOfCameras);

		// Find the ID of the back-facing ("default") camera
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		int cameraIdToOpen = -1;
		for (int i = 0; i < mNumberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);

			Log.v(TAG, "Camera facing:" + cameraInfo.facing);

			if (cameraInfo.facing == cameraFacing) {
				cameraIdToOpen = i;
			}
		}

		if (isRecording()) {
			if (cameraIdToOpen != cameraId) {
				try {
					stopRecording();
				} catch (IOException e) {
					Log.e(TAG, "FAiled to stop recoring", e);
				}
			} else {
				Log.d(TAG, "Camera is already started");
				return;
			}
		}

		if (cameraIdToOpen == -1) {
			cameraIdToOpen = mNumberOfCameras - 1;
		}

		if (cameraIdToOpen != -1) {
			camera = Camera.open(cameraIdToOpen);
			cameraId = cameraIdToOpen;
		}

		if (camera == null) {
			throw new NullPointerException("Cannot start recording, we don't have a camera!");
		}

		Camera.Parameters cameraParameters = camera.getParameters();

		List<String> focusModes = cameraParameters.getSupportedFocusModes();
		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
			cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		}

		camera.setParameters(cameraParameters);

		Log.d(TAG, "====================== CALCULATING PREVIEW SIZE ======================" + getResources().getDisplayMetrics().widthPixels + "/" + getResources().getDisplayMetrics().heightPixels);

		List<Size> sizes = cameraParameters.getSupportedPreviewSizes();
		if (mSupportedPreviewSizes == null) {
			mSupportedPreviewSizes = sizes;
		}

		final double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().heightPixels;

		Size optimalSize1 = null;

		if (sizes != null) {
			double minDiff = Double.MAX_VALUE;

			int targetHeight = getResources().getDisplayMetrics().heightPixels;

			// Find size
			for (Size size : sizes) {
				Log.d(TAG, "Supported preview size " + size.width + "," + size.height);

				double ratio = (double) size.width / size.height;
				if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize1 = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}

			if (optimalSize1 == null) {
				minDiff = Double.MAX_VALUE;
				for (Size size : sizes) {
					if (Math.abs(size.height - targetHeight) < minDiff) {
						optimalSize1 = size;
						minDiff = Math.abs(size.height - targetHeight);
					}
				}
			}
		}

		Size optimalSize = optimalSize1;

		currentSize = optimalSize;
		cameraParameters.setPreviewSize(currentSize.width, currentSize.height);

		camera.setParameters(cameraParameters);

		doUpdateDisplayOrientation();

		Log.v(TAG, "camera.setPreviewDisplay1");

		surfaceHolder.addCallback(callback);

		try {
			camera.setPreviewDisplay(surfaceHolder);
		} catch (IOException e) {
			Log.e(TAG, "Unable to attach preview to camera!", e);
		}

		camera.startPreview();
		recording = true;

		onDone.run();
	}

	public void stopRecording() throws IOException {
		Log.d(TAG, "stopRecording called");

		if (camera != null) {
			camera.setPreviewDisplay(null);

			camera.stopPreview();
			camera.release();
		}
		camera = null;

		recording = false;
	}

	void onSurfaceCreated() {
		Log.d(TAG, "onSurfaceCreated");

		if (paused) {
			try {
				camera.setPreviewDisplay(surfaceHolder);
			} catch (IOException e) {
				Log.d(TAG, "PROBLEM", e);
			}

			doUpdateDisplayOrientation();
			camera.startPreview();
			recording = true;
			paused = false;
		}

	}

	void onSurfaceChanged(int format, int w, int h) {
		if (camera != null) {
			doUpdateDisplayOrientation();
			return;
		}		

		// Now let's start camera again if needed
//		if (paused) {
//			Log.d(TAG, "camera.startPreview");
//			camera.startPreview();
//			paused = false;
//		}		
	}

	private int getOrientationInDegrees() {		
		Activity context = (Activity) getContext();
		int degrees = 0;
		int ori = context.getWindowManager().getDefaultDisplay().getRotation();
		for (int i = 0; i < ROTATIONS.length; i++) {
			if (ROTATIONS[i] == ori) {
				degrees = i*90;
				break;
			}
		}

		return degrees;
	}

	private void doUpdateDisplayOrientation() {
		int degrees = getOrientationInDegrees();

		Camera.CameraInfo info = new Camera.CameraInfo();  
		Camera.getCameraInfo(cameraId, info);

		Facing facing = Facing.fromCameraInfo(info);

		if (facing.isFlipping()) {
			camera.setDisplayOrientation((360 + 360 - degrees - info.orientation) % 360);
		} else {
			camera.setDisplayOrientation((360 + info.orientation -degrees) % 360);
		}

	}

	void onSurfaceDestroyed() {
		Log.d(TAG, "onSurfaceDestroyed");
	}

	/*
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (currentSize != null) {
			int orientationDegrees = getOrientationInDegrees();
			int measuredWidth = getMeasuredWidth();
			int measuredHeight = getMeasuredHeight();

			double measuredRatio = ((double)measuredWidth) / (double)measuredHeight;
			double cameraRatio =
					(orientationDegrees % 180 != 0) ? 
							(double)currentSize.width/ (double)currentSize.height :
							(double)currentSize.height/ (double)currentSize.width;

			int correctedWidth = (int) 
					(cameraRatio < measuredRatio ? 
							((double)measuredWidth / measuredRatio * cameraRatio) : 
							measuredWidth);

			int correctedHeight = (int) 
					(cameraRatio > measuredRatio ? 
							((double)measuredHeight / measuredRatio * cameraRatio) : 
							measuredHeight);

			Log.d(TAG, "MW: " + measuredWidth + "," + measuredHeight +
					" CW: " + correctedWidth + "," + correctedHeight + " === " +
					measuredRatio + "->" + cameraRatio + 
					" (" + (double)correctedWidth/(double)correctedHeight + ")");

			// scrollTo((- measuredWidth + correctedWidth)/2, (- measuredHeight + correctedHeight)/2);
			surfaceView.setLeft(100);
			surfaceView.setTop(100);
			setMeasuredDimension(correctedWidth, correctedHeight);
		}
	}
	*/

	public void onResume() {
		Log.d(TAG, "onResume called");
	}

	public void onPause() {		
		Log.d(TAG, "onPause called");

		try {
			camera.stopPreview();
			camera.setPreviewDisplay(null);
		} catch (IOException e) {
			Log.d(TAG, "PROBLEM", e);
		}

		recording = false;
		paused = true;

		Log.d(TAG, "onPause END");
	}

	public void onDestroy() {
		Log.d(TAG, "onDestroy called");

		onPause();
	}

	public void setZoom(int doubleOrNull, CallbackContext callbackContext) {
		try {
		Parameters parameters = camera.getParameters();
		parameters.setZoom(doubleOrNull);
		camera.setParameters(parameters);

		callbackContext.success();
		} catch (Throwable e) {
			callbackContext.error(e.getMessage());
		}
	}

	public void setLight(int intOrNull, CallbackContext callbackContext) {
		try {
		Parameters parameters = camera.getParameters();
		parameters.setFlashMode(intOrNull == 1 ? 
				Parameters.FLASH_MODE_TORCH :
				Parameters.FLASH_MODE_OFF);
		camera.setParameters(parameters);
		callbackContext.success();
		} catch (Throwable e) {
			callbackContext.error(e.getMessage());
		}
	}
}
