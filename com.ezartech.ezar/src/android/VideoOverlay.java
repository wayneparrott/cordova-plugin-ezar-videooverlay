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
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

public class VideoOverlay extends ViewGroup {
    private static final String TAG = "VideoOverlay";
    
    private Camera camera = null;
    private int cameraId = Integer.MIN_VALUE;
    
    private Camera.Size currentSize;
    private boolean recording = false;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private boolean paused = false;
	private Callback callback;

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

	public void startRecording(final Facing facing, final double zoom, final double light, final Runnable onDone) {
    	Log.d(TAG, "startRecording called " + facing + 
    			" " + zoom + 
    			" " + light + 
    			" " + "isShown " + surfaceView.isShown() + 
    			" " + surfaceView.getWidth() + ", " + surfaceView.getHeight());
    	
    	Log.v(TAG, "H1");
    	
        if (isRecording()) {
            try {
				stopRecording();
			} catch (IOException e) {
				Log.e(TAG, "FAiled to stop recoring", e);
			}
        }
        
        Log.v(TAG, "H2");

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
                cameraId = i;
            }
        }

        if (camera == null) {
        	camera = Camera.open(mNumberOfCameras - 1);
        	cameraId = mNumberOfCameras - 1;
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
        
        doUpdateDisplayOrientation();

        	Log.v(TAG, "camera.setPreviewDisplay1");
        	
        	surfaceHolder.addCallback(callback);
        	
        	post(new Runnable() {
				@Override
				public void run() {
			        try {
			        	Log.v(TAG, "camera.setPreviewDisplay2 " + surfaceHolder.getSurface().isValid());
			            camera.setPreviewDisplay(surfaceHolder);
			            Log.v(TAG, "camera.setPreviewDisplay DONE");
			        } catch (IOException e) {
			            Log.e(TAG, "Unable to attach preview to camera!", e);
			        }

			        camera.startPreview();
			        recording = true;

			        onDone.run();
				}
        	});        
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
        	Log.d(TAG, "onResume called !!!");
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
    
	private static final int[] ROTATIONS = {
			android.view.Surface.ROTATION_0,
			android.view.Surface.ROTATION_90,
			android.view.Surface.ROTATION_180,
			android.view.Surface.ROTATION_270
	};

    void onSurfaceChanged(int format, int w, int h) {
    	if (camera != null) {
	    	doUpdateDisplayOrientation();
	    	return;
    	}    	

       	// Now let's start camera again if needed
//        if (paused) {
//        	Log.d(TAG, "camera.startPreview");
//        	camera.startPreview();
//        	paused = false;
//        }    	
    }

	private void doUpdateDisplayOrientation() {
		Activity context = (Activity) getContext();
		int ori = context.getWindowManager().getDefaultDisplay().getRotation();
		for (int i = 0; i < ROTATIONS.length; i++) {
			if (ROTATIONS[i] == ori) {
				Camera.CameraInfo info = new Camera.CameraInfo();  
				Camera.getCameraInfo(cameraId, info);

				int degrees = i*90;
				Facing facing = Facing.fromCameraInfo(info);
				Log.d(TAG, "onSurfaceChanged " + 
						facing + " " + 
						"ORI:" + degrees + " " + ori + " " + 
						"ORIENT:" + info.orientation);

				if (facing.isFlipping()) {
					camera.setDisplayOrientation((360 + 360 - degrees - info.orientation) % 360);
				} else {
					camera.setDisplayOrientation((360 + info.orientation -degrees) % 360);
				}
				

				break;
			}
		}
	}
    
    void onSurfaceDestroyed() {
    	Log.d(TAG, "onSurfaceDestroyed");
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
        
        Log.d(TAG, "setCameraParameters:" + currentSize);
    }

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
