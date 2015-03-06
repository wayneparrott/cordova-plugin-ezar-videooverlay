package com.ezartech.ezar;

import android.hardware.Camera;

public enum Facing {
	FRONT {
		public int getCameraInfoFacing() {
			return Camera.CameraInfo.CAMERA_FACING_FRONT;
		}		
	},
	BACK {
		public int getCameraInfoFacing() {
			return Camera.CameraInfo.CAMERA_FACING_BACK;
		}
	}
	;

	public abstract int getCameraInfoFacing();
}
