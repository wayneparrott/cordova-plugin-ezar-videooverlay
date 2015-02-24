// CDVezAR.h
//
// Copyright 2015, ezAR Technologies
// All rights reserved.
//


#import "Cordova/CDV.h"
#import <AVFoundation/AVFoundation.h>

#ifndef CDVezAR_h
#define CDVezAR_h

@interface CDVezAR : CDVPlugin

- (void) init:(CDVInvokedUrlCommand*)command;

- (void) getCameras:(CDVInvokedUrlCommand*)command;

- (void) activateCamera:(CDVInvokedUrlCommand*)command;

- (void) deactivateCamera:(CDVInvokedUrlCommand*)command;

- (void) startCamera:(CDVInvokedUrlCommand*)command;

- (void) stopCamera:(CDVInvokedUrlCommand*)command;

- (void) maxZoom:(CDVInvokedUrlCommand*)command;

- (void) getZoom:(CDVInvokedUrlCommand*)command;

- (void) setZoom:(CDVInvokedUrlCommand*)command;

- (void) getLight:(CDVInvokedUrlCommand*)command;

- (void) setLight:(CDVInvokedUrlCommand*)command;

- (void) screenshot:(CDVInvokedUrlCommand*)command;

@end

typedef NS_ENUM(NSInteger, EZAR_ERROR_CODE) {
    EZAR_ERROR_CODE_ERROR,
    EZAR_ERROR_CODE_INVALID_ARGUMENT,
    EZAR_ERROR_CODE_INVALID_STATE,
    EZAR_ERROR_CODE_ACTIVATION
};


#endif
