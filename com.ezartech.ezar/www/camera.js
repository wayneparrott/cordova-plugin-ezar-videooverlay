var exec = require('cordova/exec'),
    utils = require('cordova/utils');

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
	
    
    this.isActive = function() {
        return _ezar.hasActiveCamera() && _ezar.getActiveCamera() === _self;
    }
    
    
	this.getId = function() {
		return _id;
	};
	
	
	this.getPosition = function() {
		return _position;
	};
	
	
//	getViewport = function() {
//		return _viewport;
//	}
//	
//	setViewport = function(top, left, width, height) {
//		
//	}
	
	
	this.hasLight = function() {
		return _hasLight;
	};
	
	
	this.getLight = function() {
		return _light;
	};
	
	
	this.setLight = function(light, successCB) {
		if (!_self.hasLight()) return;
		
		_light = light;
		
        if (_self.isActive() && _self.isRunning()) {
            exec(function() {console.log('success');},
                 function(error) {console.log('error: ' + error); },
                 "ezAR",
                 "setLight",
                 [_light]);
        } 
	};
	
	
	//todo - test for haszoom
	this.hasZoom = function() {
		return _hasZoom;
	};
	
	
	this.getZoom = function() {
		return _zoom;
	};
	
	
	this.setZoom = function(zoom) {
		_zoom = zoom;
    
        console.log("zooming camera: " + _zoom);

        if (_self.isActive() && _self.isRunning()) {
            exec(function() {console.log('success');},
                 function(error) {console.log('error: ' + error); },
                 "ezAR",
                 "setZoom",
                 [_zoom]);
        }
	};
    
	
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
    
    
    this.stop = function(successCallback,errorCallback) {
        if (!_self.isRunning()) return;
        
        exec(function(data) {
                _ezar._deactivateCamera(_self);
                if (successCallback) {
                    successCallback(data);
                }
             },
             function(error) {
                console.log('error: ' + error); 
                if (errorCallback) {
                    errorCallback(error);
                }
             },
             "ezAR",
             "stopCamera",
             []);
    };
    
    
    this.isRunning = function() {
        return _self.isActive();
        //return _self.getState() == Camera.State.RUNNING;
    };
    
    
    this.isStopped = function() {
        return !_self.isRunning();
        //return _self.getState() == Camera.State.STOPPED;
    };
    
    
    this.screenshot = function(successCB, errorCB) {
		if (!_self.isActive()) return;
		
        exec(function() {console.log('success');},
             function(error) {console.log('error: ' + error); },
             "ezAR",
             "screenshot",
             []); 
	};
	
}

Camera.Position = {
	FRONT: 'FRONT',
	BACK : 'BACK'
}

Camera.State = {
		STOPPED : 'STOPPED',
		RUNNING : 'RUNNING',
	}

function isFunction(f) {
    return typeof f == "function";
}

module.exports = Camera;