/**
 * ezar.js
 * Copyright 2015, ezAR Technologies
 * Licensed under a modified MIT license, see LICENSE or http://ezartech.com/ezarstartupkit-license
 * 
 * @file Implements the ezar api for controlling device cameras, 
 *  zoom level and lighting. 
 * @author @wayne_parrott, @vridosh, @kwparrott
 * @version 0.1.0 
 */

var Camera = require('./camera'),
    exec = require('cordova/exec'),
    argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils');

module.exports = (function() {
           
	 //--------------------------------------
    var _ezAR = {};
    var errorHandler;
    var _isInitialized = false;
    var _frontCamera;
    var _backCamera;
    var _activeCamera;
    
    /**
     * Has ezAR been successfully initialized.
     * @return {boolean} true when initialize() completes successfully
     */
    _ezAR.isInitialized = function() {
        return _isInitialized;
    }
   
    /**
     * Initialize ezAR internal state, cameras, light and zoom features.
     * @param {function} [successCB] function called on success
	 * @param {function} [errorCB] function with error data parameter called on error
     */
    _ezAR.initialize = function(successCallback,errorCallback) {
        //execute successCallback immediately if already initialized
    	if (_ezAR.isInitialized()) {
           if (isFunction(successCallback)) successCallback();
           return;
        }
        
        var onInit = function(deviceData) {
            //console.log(deviceData);
            _ezAR.displaySize =
                { width: deviceData.displayWidth,
                  height: deviceData.displayHeight
                };
            initCameras(deviceData);
            _isInitialized = true;
            if (successCallback) {
                successCallback();
            }
        }
        
        _ezAR.onError = errorCallback;
        
        exec(onInit,
             _onError,
            "ezAR",
            "init",
            []);
    }
    
    /**
     * Return Camera[]. Must call initialize() before calling this function.
     * @return {Camera[]} array of cameras detected 
     */
    _ezAR.getCameras = function() {
        var cameras = [];
        if (_frontCamera) cameras.push(_frontCamera);
        if (_backCamera) cameras.push(_backCamera);
         return cameras;
    }
    
    /**
     * The camera facing away from the user. The camera has position BACK. 
     * @return {Camera} null the device does not have a BACK position camera or if ezAR has no been initialized
     */
    _ezAR.getBackCamera = function() {
         return _backCamera;
    }
    
    
    /**
     * Test for a camera facing away from the user. Call initialize() before using this function.
     * @return {boolean} true when the device has a camera with position BACK; otherwise return false.
     */
    _ezAR.hasBackCamera = function() {
         return !!_ezAR.getBackCamera();
    }
    
    
    /**
     * The camera facing towards the user. The camera has position FRONT. 
     * @return {Camera} null the device does not have a FRONT position camera or if ezAR has no been initialized
     */
    _ezAR.getFrontCamera = function() {
         return _frontCamera;
    }
    
    /**
     * Test for a camera facing towards the user. Call initialize() before using this function.
     * @return {boolean} true when the device has a camera with position FRONT; otherwise return false.
     */    
    _ezAR.hasFrontCamera = function() {
         return !!_ezAR.getFrontCamera();
    }
    
    /**
     * The camera currently running or null.
     * Call initialize() before using this function. 
     * @return {Camera} 
     */
    _ezAR.getActiveCamera = function() {
        return _activeCamera;
    }
    
    /**
     * Test for a running camera.
     * Call initialize() before using this function. 
     * @return {boolean} 
     */           
    _ezAR.hasActiveCamera = function() {
        return _ezAR.getActiveCamera() != null;
    }
                  
    /**
     * Create a screenshot image
     *
     *
     */
    
    _ezAR.snapshot = function(successCallback,errorCallback, options) {
            //todo: wayne - add init requirement checking
            //if (!_self.isActive()) return;
                  
        //options impl inspired by cordova Camera plugin
        options = options || {};
        var getValue = argscheck.getValue;
        var encodingType = getValue(options.encodingType, _ezAR.ImageEncodingType.JPEG);
        var saveToPhotoAlbum = !!options.saveToPhotoAlbum;
        
        var onSuccess = function(imageData) {
            var encoding = encodingType == _ezAR.ImageEncodingType.JPEG ? "jpeg" : "png";
            var dataUrl = "data:image/" + encoding + ";base64," + imageData;
            if (successCallback) {
                  successCallback(dataUrl);
            }
        };
                  
        _ezAR.onError = errorCallback;
                  
        exec(onSuccess,
             _onError,
             "ezAR",
             "snapshot",
            [encodingType, saveToPhotoAlbum]);

    }
                  
    _ezAR.ImageEncodingType = {
        JPEG: 0,             // Return JPEG encoded image
        PNG: 1               // Return PNG encoded image
    };

    
    
    //PROTECTED ------------

    //protected, update ezar active camera
    _ezAR._activateCamera = function(camera) {
         _activeCamera = camera;  
    }
    
           
    //protected - update ezar activate camera to undefined
    _ezAR._deactivateCamera = function() {
        _activeCamera = null;   
    }
                  
    
    
    //PRIVATE---------------
    
    function isFunction(f) {
        return typeof f == "function";
    }
           
    function _onError(data) {
        if (isFunction(_ezAR.onError)) {
           _ezAR.onError(data);
        }
    }
                  
    function initCamera(cameraData) {
        var id = cameraData.id;
        var position = cameraData.position;
        var zoom = cameraData.zoom
        var maxZoom = cameraData.maxZoom;
        var hasLight = cameraData.light;
        var lightLevel = hasLight ? cameraData.lightLevel : 0;
        var camera = new Camera(_ezAR,id,position,true,maxZoom,zoom,hasLight,lightLevel);
                  
                  //console.log("camera: " + camera);
                  //console.log(camera);
                  
        return camera;
    }
                  
    function initCameras(deviceData) {
        CAMS = deviceData;
        //console.log(deviceData);
                  
        if ('FRONT' in deviceData) {
            _frontCamera = initCamera(deviceData.FRONT);
            //console.log('front'); console.log(_frontCamera);
        }
        if ('BACK' in deviceData) {
            _backCamera = initCamera(deviceData.BACK);
            //console.log('back'); console.log(_backCamera);
        }
    }
    
    return _ezAR;
    
}());
