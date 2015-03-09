package com.ezartech.ezar;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class VideoOverlay extends ViewGroup {
    private static final String TAG = "BACKGROUND_VID_OVERLAY";
    private final PreviewView preview;
    private Camera camera = null;
    private Camera.Size currentSize;
    private boolean recording = false;

    public VideoOverlay(Context context) {
        super(context);
        this.preview = new PreviewView(this);
        addView(preview.getView());
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

    public void startRecording(String facing, double zoom, double light) {
        if (isRecording()) {
            try {
				stopRecording();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }

        final int cameraFacing = Facing.valueOf(facing).getCameraInfoFacing();
        
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

        if(currentSize == null){
            setCameraParameters(camera, cameraParameters);
        }

        try {
            preview.attach(camera);
        } catch (IOException e) {
            Log.e(TAG, "Unable to attach preview to camera!", e);
        }

        camera.startPreview();
        recording = true;

    }

    public void stopRecording() throws IOException {
        Log.d(TAG, "stopRecording called");

        camera.stopPreview();
        camera.release();

        recording = false;
    }

    void previewAvailable() {
        
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
        addView(preview.getView());
    }

    public void onPause() {
        try {
            Log.d(TAG, "onPause called");
            stopRecording();

            Log.d(TAG, "removing View");
            if(preview != null) {
                removeView(preview.getView());
            }

        } catch (IOException e) {
            Log.e(TAG, "Error in OnPause - Could not stop camera", e);
        }
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy called");

        onPause();
    }

	public void setZoom(int doubleOrNull) {
		Parameters parameters = camera.getParameters();
		parameters.setZoom(doubleOrNull);
		camera.setParameters(parameters);
	}

	public void setLight(int intOrNull) {
		Parameters parameters = camera.getParameters();
		parameters.setFlashMode(intOrNull == 1 ? 
				Parameters.FLASH_MODE_TORCH :
				Parameters.FLASH_MODE_OFF);
		camera.setParameters(parameters);
	}
}
