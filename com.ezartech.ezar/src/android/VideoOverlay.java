package com.ezartech.ezar;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class VideoOverlay extends ViewGroup {
    private static final String TAG = "BACKGROUND_VID_OVERLAY";
    private final PreviewView preview;
    private Camera camera = null;
    private Camera.Size currentSize;
    private boolean recording = false;
    private int cameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    private boolean inPreview = false;
    private boolean viewIsAttached = false;

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

    public void startRecording() {
        if (isRecording()) {
            Log.d(TAG, "Already Recording!");
            return;
        }

        initCamera();

        if (camera == null) {
            throw new NullPointerException("Cannot start recording, we don't have a camera!");
        }

        Camera.Parameters cameraParameters = camera.getParameters();

        if(currentSize == null){
            setCameraParameters(camera, cameraParameters);
        }

        camera.startPreview();
        recording = true;

    }

    public void stopRecording() throws IOException {
        Log.d(TAG, "stopRecording called");

        camera.stopPreview();

        recording = false;
    }

    private void initCamera(){
        if (camera == null) {
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
                    return;
                }
            }
            
            camera = Camera.open(mNumberOfCameras - 1);
        }
    }

    void previewAvailable(){
        viewIsAttached = true;
        initCamera();
        if(camera != null) {
            try {
                preview.attach(camera);
            } catch (IOException e) {
                Log.e(TAG, "Unable to attach preview to camera!", e);
            }
        }
    }

    public void initPreview(int height, int width) {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();

            setCameraParameters(camera, parameters);

            camera.startPreview();
            inPreview = true;
        }
    }

    private void setCameraParameters(Camera camera, Camera.Parameters parameters){
        Camera.Size size = parameters.getSupportedPreviewSizes().get(0);
		
		if (size == null) {
		    size = parameters.getSupportedPreviewSizes().get(0);
		}
		
		currentSize = size;
        parameters.setPreviewSize(currentSize.width, currentSize.height);

        // parameters.setRotation(90);

        camera.setParameters(parameters);
        camera.setDisplayOrientation(90);
    }

    public void startPreview(boolean startRecording){
        if(!inPreview) {
            if(preview != null && !viewIsAttached ) {
                preview.startRecordingWhenAvailable(startRecording);
                addView(preview.getView());
            } else {
                previewAvailable();
                initPreview(getHeight(), getWidth());
                if (startRecording)
                    startRecording();
                inPreview = true;
            }
        }
    }

    public void stopPreview() {
        Log.d(TAG, "stopPreview called");
        if (inPreview && camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
        }

        if(camera != null) {
            camera.lock();
            camera.release();
            camera = null;
        }

        inPreview = false;
    }

    public void setCameraFacing(String cameraFace) {
        cameraFacing = ( cameraFace.equalsIgnoreCase("FRONT") ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK );
    }

    public void onResume() {
        addView(preview.getView());

        viewIsAttached = true;
    }

    public void onPause() {
        try {
            Log.d(TAG, "onPause called");
            stopRecording();
            stopPreview();
            preview.startRecordingWhenAvailable(false);
            Log.d(TAG, "removing View");
            if(preview != null)
                removeView(preview.getView());
            viewIsAttached = false;
        } catch (IOException e) {
            Log.e(TAG, "Error in OnPause - Could not stop camera", e);
        }
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy called");

        onPause();
    }
}
