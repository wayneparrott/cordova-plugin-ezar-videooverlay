/*
 * CDVezARCameraViewController.h
 * Copyright 2015, ezAR Technologies, ezartech.com
 * All rights reserved.
 */

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import "Cordova/CDV.h"
#import "Cordova/CDVViewController.h"


@interface CDVezARCameraViewController : UIViewController

-(CDVezARCameraViewController*) initWithController: (CDVViewController*) mainViewController
                                           session:(AVCaptureSession*) captureSession;

@end
