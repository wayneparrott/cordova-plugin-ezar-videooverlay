//---------------- orientation utilties ----------------
     
#import <UIKit.h>
#import <AVFoundation.h>

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