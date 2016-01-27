#ezAR Startup Kit Cordova Plugin
Develop augmented reality applications using the ezAR Startup Kit. When initialized ezAR creates and positions a camera view below the Cordova WebView component. ezAR provides access to the back and front device cameras. When a camera is started its output is displayed on the ezAR camera view. By default the camera view is not visible as it is masked off by the WebView's HTML content. Increase the transparency for the area(s) of the app UI where you wish to see the camera view.

##Supported Platforms
- iOS 7, 8 & 9
- Android 4.2 and greater 

##Getting Started
Add the ezAR plugin to your Corodva project the Cordova CLI

        cordova plugin add pathtoezar/com.ezartech.ezar

Next in your Cordova JavaScript deviceready handler include the following JavaScript snippet to initialize ezAR and activate the camera on the back of the device.

        ezar.initialize(
            function() {
                ezar.getBackCamera().start();
                },
            function(err) {
                alert('unable to init ezar: ' + err);
        });
                    
##Additional Documentation        
See [ezartech.com](http://ezartech.com) for documentation and support.

##License
The ezAR Startup Kit is licensed under a [modified MIT license](http://www.ezartech.com/ezarstartupkit-license).


Copyright (c) 2015-2016, ezAR Technologies


