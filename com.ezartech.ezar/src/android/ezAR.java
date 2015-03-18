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

import java.io.IOException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

/**
 * This class echoes a string called from JavaScript.
 */
public class ezAR extends CordovaPlugin {
	private static final String TAG = "ezAR";
	
    private VideoOverlay videoOverlay;
	
    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
    	super.initialize(cordova, webView);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {           	
                webView.setKeepScreenOn(true);
                webView.setBackgroundColor(0x00000000); // transparent RGB
                // webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

                videoOverlay = new VideoOverlay(cordova.getActivity());

                try {                	               	
                	// Set to 1 because we cannot have a transparent surface view, therefore view is not shown / tiny.
                	ViewGroup vg = (ViewGroup) webView.getParent();
                	vg.removeView(webView);

                    cordova.getActivity().setContentView(videoOverlay, 
                    		new ViewGroup.LayoutParams(
                    				LayoutParams.MATCH_PARENT, 
                    				LayoutParams.MATCH_PARENT));

                    cordova.getActivity().addContentView(webView, 
                    		new ViewGroup.LayoutParams(
                    				LayoutParams.WRAP_CONTENT, 
                    				LayoutParams.WRAP_CONTENT));
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
        	Log.v(TAG, action + " " + args);
        	
        	this.startCamera(
        			args.getString(0), 
        			getDoubleOrNull(args, 1), 
        			getDoubleOrNull(args, 2),
        			callbackContext);

        	return true;
        } else if (action.equals("stopCamera")) {
        	try {
				videoOverlay.stopRecording();
				
				callbackContext.success();
			} catch (IOException e) {
				callbackContext.error("PROBLEM " + e.getMessage());
			}
        } else if (action.equals("setZoom")) {
        	videoOverlay.setZoom(getIntOrNull(args, 0), callbackContext);
        } else if (action.equals("setLight")) {
        	videoOverlay.setLight(getIntOrNull(args, 0), callbackContext);
        }
        return false;
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
    
    private void startCamera(String type, double zoom, double light, CallbackContext callbackContext) {
    	videoOverlay.startRecording(Facing.valueOf(type), zoom, light);

    	Log.v(TAG, "startRecording DONE");
    	
    	if (callbackContext != null) {
    		callbackContext.success();
    	}
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
