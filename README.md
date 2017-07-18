# ezAR VideoOverlay Cordova Plugin  
This plugin overlays the Cordova WebView on top of a custom camera view 
which provides a video preview. Using the VideoOverlay JavaScript api you 
can switch between the front and back cameras of a device, start and stop 
video preview from a camera and adjust the zoom level of the camera. 

When a camera is started its video output is displayed on the ezAR camera 
view. By default the camera view is not visible as it is hidden below the 
WebView's HTML content. Increase the transparency for the area(s) of the 
app UI (html) where you wish to see the view preview from the camera. 
The Cordova WebView control has the same dimensions as the camera view. 
In some cases the camera view and WebView sizes maybe smaller than the 
device display due to automatic scaling performed by VideoOverlay to 
maintain a consistent aspect ratio of the video stream. 

When a camera is started it is known as the active camera and it's video 
is rendered on the camera view.  At this time the video content is NOT 
saved to the device's photo gallery. 

## Supported Platforms
- iOS 7, 8 & 9
- Android 4.2 and greater 

## Getting Started
The simplest ezAR application involves adding the VideoOverlay plugin 
to your Corodva project using the Cordova CLI

        cordova plugin add cordova-plugin-ezar-video-overlay

Next in your Cordova JavaScript deviceready handler include the following 
JavaScript snippet to initialize the VideoOverlay plugin and activate the 
camera on the back of the device, i.e., the camera away from the display.

        ezar.initializeVideoOverlay(
            function() {
                ezar.getBackCamera().start();
                },
            function(err) {
                alert('unable to init ezar: ' + err);
        });
                    
## Additional Documentation        
See [ezartech.com](http://ezartech.com) for documentation and support.

## License
The ezAR Startup Kit is licensed under a [modified MIT license](http://www.ezartech.com/ezarstartupkit-license).


Copyright (c) 2015-2017, ezAR Technologies
