/**
 * Copyright 2015, ezAR Technologies
 * http://ezartech.com
 *
 * By @wayne_parrott, @vridosh, @kwparrott
 *
 * Licensed under a modified MIT license. 
 * Please see LICENSE or http://ezartech.com/ezarstartupkit-license for more information
 *
 */
package com.ezartech.ezar.videooverlay;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaActionSound;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;

/**
 * This class echoes a string called from JavaScript.
 */
public class ezAR extends CordovaPlugin {
	private static final String TAG = "ezAR";

	//event type code
	static final int UNDEFINED 	= -1;
	static final int STARTED 	= 0;
	static final int STOPPED 	= 1;


	static int CNT = 0;


	private Activity activity;
	private View webViewView;
	private TextureView cameraView;
	private MediaActionSound mSound;
	private int displayWidth, displayHeight;
	private Camera camera = null;
	private int cameraId = -1;
	private CameraDirection cameraDirection;
	private Camera.Size previewSize = null;
	private boolean isPreviewing = false;
	private boolean isPaused = false;
	private float currentZoom;
	private float currentLight;
	private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
	static {
		ORIENTATIONS.append(Surface.ROTATION_0, 90);
		ORIENTATIONS.append(Surface.ROTATION_90, 0);
		ORIENTATIONS.append(Surface.ROTATION_180, 270);
		ORIENTATIONS.append(Surface.ROTATION_270, 180);
	}

	/**
	 * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
	 * {@link TextureView}.
	 */
	private TextureView.SurfaceTextureListener mSurfaceTextureListener =
			new TextureView.SurfaceTextureListener() {

		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
											  int width, int height) {
			if (isPreviewing) {
				startPreview(cameraDirection,currentZoom,currentLight,null);
			}
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
												int width, int height) {
			if (isPreviewing) {
				updateCameraDisplayOrientation();
				cameraView.setTransform(computePreviewTransform(width, height));
			}
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
		}

	};

	@Override
	public void initialize(final CordovaInterface cordova, final CordovaWebView cvWebView) {
		super.initialize(cordova, cvWebView);

		webViewView = cvWebView.getView();

		activity = cordova.getActivity();
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {

				//configure webview
				webViewView.setKeepScreenOn(true);
				webViewView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
				webViewView.setBackgroundColor(Color.BLACK);

				//temporarily remove webview from view stack
				((ViewGroup) webViewView.getParent()).removeView(webViewView);

				//create & add videoOverlay to view stack
				cameraView = new TextureView(activity);
				cameraView.setBackgroundColor(Color.BLACK);
				activity.setContentView(cameraView,
						new ViewGroup.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT));
				cameraView.setSurfaceTextureListener(mSurfaceTextureListener);

				//add webview on top of videoOverlay
				activity.addContentView(webViewView,
						new ViewGroup.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT));

				mSound = new MediaActionSound();
				mSound.load(MediaActionSound.SHUTTER_CLICK);
			}
		});
	}


	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Log.v(TAG, action + " " + args.length());

		if (action.equals("init")) {
			this.init(callbackContext);
			return true;
		} else if (action.equals("startCamera")) {
			this.startPreview(
					args.getString(0),
					getDoubleOrNull(args, 1),
					getDoubleOrNull(args, 2),
					callbackContext);

			return true;
		} else if (action.equals("stopCamera")) {
			this.stopPreview(callbackContext);

			return true;
		} else if (action.equals("setZoom")) {
			this.setZoom(getIntOrNull(args, 0), callbackContext);

			return true;
		} else if (action.equals("setLight")) {
			this.setLight(getIntOrNull(args, 0), callbackContext);

			return true;
		}

		return false;
	}

	private void init(final CallbackContext callbackContext) {
		JSONObject jsonObject = new JSONObject();
		try {
			Display display = activity.getWindowManager().getDefaultDisplay();
			DisplayMetrics m = new DisplayMetrics();
			display.getMetrics(m);

			displayWidth = m.widthPixels;
			displayHeight = m.heightPixels;

			jsonObject.put("displayWidth", displayWidth);
			jsonObject.put("displayHeight", displayHeight);


			int mNumberOfCameras = Camera.getNumberOfCameras();

			Log.v(TAG, "Cameras:" + mNumberOfCameras);

			// Find the ID of the back-facing ("default") camera
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			for (int i = 0; i < mNumberOfCameras; i++) {
				Camera.getCameraInfo(i, cameraInfo);

				Parameters parameters;
				Camera open = null;
				try {
					open = Camera.open(i);
					parameters = open.getParameters();
				} finally {
					if (open != null) {
						open.release();
					}
				}

				Log.v(TAG, "Camera facing:" + cameraInfo.facing);

				CameraDirection type = null;
				for (CameraDirection f : CameraDirection.values()) {
					if (f.getOrdinal() == cameraInfo.facing) {
						type = f;
					}
				}

				if (type != null) {
					JSONObject jsonCamera = new JSONObject();
					jsonCamera.put("id", i);
					jsonCamera.put("position", type.toString());
					jsonCamera.put("zoom", parameters.getZoom());
					jsonCamera.put("maxZoom", parameters.getMaxZoom());

					Log.v(TAG, "HAS LIGHT:" + (parameters.getFlashMode() == null ? false : true));

					jsonCamera.put("light", parameters.getFlashMode() == null ? false : true);
					jsonCamera.put("lightLevel", 0.);

					jsonObject.put(type.toString(), jsonCamera);
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "Can't set exception", e);
		}

		callbackContext.success(jsonObject);
	}

	private void startPreview(final String cameraDirName,
							  final double zoom,
							  final double light,
							  final CallbackContext callbackContext) {

		CameraDirection cameraDir = CameraDirection.valueOf(cameraDirName);

		Log.d(TAG, "startRecording called " + cameraDir +
				" " + zoom +
				" " + light +
				" " + "isShown " + cameraView.isShown() +
				" " + cameraView.getWidth() + ", " + cameraView.getHeight());

		startPreview(cameraDir, zoom, light, callbackContext);
	}


	private void startPreview(final CameraDirection cameraDir,
							 final double zoom,
							 final double light,
							 final CallbackContext callbackContext) {

		if (isPreviewing) {
			if (cameraId != getCameraId(cameraDir)) {
				stopPreview(null);
			}
		}

		cameraId = getCameraId(cameraDir);
		cameraDirection = cameraDir;

		if (null == activity || activity.isFinishing()) {
			return;
		}

		if (cameraId != -1) {
			camera = Camera.open(cameraId);
		}

		if (camera == null) {
			//todo replace with returning an error

			throw new NullPointerException("Cannot start recording, we don't have a camera!");
		}

		initCamera(camera);

		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					isPreviewing = true;
					updateCameraDisplayOrientation();
					cameraView.setTransform(computePreviewTransform(cameraView.getWidth(), cameraView.getHeight()));
					camera.startPreview();
					webViewView.setBackgroundColor(Color.TRANSPARENT);
					setZoom((int) zoom, null);

					sendFlashlightEvent(STARTED, cameraDirection, cameraId, camera);

					if (callbackContext != null) {
						callbackContext.success();
					}

				} catch (Exception e) {
					Log.e(TAG, "Error during preview create", e);
					// callbackContext.error(TAG + ": " + e.getMessage());
				}
			}

		});
	}

	private void stopPreview(final CallbackContext callbackContext) {
		Log.d(TAG, "stopRecording called");

		if (!isPreviewing) { //do nothing if not currently previewing
			if (callbackContext != null) {
				callbackContext.success();
			}
			return;
		}

		try {
			camera.setPreviewDisplay(null);
			camera.stopPreview();
			sendFlashlightEvent(STOPPED,cameraDirection,cameraId,null);
			camera.release();
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					webViewView.setBackgroundColor(Color.BLACK);
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}

		cameraId = UNDEFINED;
		camera = null;
		isPreviewing = false;

		if (callbackContext != null) {
			callbackContext.success();
		}
	}

	private void setLight(final int lightValue, final CallbackContext callbackContext) {
		try {
			Parameters parameters = camera.getParameters();
			parameters.setFlashMode(lightValue == 1 ?
					Parameters.FLASH_MODE_TORCH :
					Parameters.FLASH_MODE_OFF);
			camera.setParameters(parameters);
			currentLight = lightValue;
			if (callbackContext != null) {
				callbackContext.success();
			}
		} catch (Throwable e) {
			if (callbackContext != null) {
				callbackContext.error(e.getMessage());
			}
		}
	}

	private void setZoom(final int zoomValue, final CallbackContext callbackContext) {
		try {
			Parameters parameters = camera.getParameters();
			parameters.setZoom(zoomValue);
			camera.setParameters(parameters);
			currentZoom = zoomValue;
			if (callbackContext != null) {
				callbackContext.success();
			}
		} catch (Throwable e) {
			if (callbackContext != null) {
				callbackContext.error(e.getMessage());
			}
		}
	}

	private void initCamera(Camera camera) {
		Camera.Parameters cameraParameters = camera.getParameters();

		List<String> focusModes = cameraParameters.getSupportedFocusModes();
		if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
			cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			camera.setParameters(cameraParameters);
		}

		//camera.enableShutterSound(true);

		previewSize = chooseOptimalPreviewSize(cameraParameters.getSupportedPreviewSizes(),
				cameraView.getWidth(), cameraView.getHeight());

		chooseOptimalPictureSize(cameraParameters.getSupportedPictureSizes(), 0, 0);

		cameraParameters.setPreviewSize(previewSize.width, previewSize.height);

		//cameraParameters.setPictureSize(previewSize.width, previewSize.height);
		Camera.Size s = cameraParameters.getPreviewSize();
		Log.i(TAG,"PIC SZ W: " + s.width + "  H: " + s.height);

		camera.setParameters(cameraParameters);

		try {
			camera.setPreviewTexture(cameraView.getSurfaceTexture());
		} catch (IOException e) {
			Log.e(TAG, "Unable to attach preview to camera!", e);
		}
	}


	private Camera.Size chooseOptimalPreviewSize(List<Camera.Size> previewSizes, int width, int height) {
		Log.i(TAG, "=== CALCULATING PREVIEW SIZE ===  " + displayWidth + "/" + displayHeight);
		Log.i(TAG, "start: optimal size, width: " + width + " height: " + height);

		int maxWidth = 0;
		int maxHeight = 0;

		Camera.Size bestSize = null;
		for (Camera.Size size : previewSizes) {
			Log.d(TAG, "preview size " + size.width + "," + size.height);

			float r1 = (float)size.width / (float)size.height;
			float r2 = 4f / 3f;
			if (r1 == r2) return size;

			if (size.width * size.height > maxWidth * maxHeight) {
					bestSize = size;
					maxWidth = bestSize.width;
					maxHeight = bestSize.height;
			}
		}

		return  bestSize;
	}


	@Override
	public void onPause(boolean multitasking) {
		super.onPause((multitasking));
		onPause();
	}

	private void onPause() {
		Log.d(TAG, "onPause called");

		camera.stopPreview();
		camera.release();
		camera = null;
		isPaused = true;

		Log.d(TAG, "onPause END");
	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);
		onResume();
	}

	private void onResume() {
		Log.v(TAG, "onResume");

		isPaused = false;

		Log.v(TAG, "onResume DONE");
	}


	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.v(TAG, "onDestroy");
		onPause();
		Log.v(TAG, "onDestroy DONE");
	}

	private int getCameraId(CameraDirection cameraDir) {

		// Find number of cameras available
		int numberOfCameras = Camera.getNumberOfCameras();
		Log.v(TAG, "Cameras:" + numberOfCameras);

		// Find ID of the back-facing ("default") camera
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		int cameraIdToOpen = -1;
		for(int i=0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);

			Log.v(TAG, "Camera facing:" + cameraInfo.facing);

			if (cameraInfo.facing == cameraDir.getOrdinal()) {
				cameraIdToOpen = i;
				break;
			}
		}

		if (cameraIdToOpen == -1) {
			cameraIdToOpen = numberOfCameras - 1;
		}

		return cameraIdToOpen;
	}

	public void updateCameraDisplayOrientation() {
		int result = getRoatationAngle(cameraId);
		camera.setDisplayOrientation(result);

		Camera.Parameters params = camera.getParameters();
		params.setRotation(result);
		camera.setParameters(params);

		Log.i(TAG, "updateCameraDeviceOrientation: " + result);
	}

	/**
	 * Get Rotation Angle
	 *
	 * @param cameraId probably front cam
	 * @return angel to rotate
	 */
	public int getRoatationAngle(int cameraId) {
		Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				break;
		}
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	private Matrix computePreviewTransform(int width, int height)
	{
		Log.d(TAG, "computeTransform, width: " + width + " ht: " + height);

		if (!isPreviewing) return new Matrix();
		Camera.Parameters cameraParameters = camera.getParameters();
		Camera.Size sz = cameraParameters.getPreviewSize();
		Log.d(TAG, "computeTransform, pvwidth: " + sz.width + " pvht: " + sz.height);

		boolean isPortrait = false;

		Display display = activity.getWindowManager().getDefaultDisplay();
		if (display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180) isPortrait = true;
		else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) isPortrait = false;

		int previewWidth = previewSize.width;
		int previewHeight = previewSize.height;

		if (isPortrait)
		{
			previewWidth = previewSize.height;
			previewHeight = previewSize.width;
		}
		float scaleX = 1;
		float scaleY = 1;

		if (isPortrait) {
			scaleX = (float)height / (float)previewHeight * (float)previewWidth / (float)width;
		} else {
			scaleY = (float)width / (float)previewWidth * (float)previewHeight / (float)height;
		}
		Log.d(TAG, "computeMatrix, scaledX: " + scaleX + " scaleY: " + scaleY);

		Matrix matrix = new Matrix();
		matrix.setScale(scaleX, scaleY);

		return matrix;
	}


	private Matrix computePictureTransform(int width, int height)
	{
		Log.d(TAG, "computePICTURETransform, width: " + width + " ht: " + height);

		Camera.Parameters cameraParameters = camera.getParameters();
		Camera.Size sz = cameraParameters.getPictureSize();
		Log.d(TAG, "computePICTURETransform, pic width: " + sz.width + " pic ht: " + sz.height);

		boolean isPortrait = false;

		Display display = activity.getWindowManager().getDefaultDisplay();
		if (display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180) isPortrait = true;
		else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) isPortrait = false;

		int previewWidth = previewSize.width;
		int previewHeight = previewSize.height;

		if (isPortrait)
		{
			previewWidth = previewSize.height;
			previewHeight = previewSize.width;
		}
		float scaleX = 1;
		float scaleY = 1;

		if (isPortrait) {
			scaleX = (float)height / (float)previewHeight * (float)previewWidth / (float)width;
		} else {
			scaleY = (float)width / (float)previewWidth * (float)previewHeight / (float)height;
		}

		scaleX = 1.0f;

		Log.d(TAG, "computeMatrix, scaledX: " + scaleX + " scaleY: " + scaleY);

		Matrix matrix = new Matrix();
		matrix.setScale(scaleX, scaleY);

		return matrix;
	}

	private static int getIntOrNull(JSONArray args, int i) {
		if (args.isNull(i)) {
			return Integer.MIN_VALUE;
		}

		try {
			return args.getInt(i);
		} catch (JSONException e) {
			Log.e(TAG, "Can't get double", e);
			throw new RuntimeException(e);
		}
	}

	private static double getDoubleOrNull(JSONArray args, int i) {
		if (args.isNull(i)) {
			return Double.NaN;
		}

		try {
			return args.getDouble(i);
		} catch (JSONException e) {
			Log.e(TAG, "Can't get double", e);
			throw new RuntimeException(e);
		}
	}

	private void chooseOptimalPictureSize(List<Camera.Size> previewSizes, int width, int height) {
		for (Camera.Size size : previewSizes) {
			Log.d(TAG, "PICTURE size " + size.width + "," + size.height);
		}

	}

  //------------- used by Flashlight plugin --------------------
  //HACK using IPC between videoOverlayPlugin and flashlight plugin
  //TODO: refactor to use events and listener pattern

	public Camera getActiveCamera() {
		return camera;
	}

	public Camera getBackCamera() {
		Camera camera = null;
		if (cameraDirection == CameraDirection.BACK) {
			camera = this.camera;
		}
		return camera;
	}

	public Camera getFrontCamera() {
		Camera camera = null;
		if (cameraDirection == CameraDirection.FRONT) {
			camera = this.camera;
		}
		return camera;
	}

	//reflectively access VideoOverlay plugin to get camera in same direction as lightLoc
	private void sendFlashlightEvent(int state, CameraDirection cameraDirection, int cameraId, Camera camera) {

		CordovaPlugin flashlightPlugin = getFlashlightPlugin();
		if (flashlightPlugin == null) {
			return;
		}

		Method method = null;

		try {
			if (state == STARTED) {
				method = flashlightPlugin.getClass().getMethod("videoOverlayStarted", int.class, int.class, Camera.class );
			} else {
				method = flashlightPlugin.getClass().getMethod("videoOverlayStopped", int.class, int.class, Camera.class );
			}
		} catch (SecurityException e) {
			//e.printStackTrace();
		} catch (NoSuchMethodException e) {
			//e.printStackTrace();
		}

		try {
			if (method == null) return;

			method.invoke(flashlightPlugin, cameraDirection.ordinal(), cameraId, camera);

		} catch (IllegalArgumentException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (IllegalAccessException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (InvocationTargetException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		}
	}

	private CordovaPlugin getFlashlightPlugin() {
		String pluginName = "flashlight";
		CordovaPlugin flashlightPlugin = webView.getPluginManager().getPlugin(pluginName);
		return flashlightPlugin;
	}

}


