#ezAR VideoOverlay Plugin Release Notes

##0.2.4 (20160606)
Changes:
1. Added backgroundColor (#RRGGBB) property to initializeVideoOverlay options parameter. Set the background color visible when a camera is not running. This background color is 
visible only when the HTML <body> background color is transperent.

##0.2.3 (20160503)
Changes:
1. Fixed render error on Galaxy S4. Removed unused setting of android picture size.

##0.2.2 (20160310)
Changes:
1. Fixed video orientation to rotate consistently on device rotation.

##0.2.1 (20160302-1)
Minor changes:
1. Fixed plugin name in plugin.xml

##0.2.0 (20160302-1)
This update is refactor of the previous super ezar plugin to only include functionality  
for setting up and controlling the camera view and the device cameras.  The Android  
implementation has been completely rewritten and fixes preview aspect ratio issue  
from beta-1.  

This plugin works with or without the Flashlight and Snapshot plugins.

###Known Issues
1. Plugin error reporting needs improvement.


##0.1.0 beta-1
This is the first public release of the ezAR Startup Kit.

##Known Issues
1. Camera view does not consistently maintain aspect ratio on device rotation
2. Plugin error reporting needs improvement.
