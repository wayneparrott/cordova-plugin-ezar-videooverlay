/**
 * CDVezAR.h
 *
 * Copyright 2015, ezAR Technologies
 * http://ezartech.com
 *
 * By @wayne_parrott, @vridosh, @kparrott
 *
 * Licensed under a modified MIT license. 
 * Please see LICENSE or http://ezartech.com/ezarstartupkit-license for more information
 */

#import <AVFoundation/AVFoundation.h>

#import "Cordova/CDV.h"

/**
 * Implements the ezAR Cordova api. 
 */
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

- (void) snapshot:(CDVInvokedUrlCommand*)command;

@end

typedef NS_ENUM(NSUInteger, EZAR_ERROR_CODE) {
    EZAR_ERROR_CODE_ERROR=1,
    EZAR_ERROR_CODE_INVALID_ARGUMENT,
    EZAR_ERROR_CODE_INVALID_STATE,
    EZAR_ERROR_CODE_ACTIVATION
};

typedef NS_ENUM(NSUInteger, EZAR_IMAGE_ENCODING) {
    EZAR_IMAGE_ENCODING_JPG=0,
    EZAR_IMAGE_ENCODING_PNG
};



