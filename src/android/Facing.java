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

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;

public enum Facing {
	BACK {
		public int getCameraInfoFacing() {
			return Camera.CameraInfo.CAMERA_FACING_BACK;
		}
		
		public boolean isFlipping() {
			return false;
		}
	},
	FRONT {
		public int getCameraInfoFacing() {
			return Camera.CameraInfo.CAMERA_FACING_FRONT;
		}		
		
		public boolean isFlipping() {
			return true;
		}
	}
	;

	public abstract int getCameraInfoFacing();
	public abstract boolean isFlipping();

	public static Facing fromCameraInfo(CameraInfo info) {
		return values()[info.facing];
	}
}
