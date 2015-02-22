

var Camera = require('./camera'),
    exec = require('cordova/exec'),
    utils = require('cordova/utils');

module.exports = (function() {
           
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
                  
                  console.log("camera: " + camera);
                  console.log(camera);
                  
        return camera;
    }
                  
    function initCameras(deviceData) {
        CAMS = deviceData;
        console.log(deviceData);
                  
        if ('FRONT' in deviceData) {
            _frontCamera = initCamera(deviceData.FRONT);
            console.log('front'); console.log(_frontCamera);
        }
        if ('BACK' in deviceData) {
            _backCamera = initCamera(deviceData.BACK);
            console.log('back'); console.log(_backCamera);
        }
    }
           
    //--------------------------------------
    var _ezAR = {};
    var errorHandler;
    var _isInitialized = false;
    var _frontCamera;
    var _backCamera;
    var _activeCamera;
    
    _ezAR.isInitialized = function() {
        return _isInitialized;
    }
    
    //todo support errorCallback
    _ezAR.initialize = function(successCallback,errorCallback) {
        //execute successCallback immediately if already initialized
    	if (_ezAR.isInitialized()) {
           if (isFunction(successCallback)) successCallback();
           return;
        }
        
        var onInit = function(deviceData) {
            console.log(deviceData);
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
    
    _ezAR.getCameras = function() {
        var cameras = [];
        if (_frontCamera) cameras.push(_frontCamera);
        if (_backCamera) cameras.push(_backCamera);
         return cameras;
    }
    
    _ezAR.getBackCamera = function() {
         return _backCamera;
    }
    
    _ezAR.hasBackCamera = function() {
         return !!_ezAR.getBackCamera();
    }
    
    _ezAR.getFrontCamera = function() {
         return _frontCamera;
    }
    
    _ezAR.hasFrontCamera = function() {
         return !!_ezAR.getFrontCamera();
    }
    
    _ezAR.getActiveCamera = function() {
        return _activeCamera;
    }
           
    _ezAR.hasActiveCamera = function() {
        return _ezAR.getActiveCamera() != null;
    }
           
    
    //private, update ezar active camera
    _ezAR._activateCamera = function(camera) {
         _activeCamera = camera;  
        
        /*
        if (!camera) return;
        if (_ezAR.getActiveCamera() == camera) return;
        
        if (_ezAR.hasActiveCamera()) {
            _ezAR.deactivateCamera();
        }
                  
        _activeCamera = camera;
        _ezAR.onError = errorCallback;
                  
          exec(function(data) {
                 camera._setActive(true);
                 console.log('activate success: ' + data);},
           _ezAR.onError,
           ezAR",
           "activateCamera",
           [camera.getPosition(),
            camera.hasZoom() ? camera.getZoom() : 0,
            camera.hasLight() ? camera.getLight() : 0]);
        */
    }
           
    //private - update ezar activate camera to undefined
    _ezAR._deactivateCamera = function() {
        _activeCamera = null;   
    }
    
                  
                       
    return _ezAR;
    
}());



         

    