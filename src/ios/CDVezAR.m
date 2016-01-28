/*
 * CDVezAR.m
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
 
#import "CDVezAR.h"
#import "CDVezARCameraViewController.h"

NSString *const EZAR_ERROR_DOMAIN = @"EZAR_ERROR_DOMAIN";

static NSString* toBase64(NSData* data) {
    SEL s1 = NSSelectorFromString(@"cdv_base64EncodedString");
    SEL s2 = NSSelectorFromString(@"base64EncodedString");
    SEL realSel = [data respondsToSelector:s1] ? s1 : s2;
    NSString* (*func)(id, SEL) = (void *)[data methodForSelector:realSel];
    return func(data, realSel);
}

@implementation CDVezAR
{
    CDVezARCameraViewController* camController;
    AVCaptureSession *captureSession;
    AVCaptureDevice  *backVideoDevice, *frontVideoDevice, *videoDevice;
    AVCaptureDeviceInput *backVideoDeviceInput, *frontVideoDeviceInput, *videoDeviceInput;
    AVCaptureStillImageOutput *stillImageOutput;
    UIColor *bgColor;
}


// INIT PLUGIN - does nothing atm
- (void) pluginInitialize
{
    [super pluginInitialize];
}

// SETUP EZAR 
// Create camera view and preview, make webview transparent.
// return camera, light features and display details
// 
- (void)init:(CDVInvokedUrlCommand*)command
{
    //cache original webview background color for restoring later
    bgColor = self.webView.backgroundColor;
    
    //set main view background to black; otherwise white area appears during rotation
    self.viewController.view.backgroundColor = [UIColor blackColor];
 
    
    // SETUP CAPTURE SESSION -----
    NSLog(@"Setting up capture session");
    captureSession = [[AVCaptureSession alloc] init];
    
    NSError *error;
    NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for (AVCaptureDevice *device in devices) {
        if (error) break;
        if ([device position] == AVCaptureDevicePositionBack) {
            backVideoDevice = device;
            backVideoDeviceInput =
                [AVCaptureDeviceInput deviceInputWithDevice:backVideoDevice error: &error];
        } else if ([device position] == AVCaptureDevicePositionFront) {
            frontVideoDevice = device;
            frontVideoDeviceInput=
                [AVCaptureDeviceInput deviceInputWithDevice:frontVideoDevice error:&error];
        }
    }
    
    if (error) {
        NSDictionary* errorResult = [self makeErrorResult: 1 withError: error];
        
        CDVPluginResult* pluginResult =
          [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                        messageAsDictionary: errorResult];
        
        return  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    
    //SETUP CameraViewController
    camController =[[CDVezARCameraViewController alloc]
                    initWithController: (CDVViewController*)self.viewController
                    session: captureSession];
    camController.view;
    
    //MAKE WEBVIEW TRANSPARENT
    self.webView.opaque = NO;
    [self forceWebViewRedraw];
    
    //ACCESS DEVICE INFO: CAMERAS, ...
    NSDictionary* deviceInfoResult = [self basicGetDeviceInfo];
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: deviceInfoResult];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
// Return device Camera details
//
- (void)getCameras:(CDVInvokedUrlCommand*)command
{
    NSDictionary* cameras = [self basicGetCameras];
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: cameras];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
// Set camera as the default 
//
- (void)activateCamera:(CDVInvokedUrlCommand*)command
{
    NSString* cameraPos = [command.arguments objectAtIndex:0];

    NSNumber* zoomArg = [command.arguments objectAtIndex:1];
    double zoomLevel = [zoomArg doubleValue];

    NSNumber* lightArg = [command.arguments objectAtIndex:2];
    int lightLevel = (int)[lightArg integerValue];
    
    //todo add error handling
    NSError *error;
    [self basicActivateCamera: cameraPos zoom: zoomLevel light: lightLevel error: error];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void)deactivateCamera:(CDVInvokedUrlCommand*)command
{
    [self basicDeactivateCamera];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void)startCamera:(CDVInvokedUrlCommand*)command
{
    NSString* cameraPos = [command.arguments objectAtIndex:0];
    
    NSNumber* zoomArg = [command.arguments objectAtIndex:1];
    double zoomLevel = [zoomArg doubleValue];
    
    NSNumber* lightArg = [command.arguments objectAtIndex:2];
    int lightLevel = (int)[lightArg integerValue];
    
    [self basicDeactivateCamera]; //stops camera if running before deactivation
    
    NSError *error;
    [self basicActivateCamera: cameraPos zoom: zoomLevel light: lightLevel error: error];
    
    if (error) {
        
    }

    //SET WEBVIEW TRANSPARENT BACKGROUND
    self.webView.backgroundColor = [UIColor clearColor ];
   
    
    //START THE CAPTURE SESSION
    [captureSession startRunning];
    
     CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void)stopCamera:(CDVInvokedUrlCommand*)command
{
    [self basicStopCamera];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void) maxZoom:(CDVInvokedUrlCommand*)command
{
    CGFloat result = videoDeviceInput.device.activeFormat.videoZoomFactorUpscaleThreshold;
 
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDouble: result ];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void) getZoom:(CDVInvokedUrlCommand*)command
{
    double zoomLevel = videoDevice.videoZoomFactor;
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDouble: zoomLevel ];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void) setZoom:(CDVInvokedUrlCommand*)command
{
    NSNumber* zoomArg = [command.arguments objectAtIndex:0];
    double zoomLevel = [zoomArg doubleValue];
    
    [self basicSetZoom: zoomLevel];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void) getLight:(CDVInvokedUrlCommand*)command
{
    NSInteger lightLevel = -1;
    
    if ([videoDevice hasTorch]) {
        lightLevel = videoDevice.torchMode;
    }
    
    //int x = lightLevel;
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt: (int)lightLevel];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void) setLight:(CDVInvokedUrlCommand*)command
{
    NSNumber* lightArg = [command.arguments objectAtIndex:0];
    int lightLevel = (int)[lightArg integerValue];
    [self basicSetLight: lightLevel];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void) snapshot:(CDVInvokedUrlCommand*)command
{
    EZAR_IMAGE_ENCODING encodingType = [[command argumentAtIndex:0 withDefault:@(EZAR_IMAGE_ENCODING_JPG)] unsignedIntegerValue];
    BOOL saveToPhotoAlbum = [[command argumentAtIndex:1 withDefault:@(NO)] boolValue];
    
    //
    AVCaptureConnection *videoConnection = nil;
    for (AVCaptureConnection *connection in stillImageOutput.connections) {
        for (AVCaptureInputPort *port in [connection inputPorts]) {
            if ([[port mediaType] isEqual:AVMediaTypeVideo] ) {
                videoConnection = connection;
                break;
            }
        }
        if (videoConnection) { break; }
    }
    
    //workaround for xxx
    //Capture video frame as image. The image will not include the webview content.
    //Temporarily set the cameraView image to the video frame (a jpg) long enough
    //to capture the entire view hierarcy as an image.
    //
    //NSLog(@"about to request a capture from: %@", stillImageOutput);
    [stillImageOutput captureStillImageAsynchronouslyFromConnection: videoConnection
                       completionHandler: ^(CMSampleBufferRef imageBuffer, NSError *error) {
                                                      
        if (error) {
            NSDictionary* errorResult = [self makeErrorResult: 1 withError: error];
                                                          
            CDVPluginResult* pluginResult =
                [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                messageAsDictionary: errorResult];
                                                          
            return  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
                                                      
                                                      
        NSData *imageData = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageBuffer];
        UIImage *cameraImage = [[UIImage alloc] initWithData:imageData];
                           
        //rotate image to match device orientation
        UIDeviceOrientation devOrient = [[UIDevice currentDevice] orientation];
        /*
         switch (devOrient) {
            case UIDeviceOrientationLandscapeLeft:
                cameraImage = [UIImage imageWithCGImage: [cameraImage CGImage] scale:1.0 orientation:UIImageOrientationUp];
                break;
            case UIDeviceOrientationPortraitUpsideDown:
                cameraImage = [UIImage imageWithCGImage: [cameraImage CGImage] scale:1.0 orientation:UIImageOrientationLeft];
                break;
            case UIDeviceOrientationLandscapeRight:
                cameraImage = [UIImage imageWithCGImage: [cameraImage CGImage] scale:1.0 orientation:UIImageOrientationDown];
                break;
            default:
                //portrait orient; do nothing
                break;
        }
         */
        
        switch (camController.interfaceOrientation) {
            case UIInterfaceOrientationPortrait:
                cameraImage = [UIImage imageWithCGImage: [cameraImage CGImage] scale:1.0 orientation:UIImageOrientationRight];
                break;
            case UIInterfaceOrientationLandscapeLeft:
                cameraImage = [UIImage imageWithCGImage: [cameraImage CGImage] scale:1.0 orientation:UIImageOrientationDown];
                break;
            case UIInterfaceOrientationPortraitUpsideDown:
                cameraImage = [UIImage imageWithCGImage: [cameraImage CGImage] scale:1.0 orientation:UIImageOrientationLeft];
                break;
            case UIInterfaceOrientationLandscapeRight:
                cameraImage = [UIImage imageWithCGImage: [cameraImage CGImage] scale:1.0 orientation:UIImageOrientationUp];
                break;
            
        }
        
                           
        //assign the video frame image to the cameraView image
        ((UIImageView *)camController.view).image = cameraImage;
        ((UIImageView *)camController.view).contentMode = UIViewContentModeScaleAspectFill;
                           
        //capture the entire view hierarchy
        UIView *view = self.viewController.view;
        UIGraphicsBeginImageContextWithOptions(view.bounds.size, YES, 0);
        [view drawViewHierarchyInRect: view.bounds afterScreenUpdates: YES];
        UIImage* screenshot = UIGraphicsGetImageFromCurrentImageContext();
        UIGraphicsEndImageContext();
                                                      
        //clear camera view image
        ((UIImageView *)camController.view).image = nil;
                                                      
        if (saveToPhotoAlbum) { //save image to gallery
            //todo: handling error saving to photo gallery
            UIImageWriteToSavedPhotosAlbum(screenshot, nil, nil, nil);
        }
                                                      
        //format image for return
        NSData *screenshotData = nil;
        if (encodingType == EZAR_IMAGE_ENCODING_JPG) {
            screenshotData = UIImageJPEGRepresentation(screenshot, 1.0);
        } else {
            screenshotData = UIImagePNGRepresentation(screenshot);
        }
                                                      
        CDVPluginResult* pluginResult =
            [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:toBase64(screenshotData)];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}


//---------------------------------------------------------------
//
- (NSDictionary*)basicGetDeviceInfo
{
    NSMutableDictionary* deviceInfo = 
    	[NSMutableDictionary dictionaryWithDictionary: [self basicGetCameras]];

    CGRect screenRect = [[UIScreen mainScreen] bounds];
    CGFloat screenWidth = screenRect.size.width;
    CGFloat screenHeight = screenRect.size.height;

    [deviceInfo setObject: @(screenWidth) forKey:@"displayWidth"];
    [deviceInfo setObject: @(screenHeight) forKey:@"displayHeight"];

    return deviceInfo;
}


//
//
//
- (NSDictionary*)basicGetCameras
{
    NSMutableDictionary* cameraInfo = [NSMutableDictionary dictionaryWithCapacity:4];

    NSArray *cameras = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for (AVCaptureDevice *camera in cameras) {
        if ([camera position] == AVCaptureDevicePositionFront) {
            [cameraInfo setObject: [self basicGetCameraProps: camera]  forKey:@"FRONT"];
        } else if ([camera position] == AVCaptureDevicePositionBack) {
            [cameraInfo setObject: [self basicGetCameraProps: camera]  forKey:@"BACK"];
        }
    }

    return cameraInfo;
}


//
//
//
- (NSDictionary*)basicGetCameraProps: (AVCaptureDevice *)camera
{
    NSMutableDictionary* cameraProps = [NSMutableDictionary dictionaryWithCapacity:4];
    [cameraProps setObject: camera.uniqueID forKey:@"id"];
    
    [cameraProps setObject: @(camera.activeFormat.videoZoomFactorUpscaleThreshold) forKey:@"maxZoom"];
    [cameraProps setObject: @(camera.videoZoomFactor) forKey:@"zoom"];
    
    [cameraProps setObject: @([camera hasTorch]) forKey:@"light"];
    if ([camera hasTorch]) {
        [cameraProps setObject: @(camera.torchLevel) forKey:@"lightLevel"];
    }
    
    if ([camera position] == AVCaptureDevicePositionFront) {
        [cameraProps setObject: @"FRONT" forKey:@"position"];
    } else if ([camera position] == AVCaptureDevicePositionBack) {
        [cameraProps setObject: @"BACK" forKey:@"position"];
    }
    return cameraProps;
}


//
//
//
- (void)basicActivateCamera: (NSString*)cameraPos zoom: (double)zoomLevel light: (int)lightLevel error: (NSError*) error
{
    videoDevice = nil;
    videoDeviceInput = nil;
    
    if ([cameraPos caseInsensitiveCompare: @"FRONT"] == NSOrderedSame) {
        videoDevice = frontVideoDevice;
        videoDeviceInput = frontVideoDeviceInput;
    } else  if ([cameraPos caseInsensitiveCompare: @"BACK"] == NSOrderedSame) {
        videoDevice = backVideoDevice;
        videoDeviceInput = backVideoDeviceInput;
    }
    
    if (!videoDevice) {
        error = [NSError errorWithDomain: EZAR_ERROR_DOMAIN
                                code: EZAR_ERROR_CODE_INVALID_ARGUMENT
                                userInfo: @{@"description": @"No camera found"}];
        return;
    }
   
    if ([captureSession canAddInput:videoDeviceInput]) {
        
        [captureSession addInput:videoDeviceInput];
            
        if ([videoDevice lockForConfiguration: &error]) {
                
            //configure focus
            if ([videoDevice isFocusModeSupported: AVCaptureFocusModeContinuousAutoFocus]) {
                videoDevice.focusMode = AVCaptureFocusModeContinuousAutoFocus;
            }
                
            if ([videoDevice isExposureModeSupported: AVCaptureExposureModeContinuousAutoExposure]) {
                videoDevice.exposureMode = AVCaptureExposureModeContinuousAutoExposure;
            } else if ([videoDevice isExposureModeSupported: AVCaptureExposureModeAutoExpose]) {
                videoDevice.exposureMode = AVCaptureExposureModeAutoExpose;
            }
                
            [self basicSetZoom: zoomLevel];
            [self basicSetLight: lightLevel];
                
            [videoDevice unlockForConfiguration];
        }
        
        //configure to capture a video frame
        stillImageOutput = [[AVCaptureStillImageOutput alloc] init];
        NSDictionary *outputSettings = [[NSDictionary alloc] initWithObjectsAndKeys: AVVideoCodecJPEG, AVVideoCodecKey, nil];
        [stillImageOutput setOutputSettings:outputSettings];
        [captureSession addOutput: stillImageOutput];
        
        
    } else
    {
        error = [NSError errorWithDomain: EZAR_ERROR_DOMAIN
                                code: EZAR_ERROR_CODE_ACTIVATION
                                userInfo: @{@"description": @"Unable to activate camera"}];
    }
                 
}


//
//
//
- (void)basicDeactivateCamera
{
    //stop the session
    //remove the current video device from the session
    [self basicStopCamera];
    
    [captureSession removeInput: videoDeviceInput];
    
    videoDevice = nil;
    videoDeviceInput = nil;
    
}


//
//
//
- (void)basicStopCamera
{
    if (captureSession && [captureSession isRunning]) {
        //----- STOP THE CAPTURE SESSION RUNNING -----
        [captureSession stopRunning];
        self.webView.backgroundColor = bgColor;
    }
}


//
//
//
- (void) basicSetZoom:(double) zoomLevel
{
    if ([videoDevice lockForConfiguration:nil]) {
        [videoDevice setVideoZoomFactor:zoomLevel];
        [videoDevice unlockForConfiguration];
    }
}


//
//
//
- (void) basicSetLight: (int)lightLevel
{
    if (![videoDevice hasTorch]) return;
    
    if ([videoDevice lockForConfiguration:nil]) {
        [videoDevice setTorchMode: lightLevel];
        [videoDevice unlockForConfiguration];
    }
}

//
//
//
- (NSDictionary*)makeErrorResult: (EZAR_ERROR_CODE) errorCode withData: (NSString*) description
{
    NSMutableDictionary* errorData = [NSMutableDictionary dictionaryWithCapacity:4];
    
    [errorData setObject: @(errorCode)  forKey:@"code"];
    [errorData setObject: @{ @"description": description}  forKey:@"data"];
    
    return errorData;
}

//
//
//
- (NSDictionary*)makeErrorResult: (EZAR_ERROR_CODE) errorCode withError: (NSError*) error
{
    NSMutableDictionary* errorData = [NSMutableDictionary dictionaryWithCapacity:2];
    [errorData setObject: @(errorCode)  forKey:@"code"];
    
     NSMutableDictionary* data = [NSMutableDictionary dictionaryWithCapacity:2];
    [data setObject: [error.userInfo objectForKey: NSLocalizedFailureReasonErrorKey] forKey:@"description"];
    [data setObject: @(error.code) forKey:@"iosErrorCode"];
    
    [errorData setObject: data  forKey:@"data"];
    
    return errorData;
}

//warning! total hack - setting transparency in web doc does not immediately take effect.
//The hack is to toggle <body> display to none and then to block causes full repaint.
//  
- (void)forceWebViewRedraw 
{
    NSString *jsstring =
        @"document.body.style.display='none';"
         "setTimeout(function(){document.body.style.display='block'},10);";
    
    //todo provide compatibility if this is a wkwebview component
	[(UIWebView*)self.webView stringByEvaluatingJavaScriptFromString: jsstring];
}



@end
