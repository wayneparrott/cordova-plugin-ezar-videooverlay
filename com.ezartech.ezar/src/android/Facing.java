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
