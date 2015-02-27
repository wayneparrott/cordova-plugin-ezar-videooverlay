// CDVezARCameraViewController.m
//
// Copyright 2015, ezAR Technologies
// All rights reserved.
//

#include "CDVezARCameraViewController.h"

@implementation CDVezARCameraViewController
{
    CDVViewController* _mainController;
    AVCaptureSession* _captureSession;
    AVCaptureVideoPreviewLayer *_previewLayer;
}

-(CDVezARCameraViewController*) initWithController: (CDVViewController*) mainViewController
                                           session:(AVCaptureSession*) captureSession;
{
    //[super init];
    _mainController = mainViewController;
    _captureSession = captureSession;
    
    return self;
}

-(void)loadView
{
    //ADD VIDEO PREVIEW LAYER
    _previewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession: _captureSession] ;
    [_previewLayer setVideoGravity:AVLayerVideoGravityResizeAspectFill];
    _previewLayer.frame = _mainController.view.frame;
    
    UIView *cameraView = [[UIView alloc] init];
    [[cameraView layer] addSublayer: _previewLayer];
    cameraView.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    cameraView.backgroundColor = [UIColor blackColor];
    
    [_mainController addChildViewController: self];
    [self didMoveToParentViewController: _mainController];
    
    //POSITION cameraview below the webview
    [_mainController.view insertSubview: cameraView belowSubview: _mainController.webView];
    
    self.view = cameraView;
    [self updatePreviewOrientation: [self getUIInterfaceOrientation]];
}

- (void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear: animated];
    [self updatePreviewOrientation: [self getUIInterfaceOrientation]];
}

- (void)viewWillLayoutSubviews
{
    CGRect webViewFrame = _mainController.webView.frame;
   _previewLayer.frame = webViewFrame;
    
    //hack: FORCE webview to redraw by resizing
    CGRect webviewFramePrime =
        CGRectMake(webViewFrame.origin.x, webViewFrame.origin.y, webViewFrame.size.width, webViewFrame.size.height-1);
    _mainController.webView.frame = webviewFramePrime;
    _mainController.webView.frame = webViewFrame;
}

- (void)viewDidLayoutSubviews
{
}

- (void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation
                        duration:(NSTimeInterval)duration {

    //[super willAnimateRotationToInterfaceOrientation:<#toInterfaceOrientation#> duration:<#duration#>];
    [self updatePreviewOrientation: toInterfaceOrientation];
    
}

-(void)updatePreviewOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    AVCaptureVideoOrientation orient = [self videoOrientationFromUIInterfaceOrientation: interfaceOrientation];
    
    BOOL shouldRotate = [_mainController shouldAutorotateToInterfaceOrientation: interfaceOrientation];
   
    if (shouldRotate) {
        // set the orientation of preview layer
        AVCaptureVideoOrientation orient = [self videoOrientationFromUIInterfaceOrientation: interfaceOrientation];
        [_previewLayer.connection setVideoOrientation: orient];
    }
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
@end