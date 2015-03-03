package com.ezartech.ezar;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * This class echoes a string called from JavaScript.
 */
public class ezAR extends CordovaPlugin {
	private static final String TAG = "ezAR";
	
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

	private void init(CallbackContext callbackContext) {
        JSONObject jsonObject = new JSONObject();
        try {
			jsonObject.put("displayWidth", 100);
			jsonObject.put("displayHeight", 100);
			
			JSONObject jsonFront = new JSONObject(); 
			jsonObject.put("FRONT", jsonFront);
			
			JSONObject jsonBack = new JSONObject(); 
			jsonObject.put("BACK", jsonBack);
		} catch (JSONException e) {
			Log.e(TAG, "Can't set exception", e);
		}
        
        
		callbackContext.success(jsonObject);
    }
    
    private void startCamera(double pos, double zoom, double light) {
    	Log.v(TAG, "startCamera " + pos + "," + zoom + "," + light);
    }
}
