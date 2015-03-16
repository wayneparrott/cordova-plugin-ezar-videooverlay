package com.ezartech.ezar;

import java.io.IOException;
import java.util.List;

import org.apache.cordova.CallbackContext;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class VideoOverlay extends ViewGroup {
    private static final String TAG = "VideoOverlay";
    private Camera camera = null;
    private Camera.Size currentSize;
    private boolean recording = false;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean paused = false;
	private Callback callback;


    public VideoOverlay(Context context) {
        super(context);

        this.surfaceView = new SurfaceView(context);
        
        this.surfaceHolder = surfaceView.getHolder();
        this.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        callback = new Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceCreated called");
                previewAvailable();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            	Log.d(TAG, "surfaceChanged called");
            	onSurfaceChanged();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d(TAG, "surfaceDestroyed called");
            }
        };
		surfaceHolder.addCallback(callback);
        
        addView(surfaceView);
    }

    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int numChildren = getChildCount();
        if (changed && numChildren > 0) {
            int itemWidth = (r - l) / numChildren;
            for (int i = 0; i < numChildren; i++) {
                View v = getChildAt(i);
                v.layout(itemWidth * i, 0, (i + 1) * itemWidth, b - t);
            }
        }
    }

    public boolean isRecording() {
        return recording;
    }

    public void startRecording(Facing facing, double zoom, double light) {
    	Log.d(TAG, "startRecording called " + facing + " " + zoom + " " + light);
    	
        if (isRecording()) {
            try {
				stopRecording();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }

        final int cameraFacing = facing.getCameraInfoFacing();
        
        // Find the total number of cameras available
        int mNumberOfCameras = Camera.getNumberOfCameras();
        
        Log.v(TAG, "Cameras:" + mNumberOfCameras);
        
        // Find the ID of the back-facing ("default") camera
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < mNumberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);

            Log.v(TAG, "Camera facing:" + cameraInfo.facing);
            
            if (cameraInfo.facing == cameraFacing) {
                camera = Camera.open(i);
            }
        }

        if (camera == null) {
        	camera = Camera.open(mNumberOfCameras - 1);
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

        if (currentSize == null) {
            setCameraParameters(camera, cameraParameters);
        }

        try {
        	Log.v(TAG, "camera.setPreviewDisplay");
            camera.setPreviewDisplay(surfaceHolder);
            Log.v(TAG, "camera.setPreviewDisplay DONE");
        } catch (IOException e) {
            Log.e(TAG, "Unable to attach preview to camera!", e);
        }

        camera.startPreview();
        recording = true;
    }

    public void stopRecording() throws IOException {
        Log.d(TAG, "stopRecording called");

        camera.setPreviewDisplay(null);
        
        camera.stopPreview();
        camera.release();
        camera = null;

        recording = false;
    }

    void previewAvailable() {
        if (paused) {
        	Log.d(TAG, "onResume called !!!");
        	try {
        		camera.setPreviewDisplay(surfaceHolder);
    		} catch (IOException e) {
    			Log.d(TAG, "PROBLEM", e);
    		}

        	camera.startPreview();
        	recording = true;
        	paused = false;
        }

    }
    
    void onSurfaceChanged() {
    	Log.d(TAG, "onSurfaceChanged");

       	// Now let's start camera again if needed
//        if (paused) {
//        	Log.d(TAG, "camera.startPreview");
//        	camera.startPreview();
//        	paused = false;
//        }    	
    }

    private void setCameraParameters(Camera camera, Camera.Parameters parameters){
        Camera.Size size = parameters.getSupportedPreviewSizes().get(0);
		
		if (size == null) {
		    size = parameters.getSupportedPreviewSizes().get(0);
		}
		
		currentSize = size;
        parameters.setPreviewSize(currentSize.width, currentSize.height);
        // parameters.setRotation(0);

        camera.setParameters(parameters);
        // camera.setDisplayOrientation(0);
    }

    public void onResume() {
    	Log.d(TAG, "onResume called");
/*
    	Handler mainHandler = new Handler(getContext().getMainLooper());
    	mainHandler.postDelayed(new Runnable() {
    		public void run() {    	    	        
    		}
    	}, 200);
*/
    }

    public void onPause() {    	
        Log.d(TAG, "onPause called");

        try {
        	camera.stopPreview();
			camera.setPreviewDisplay(null);
			// camera.release();
			// camera = null;
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
		Parameters parameters = camera.getParameters();
		parameters.setZoom(doubleOrNull);
		camera.setParameters(parameters);
		
		callbackContext.success();
	}

	public void setLight(int intOrNull, CallbackContext callbackContext) {
		// camera.stopPreview();
		
		Parameters parameters = camera.getParameters();
		parameters.setFlashMode(intOrNull == 1 ? 
				Parameters.FLASH_MODE_TORCH :
				Parameters.FLASH_MODE_OFF);
		camera.setParameters(parameters);
		
		// camera.startPreview();
		
    	callbackContext.success();
	}
}
