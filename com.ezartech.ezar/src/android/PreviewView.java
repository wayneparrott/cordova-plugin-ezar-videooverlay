package com.ezartech.ezar;

import java.io.IOException;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class PreviewView implements SurfaceHolder.Callback {
    private static final String TAG = "BACKGROUND_VID_SURFACE";
    private final VideoOverlay overlay;
    private final SurfaceView surfaceView;
    private final SurfaceHolder surfaceHolder;

    @SuppressWarnings("deprecation")
    public PreviewView(VideoOverlay overlay) {
        Log.d(TAG, "Creating Surface Preview");
        this.overlay = overlay;

        surfaceView = new SurfaceView(overlay.getContext());
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Surface Created");
        overlay.previewAvailable();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed called");
    }

    public void attach(Camera camera) throws IOException {
        camera.setPreviewDisplay(surfaceHolder);
    }

    public View getView() {
        return surfaceView;
    }
}
