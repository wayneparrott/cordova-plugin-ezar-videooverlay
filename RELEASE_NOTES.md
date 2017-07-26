# ezAR VideoOverlay Plugin Release Notes

## 1.0.0 (20170717)
1. Added package.json for Cordova 7 compliance.
2. Renamed to cordova-plugin-ezar-video-overlay in preparation for publishing to npm


## 0.2.11 (20170306)
1. New Camera api, getHorizontalViewAngle() & getVerticalViewAngle()
2. Android - fixed refresh issue when performing camera start(), stop(), start() sequence


## 0.2.10 (20170123)
1. iOS - handle multiple calls to initializeVideoOverlay().
2. Android - added new android specific initializeVideoOverlay option 'fitWebViewToCameraView'.
3. Android - fixed issue where [no camera preview size is selected]https://support.ezartech.com/index.php?p=/discussion/49/videooverlay-ezar-java-selectsizepair-return-null-value
 resulted in NPE.


## 0.2.9 (20160921)
1. iOS - added support for WKWebView.


## 0.2.8 (20160915)
1. iOS - added NSCameraUsageDescription to plist, required for iOS 10.


## 0.2.7 (20160901)
1. Android - added support for the [Crosswalk webview](https://crosswalk-project.org/). 


## 0.2.6 (20160818)
1. Android - rolled back agressive use of hardware acceleration. In some cases users reported slower performance. 
Now uses device default hardware acceleration settings.
2. Android - fixed compile issue on Cordova 5 and earlier versions


## 0.2.5 (20160620)
1. Android - modified strategy for camera resolution selection and UI resizing to reduce letterbox mattes 
(black bars on opposing sides of the UI when camera aspect ratio does not match the display area). This 
change is most noticable when in fullscreen mode, a.k.a., immersive mode.
2. Fixed error in initializeVideoOverlay(). On older OSes the JavaScript String.startsWith() method is not
available.


## 0.2.4 (20160606)
1. Added backgroundColor (#RRGGBB) property to initializeVideoOverlay options parameter. Set the background color visible when a camera is not running. This background color is 
visible only when the HTML <body> background color is transperent.


## 0.2.3 (20160503)
1. Fixed render error on Galaxy S4. Removed unused setting of android picture size.


## 0.2.2 (20160310)
1. Fixed video orientation to rotate consistently on device rotation.


## 0.2.1 (20160302-1)
1. Fixed plugin name in plugin.xml


## 0.2.0 (20160302-1)
This update is refactor of the previous super ezar plugin to only include 
functionality for setting up and controlling the camera view and the 
device cameras. The Android implementation has been completely rewritten 
and fixes preview aspect ratio issue from beta-1.  

This plugin works with or without the Flashlight and Snapshot plugins.

### Known Issues
1. Plugin error reporting needs improvement.


## 0.1.0 beta-1
This is the first public release of the ezAR Startup Kit.

## Known Issues
1. Camera view does not consistently maintain aspect ratio on device rotation
2. Plugin error reporting needs improvement.
