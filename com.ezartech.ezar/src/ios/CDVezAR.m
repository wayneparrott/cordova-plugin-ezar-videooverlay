// CDVezAR.m
//
// Copyright 2015, ezAR Technologies
// All rights reserved.
//
 
#import "Cordova/CDV.h"
#import <AVFoundation/AVFoundation.h>
#import "CDVezAR.h"

NSString *const EZAR_ERROR_DOMAIN = @"EZAR_ERROR_DOMAIN";

@implementation CDVezAR
{
    AVCaptureSession *captureSession;
    AVCaptureDevice  *backVideoDevice, *frontVideoDevice, *videoDevice;
    AVCaptureDeviceInput *backVideoDeviceInput, *frontVideoDeviceInput, *videoDeviceInput;
    UIColor *bgColor;
    AVCaptureVideoPreviewLayer *previewLayer;
}


// INIT PLUGIN - Register for orientation changes
- (void) pluginInitialize
{
    [super pluginInitialize];
    
    UIDevice *device = [UIDevice currentDevice];
    NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];//Get the notification centre for the app
    [nc addObserver:self											//Add ezar as an observer
           selector:@selector(orientationChanged:)
           name:UIDeviceOrientationDidChangeNotification
           object:device];
}


//DEVICE ORIENTATION CHANGE HANDLER
- (void)orientationChanged:(NSNotification *)note
{
    [self updatePreviewOrientation];
}


// SETUP EZAR 
// create video-preview under webview and 
// make webview transparent
// return camera, light features and display details
// 
- (void)init:(CDVInvokedUrlCommand*)command
{
    //set main view background to black; otherwise white area appears during rotation
    self.viewController.view.backgroundColor = [UIColor blackColor];
 
 	//cache original webview background color for restoring later
    bgColor = self.webView.backgroundColor;
    
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
    
    //ADD VIDEO PREVIEW LAYER
    previewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession:captureSession] ;
    [previewLayer setVideoGravity:AVLayerVideoGravityResizeAspectFill];
    previewLayer.frame = self.webView.frame;
    
     UIView *cameraView = [[UIView alloc] init];
    [[cameraView layer] addSublayer: previewLayer];
    cameraView.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    cameraView.frame = previewLayer.frame;
    
    //POSITION cameraview below the webview
    [[self.webView superview] insertSubview:cameraView belowSubview: self.webView];
   
   
    //MAKE WEBVIEW TRANSPARENT
    self.webView.opaque = NO;
    
    //ACCESS DEVICE INFO: CAMERAS, ...
    NSDictionary* deviceInfoResult = [self getDeviceInfo];
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: deviceInfoResult];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
// Return device Camera details
//
- (void)getCameras:(CDVInvokedUrlCommand*)command
{
    NSDictionary* cameras = [self getCameras];
    
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

    self.webView.backgroundColor = [UIColor clearColor ];
    
    //----- START THE CAPTURE SESSION RUNNING -----
    [captureSession startRunning];
     
    [self updatePreviewOrientation];
     
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
- (void) screenshot:(CDVInvokedUrlCommand*)command
{
    UIWindow *topWindow = [[[UIApplication sharedApplication].windows sortedArrayUsingComparator:^NSComparisonResult(UIWindow *win1, UIWindow *win2) {
        return win1.windowLevel - win2.windowLevel;
    }] lastObject];
    
    UIView *view = [[topWindow subviews] lastObject];
    //UIView *view = self.webView;
    UIGraphicsBeginImageContextWithOptions(view.bounds.size, NO, 0.0);
    [view drawViewHierarchyInRect: view.bounds afterScreenUpdates: YES];
    UIImage* screenshot = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    UIImageWriteToSavedPhotosAlbum(screenshot, self, @selector(imageSavedToPhotosAlbum: didFinishSavingWithError: contextInfo:), nil);
    
//    UIImageWriteToSavedPhotosAlbum ( UIImage *image, id completionTarget, SEL completionSelector, void *contextInfo );
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void)imageSavedToPhotosAlbum:(UIImage *)image didFinishSavingWithError:(NSError *)error contextInfo:(void *)contextInfo {
    NSString *message;
    NSString *title;
    if (!error) {
        title = NSLocalizedString(@"SaveSuccessTitle", @"");
        message = NSLocalizedString(@"SaveSuccessMessage", @"");
    } else {
        title = NSLocalizedString(@"SaveFailedTitle", @"");
        message = [error description];
    }
    UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title
                                                    message:message
                                                   delegate:nil
                                          cancelButtonTitle:NSLocalizedString(@"ButtonOK", @"")
                                          otherButtonTitles:nil];
    [alert show];
}


//---------------------------------------------------------------
//
- (NSDictionary*)getDeviceInfo
{
    NSMutableDictionary* deviceInfo = [NSMutableDictionary dictionaryWithDictionary: [self getCameras]];

    CGRect screenRect = [[UIScreen mainScreen] bounds];
    CGFloat screenWidth = screenRect.size.width;
    CGFloat screenHeight = screenRect.size.height;

    [deviceInfo setObject: @(screenWidth) forKey:@"displayWidth"];
    [deviceInfo setObject: @(screenHeight) forKey:@"displayHeight"];

    //NSDictionary* result = [NSDictionary dictionaryWithDictionary: deviceInfo];
    //return result;
    
    return deviceInfo;
}


//
//
//
- (NSDictionary*)getCameras
{
    NSMutableDictionary* cameraInfo = [NSMutableDictionary dictionaryWithCapacity:4];

    NSArray *cameras = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for (AVCaptureDevice *camera in cameras) {
        if ([camera position] == AVCaptureDevicePositionFront) {
            [cameraInfo setObject: [self getCameraProps: camera]  forKey:@"FRONT"];
        } else if ([camera position] == AVCaptureDevicePositionBack) {
            [cameraInfo setObject: [self getCameraProps: camera]  forKey:@"BACK"];
        }
    }

    return cameraInfo;
}


//
//
//
- (NSDictionary*)getCameraProps: (AVCaptureDevice *)camera
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
- (void) updatePreviewOrientation
{
    if (previewLayer == nil) return;
    
    UIInterfaceOrientation ifOrient = [self getUIInterfaceOrientation];
    BOOL shouldRotate = [self.viewController shouldAutorotateToInterfaceOrientation: ifOrient];
    if (shouldRotate) {
        // set the orientation of preview layer
        AVCaptureVideoOrientation orient = [self videoOrientationFromUIInterfaceOrientation: ifOrient];
        [previewLayer.connection setVideoOrientation: orient];
       // NSLog(@"Orientation has changed");
    } else {
        //NSLog(@"Orientation NOT changed");
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


//---------------- orientation utilties ----------------
     
- (UIDeviceOrientation) getDeviceOrientation
{
    return [[UIDevice currentDevice] orientation];
}


- (UIInterfaceOrientation)getUIInterfaceOrientation
{
    return [UIApplication sharedApplication].statusBarOrientation;
}


- (AVCaptureVideoOrientation)videoOrientationFromUIInterfaceOrientation: (UIInterfaceOrientation) ifOrientation
{
    AVCaptureVideoOrientation videoOrientation;
    switch (ifOrientation) {
        case UIInterfaceOrientationPortrait:
            videoOrientation = AVCaptureVideoOrientationPortrait;
            break;
        case UIInterfaceOrientationPortraitUpsideDown:
            videoOrientation = AVCaptureVideoOrientationPortraitUpsideDown;
            break;
        case UIInterfaceOrientationLandscapeRight:
            videoOrientation = AVCaptureVideoOrientationLandscapeRight;
            break;
        case UIInterfaceOrientationLandscapeLeft:
            videoOrientation = AVCaptureVideoOrientationLandscapeLeft;
            break;
        default:
            videoOrientation = AVCaptureVideoOrientationPortrait;
    }
    
    return videoOrientation;
}


- (AVCaptureVideoOrientation)videoOrientationFromDeviceOrientation: (UIDeviceOrientation) deviceOrientation
{
    //[[UIApplication sharedApplication] statusBarOrientation];
    
    AVCaptureVideoOrientation videoOrientation;
    
    //UIDeviceOrientation deviceOrientation = [[UIDevice currentDevice] orientation];
    switch (deviceOrientation) {
        case UIDeviceOrientationPortrait:
            videoOrientation = AVCaptureVideoOrientationPortrait;
            break;
        case UIDeviceOrientationPortraitUpsideDown:
            videoOrientation = AVCaptureVideoOrientationPortraitUpsideDown;
            break;
        case UIDeviceOrientationLandscapeLeft:
            // Not clear why but the landscape orientations are reversed
            // if I use AVCaptureVideoOrientationLandscapeRight here the pic ends up upside down
            videoOrientation = AVCaptureVideoOrientationLandscapeRight;
            break;
        case UIDeviceOrientationLandscapeRight:
            // Not clear why but the landscape orientations are reversed
            // if I use AVCaptureVideoOrientationLandscapeRight here the pic ends up upside down
            videoOrientation = AVCaptureVideoOrientationLandscapeLeft;
            break;
        default:
            videoOrientation = AVCaptureVideoOrientationPortrait;
    }
    
    return videoOrientation;
}


- (UIInterfaceOrientation)uiOrientationFromDeviceOrientation: (UIDeviceOrientation) deviceOrientation
{
    UIInterfaceOrientation ifOrientation;
    
    switch (deviceOrientation) {
        case UIDeviceOrientationPortrait:
            ifOrientation = UIInterfaceOrientationPortrait;
            break;
        case UIDeviceOrientationPortraitUpsideDown:
            ifOrientation = UIInterfaceOrientationPortraitUpsideDown;
            break;
        case UIDeviceOrientationLandscapeLeft:
            // Not clear why but the landscape orientations are reversed
            // if I use AVCaptureVideoOrientationLandscapeRight here the pic ends up upside down
            ifOrientation = UIInterfaceOrientationLandscapeRight;
            break;
        case UIDeviceOrientationLandscapeRight:
            // Not clear why but the landscape orientations are reversed
            // if I use AVCaptureVideoOrientationLandscapeRight here the pic ends up upside down
            ifOrientation = UIInterfaceOrientationLandscapeLeft;
            break;
        default:
            ifOrientation = UIInterfaceOrientationPortrait;
    }
    
    return ifOrientation;
}

@end
