/*
 * Camera.js
 * Copyright 2015, ezAR Technologies
 * Licensed under a modified MIT license, see LICENSE or http://ezartech.com/ezarstartupkit-license
 *
 * @file Implements the ezar camera api for controlling device cameras, 
 *  zoom level and lighting. 
 * @author @wayne_parrott, @vridosh, @kwparrott
 * @version 0.1.0 
 */

var exec = require('cordova/exec'),
    utils = require('cordova/utils');

/**
 * Manages a mobile device camera
 * @class
 * 
 * Created by ezar during initialization. Do not use directly.
 * 
 * @param {ezAR} ezar  protected 
 * @param {string} id  unique camera id assigned by the device os
 * @param {string} position  side of the device the camera resides, BACK
 * 			faces away from the user, FRONT faces the user
 * @param {boolean} hasZoom  true if the camera's magnification can be changed
 * @param {float} zoom  current magnification level of the camera up 
 * 			to the maxZoom
 * @param {boolean} hasLight true when the device has a light on the
 * 			same side as the camera position; false otherwise
 * @param {integer} light  if the device has a light on the same side 
 * 			(position) as the camera then 1 turns on the device light, 
 * 			0 turns the light off
 */
var Camera = function(ezar,id,position,hasZoom,maxZoom,zoom,hasLight,light) {
	var _ezar,
	    _id,
		_position,
		_viewport, //not used
		_hasLight,
		_light,
		_hasZoom,
		_maxZoom,
		_zoom,
		//_state = Camera.State.STOPPED,
    
    _self = this;
	_ezar = ezar;
    _id = id;
    _position = position;
    _hasZoom = hasZoom;
    _maxZoom = maxZoom;
    _zoom = zoom;
    _hasLight = hasLight;
    _light = light;
	    
    /**
     * @return {boolean} Test if this camera is currently running.
     */
    this.isActive = function() {
        return _ezar.hasActiveCamera() && _ezar.getActiveCamera() === _self;
    }
        
    /**
     * Camera's unique id assigned by the device.
     * @return {string} 
     */
	this.getId = function() {
		return _id;
	};
	
	/**
	 * The side of the device on which the camera resides
	 * 		BACK is the side of the device facing away from the user,
	 *      FRONT is the side of the device facing the user.
	 *  @return {string}
	 */
	this.getPosition = function() {
		return _position;
	};
	
	/**
	 * Identifies if the camera side of the device includes a light
	 * @return {boolean} 
	 */
	this.hasLight = function() {
		return _hasLight;
	};
	
	/**
	 * The current light setting, 0 == off, 1 == on
	 * @return {integer} 
	 */
	this.getLight = function() {
		return _light;
	};
	
	/**
	 * Turn the light on or off.
	 * @param {integer} light 0 to turn light off, 1 to turn light on
	 * @param {function} [successCB] function called on success
	 * @param {function} [errorCB] function with error data parameter called on error
	 */
	this.setLight = function(light, successCB, errorCB) {
		if (!_self.hasLight()) return;
		
		_light = light;
		
        if (_self.isActive() && _self.isRunning()) {
            exec( successCB,
                  errorCB,
                 "ezAR",
                 "setLight",
                 [_light]);
        } 
	};
	
	
	/**
	 * Camera supports magnification.
	 * @return {boolean} true indicates the camera supports zooming; otherwise return false.
	 */
	this.hasZoom = function() {
		return _hasZoom;
	};
	
	/**
	 * Current magnification level
	 * @return {float} a value between 1.0 and maxZoom
	 */
	this.getZoom = function() {
		return _zoom;
	};
	
	/**
	 * Increase or decrease magnification
	 * @param {float} zoom new magnification level, must be between 1.0 and maxZoom 
	 * @param {function} [successCB] function called on success
	 * @param {function} [errorCB] function with error data parameter called on error
	 */
	this.setZoom = function(zoom, successCB, errorCB) {
		_zoom = zoom;

        if (_self.isActive() && _self.isRunning()) {
            exec(function() {console.log('success');},
                 function(error) {console.log('error: ' + error); },
                 "ezAR",
                 "setZoom",
                 [_zoom]);
        }
	};

	/**
	 * Start video capture and presentation. This camera is the ezar#activeCamera
	 * @param {function} [successCB] function called on success
	 * @param {function} [errorCB] function with error data parameter called on error
	 */
	this.start = function(successCallback,errorCallback) {
        if (!_self.isStopped()) return;
        
        exec(function(data) {
                _ezar._activateCamera(_self);
                if (isFunction(successCallback)) {
                    successCallback(data);
                }
             },
             function(data) {
            	 if (isFunction(errorCallback)) {
            		 errorCallback(data);
            	 }
             },
             "ezAR",
             "startCamera",
             [_self.getPosition(),
              _self.hasZoom() ? _self.getZoom() : 0,
              _self.hasLight() ? _self.getLight() : 0]);
    };
    
	/**
	 * Stop video capture and presentation. Update ezar#activeCamera to be null
	 * @param {function} [successCB] function called on success
	 * @param {function} [errorCB] function with error data parameter called on error
	 */    
    this.stop = function(successCallback,errorCallback) {
        if (!_self.isRunning()) return;
        
        exec(function(data) {
                _ezar._deactivateCamera(_self);
                if (successCallback) {
                    successCallback(data);
                }
             },
             function(error) {
                if (errorCallback) {
                    errorCallback(error);
                }
             },
             "ezAR",
             "stopCamera",
             []);
    };
    
    /**
     * Check if camera is running.
     * @return {boolean} 
     */
    this.isRunning = function() {
        return _self.isActive();
    };
    
    /**
     * Check if camera is stopped.
     * @return {boolean} 
     */
    this.isStopped = function() {
        return !_self.isRunning();
    };
    
}

function isFunction(f) {
    return typeof f == "function";
}

module.exports = Camera;