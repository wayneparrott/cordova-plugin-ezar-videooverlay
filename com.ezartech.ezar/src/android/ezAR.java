package com.ezartech.ezar;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.hardware.Camera;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

/**
 * This class echoes a string called from JavaScript.
 */
public class ezAR extends CordovaPlugin {
	private static final String TAG = "ezAR";
	
    private VideoOverlay videoOverlay;
    private RelativeLayout relativeLayout;
	
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    	Log.v(TAG, "initialize 1");
    	super.initialize(cordova, webView);
    	Log.v(TAG, "initialize 2");
    }
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	Log.v(TAG, action + " " + args.length() + " - " + action.equals("startCamera"));
    	
        if (action.equals("init")) {            
            this.init(callbackContext);
            return true;
        } else if (action.equals("startCamera")) {
        	this.startCamera(
        			getDoubleOrNull(args, 0), getDoubleOrNull(args, 1), getDoubleOrNull(args, 2));  
        	return true;
        }
        return false;
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
			jsonObject.put("displayWidth", 100);
			jsonObject.put("displayHeight", 100);
			
            int mNumberOfCameras = Camera.getNumberOfCameras();
            
            Log.v(TAG, "Cameras:" + mNumberOfCameras);
            
            // Find the ID of the back-facing ("default") camera
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            for (int i = 0; i < mNumberOfCameras; i++) {
                Camera.getCameraInfo(i, cameraInfo);
                
                Log.v(TAG, "Camera facing:" + cameraInfo.facing);
                
                String type = null;
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                	type = "BACK";
                } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                	type = "FRONT";
                }
                
                if (type != null) {
        			JSONObject jsonCamera = new JSONObject(); 
        			jsonObject.put(type, jsonCamera);
                }
            }
		} catch (JSONException e) {
			Log.e(TAG, "Can't set exception", e);
		}
                
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {           	
                webView.setKeepScreenOn(true);
                webView.setBackgroundColor(0x00000000); // transparent RGB
                // webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

                videoOverlay = new VideoOverlay(cordova.getActivity());
                videoOverlay.setCameraFacing("BACK");

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
                    callbackContext.error(TAG + ": " + e.getMessage());
                }
                
            }
        });
        
		callbackContext.success(jsonObject);
    }
    
    private void startCamera(double pos, double zoom, double light) {
    	Log.v(TAG, "startCamera " + pos + "," + zoom + "," + light);
    	videoOverlay.startRecording();
    	Log.v(TAG, "startRecording DONE");
    }
}
