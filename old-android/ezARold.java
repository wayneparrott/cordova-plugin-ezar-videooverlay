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
package com.ezartech.ezar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;

/**
 * This class echoes a string called from JavaScript.
 */
public class ezAR extends CordovaPlugin {
	private static final String TAG = "ezAR";
	
	private VideoOverlay videoOverlay;
	private SurfaceView stoppedCameraView;
	private View activeView = null;
	
	@Override
	public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
		super.initialize(cordova, webView);
		
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {		   	
				webView.getView().setKeepScreenOn(true);
				webView.getView().setBackgroundColor(0x00000000); // transparent RGB
				// webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

				videoOverlay = new VideoOverlay(cordova.getActivity());
				stoppedCameraView = new SurfaceView(cordova.getActivity());

				stoppedCameraView.getHolder().addCallback(new Callback() {
					@Override
					public void surfaceCreated(SurfaceHolder holder) {
						Rect surfaceFrame = holder.getSurfaceFrame();
						Canvas lockCanvas = holder.lockCanvas(surfaceFrame);
						lockCanvas.drawRGB(0, 0, 0);
						holder.unlockCanvasAndPost(lockCanvas);
					}

					@Override
					public void surfaceChanged(SurfaceHolder holder,
							int format, int width, int height) {
					}

					@Override
					public void surfaceDestroyed(SurfaceHolder holder) {
					}					
				});
				stoppedCameraView.setWillNotDraw(false);
				
				// Set to 1 because we cannot have a transparent surface view, therefore view is not shown / tiny.
				ViewGroup vg = (ViewGroup) webView.getView().getParent();
				vg.removeView(webView.getView());

				cordova.getActivity().setContentView(stoppedCameraView, 
						new ViewGroup.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT));

				cordova.getActivity().addContentView(videoOverlay, 
						new ViewGroup.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT));

				cordova.getActivity().addContentView(webView.getView(),
						new ViewGroup.LayoutParams(
								LayoutParams.MATCH_PARENT, 
								LayoutParams.MATCH_PARENT));

				videoOverlay.setVisibility(View.INVISIBLE);
				stoppedCameraView.setVisibility(View.VISIBLE);
				
				activeView = stoppedCameraView;
			}
		});
	}
	
	private void makeViewActive(final View view, final Runnable andThen) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {		   	
				try {
					if (activeView != null) {
						activeView.setVisibility(View.INVISIBLE);
						activeView.invalidate();
					}

					view.setVisibility(View.VISIBLE);
					
					view.invalidate();

					activeView = view;

					andThen.run();
				} catch(Exception e) {
					Log.e(TAG, "Error during preview create", e);
					// callbackContext.error(TAG + ": " + e.getMessage());
				}
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
			this.startCamera(
					args.getString(0),
					getDoubleOrNull(args, 1),
					getDoubleOrNull(args, 2),
					callbackContext);

			return true;
		} else if (action.equals("stopCamera")) {
			this.stopCamera(callbackContext);

			return true;
		} else if (action.equals("setZoom")) {
			this.setZoom(getIntOrNull(args, 0), callbackContext);

			return true;
		} else if (action.equals("setLight")) {
			this.setLight(getIntOrNull(args, 0), callbackContext);

			return true;
		} else if (action.equals("snapshot")) {
            //JPG: 0, PNG: 1
			this.snapshot(0, true, callbackContext);

			return true;
		}

		return false;
	}

	private void startCamera(final String type, 
			final double zoom, 
			final double light, 
			final CallbackContext callbackContext) {
				videoOverlay.startRecording(Facing.valueOf(type), zoom, light, new Runnable() {
					@Override
					public void run() {
						makeViewActive(videoOverlay, new Runnable() {
							@Override
							public void run() {
								Log.v(TAG, "startRecording DONE");
								if (callbackContext != null) {
									callbackContext.success();
								}
							}
				});
			}
		});
	}
	
	private void stopCamera(final CallbackContext callbackContext) {
		makeViewActive(stoppedCameraView, new Runnable() {
			@Override
			public void run() {
				try {
					videoOverlay.stopRecording();
					callbackContext.success();
				} catch (IOException e) {
					callbackContext.error("PROBLEM " + e.getMessage());
				}
			}
		});
	}

	private void setLight(final int lightValue, final CallbackContext callbackContext) {
		videoOverlay.setLight(lightValue, callbackContext);
	}

	private void setZoom(final int zoomValue, final CallbackContext callbackContext) {
		videoOverlay.setZoom(zoomValue, callbackContext);
	}
    
    private void snapshot(final int encodingType, final boolean saveToPhotoAlbum, final CallbackContext callbackContext) {
        //JPG: 0, PNG: 1
        System.out.println("snapshot");

		Window window = cordova.getActivity().getWindow();
		View rootView = window.getDecorView().getRootView();
		rootView.setDrawingCacheEnabled(true);
		Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
		rootView.setDrawingCacheEnabled(false);

		String title = "" + System.currentTimeMillis();
		String path = Environment.getExternalStorageDirectory().toString() + "/" + title;
		OutputStream out = null;
		File imageFile = new File(path);

		try {
			out = new FileOutputStream(imageFile);
			// choose JPEG format
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
			out.flush();
		} catch (FileNotFoundException e) {
			// manage exception
		} catch (IOException e) {
			// manage exception
		} finally {

			try {
				if (out != null) {
					out.close();
					MediaStore.Images.Media.insertImage(
							cordova.getActivity().getContentResolver(),
							bitmap,
							title,
							"");
				}

			} catch (Exception exc) {
			}

		}



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

	private void init(final CallbackContext callbackContext) {
		JSONObject jsonObject = new JSONObject();
		try {
			Display display = cordova.getActivity().getWindowManager().getDefaultDisplay();
			DisplayMetrics m = new DisplayMetrics(); 
			display.getMetrics(m);
			
			jsonObject.put("displayWidth", m.widthPixels);
			jsonObject.put("displayHeight", m.heightPixels);
			
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
				
				Facing type = null;
				for (Facing f : Facing.values()) {
					if (f.getCameraInfoFacing() == cameraInfo.facing) {
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
	
	@Override
	public void onPause(boolean multitasking) {
		super.onPause(multitasking);

		Log.v(TAG, "onPause");
		videoOverlay.onPause();
		Log.v(TAG, "onPause DONE");		
	}

	@Override
	public void onResume(boolean multitasking) {
		super.onResume(multitasking);
		
		Log.v(TAG, "onResume");
		videoOverlay.onResume();
		Log.v(TAG, "onResume DONE");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Log.v(TAG, "onDestroy");
		videoOverlay.onDestroy();
		Log.v(TAG, "onDestroy DONE");
	}
}
