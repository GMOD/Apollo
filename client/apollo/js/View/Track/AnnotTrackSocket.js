define( [
            'dojo/_base/declare',
            'dojo/request/xhr',
            'dojox/socket'
], 
    function( declare, xhr, Socket ) {

    var maxTries = 5;
    
    var AnnotTrackSocket = declare ( null, {
    
        constructor: function(contextPath, track) {
            this.contextPath = contextPath;
            this.track = track;
            this.tryNum = 0;
            this.socketSupport = this.hasWebSocketSupport();
            this.open();
        },
    
        open: function() {
            if (this.socketSupport) {
                this._initWebSocket();
            }
            else {
                this._initLongPoll();
            }
        },
    
        close: function() {
            if (this.socketSupport) {
                if (this.socket) {
                    this.socket.close();
                }
            }
            else {
                if (this.socket && !this.socket.isResolved() ) {
                    this.socket.cancel();
                }
            }
        },
    
        send: function(updateData, loadCallback) {
            var track = this.track;
            var socket = this;
            if (!this.socket) {
                this.track.handleError({responseText: '{ "error": "Server connection error - try reloading the page" }'});
                return;
            }
            if (this.socketSupport) {
                this.socket.send(updateData);
            }
            else {
                alert('Web sockets are not support by this browser');
                //if (!this.socket || this.socket.isResolved() ) {
                //  track.handleError({responseText: '{ "error": "Server connection error - try reloading the page" }'});
                //  return;
                //}
                //xhr(socket.contextPath + "/AnnotationEditorService", {
                //  method: "post",
                //  data: updateData,
                //  handleAs: "json"
                //}).then(function(response) {
                //  if (loadCallback) {
                //      loadCallback(response);
                //  }
                //  if (response && response.alert) {
                //      alert(response.alert);
                //  }
                //}, function(response) {
                //  track.handleError(response);
                //});
        
            }
        },
    
        hasWebSocketSupport: function() {
            return "WebSocket" in window || "MozWebSocket" in window;
        },
    
        _initWebSocket: function() {
            var track = this.track;
            //this.socket = new Socket(this.contextPath + "/AnnotationEditor/" + track.getUniqueTrackName());
            this.socket = new Socket(this.contextPath + "/AnnotationNotification/incoming");
            var socket = this;
            this.socket.on("message", function(event) {
                socket.tryNum = 0;
                var response = JSON.parse(event.data);
                if (response.confirm) {
                    if (track.handleConfirm(response.confirm)) {
                        var json = { track : response.track, features: response.features, operation: response.operation, confirm: true };
                        var postData = JSON.stringify(json);
                        track.executeUpdateOperation(postData);
                    }
                }
                else if (response.error) {
                    track.handleError({ response: { data: response } });
                }
                else {
                    socket._handleUpdate(response);
                }
            });
        
            this.socket.on("close", function(event) {
                switch (event.code) {
                case 1000:
                    var reason = event.reason;
                    if (reason == "Logged out") {
                        track.hide();
                        track.changed();
                        track.handleError({responseText: '{ "error": "Logged out" }'});
                        window.location.reload();
                    }
                    break;
                case 1001:
                    track.handleError({responseText: '{ "error": "Server connection error" }'});
                    window.location.reload();
                    break;
                default:
                    if (socket.tryNum < maxTries) {
                        window.setTimeout(function() {
                            socket.tryNum++;
                            socket._initWebSocket();
                        }, socket.tryNum * 500);
                    }
                    else {
                        track.handleError({responseText: '{ "error": "Server connection error" }'});
                        window.location.reload();
                    }
                    break;
                }
            });
            
        },
    
        _initLongPoll: function() {
            var track = this.track;
            var socket = this;
            this.socket = xhr( socket.contextPath + "/AnnotationChangeNotificationService", {
                query: { track: track.getUniqueTrackName() },
                handleAs: "json",
                preventCache: true, 
                timeout: 5 * 60 * 1000
            }).then(function(response) {
                if (response != null) {
                    socket._handleUpdate(response);
                }
                socket.tryNum = 0;
                socket._initLongPoll();
            }, function(response, ioArgs) {
                // client cancel
                if (response.dojoType == "cancel") {
                    return;
                }
                // client timeout
                if (response.dojoType == "timeout") {
                    socket._initLongPoll();
                    return;
                }
                if (response.response.status == 0) {
                    if (socket.tryNum < maxTries) {
                        window.setTimeout(function() {
                            socket.tryNum++;
                            socket._initLongPoll();
                        }, socket.tryNum * 500);
                        return;
                    }
                    else {
                        track.handleError({responseText: '{ "error": "Server connection error" }'});
                        window.location.reload();
                        return;
                    }
                }
                // bad gateway
                if (response.response.status == 502) {
                    socket._initLongPoll();
                    return;
                }
                // server killed
                if (response.response.status == 503) {
                    track.handleError({responseText: '{ "error": "Server connection error" }'});
                    window.location.reload();
                    return;
                }
                // server timeout
                else if (response.response.status == 504){
                    socket._initLongPoll();
                    return;
                }
                // forbidden
                else if (response.response.status == 403) {
                    track.hide();
                    track.changed();
                    track.handleError({responseText: '{ "error": "Logged out" }'});
                    window.location.reload();
                    return;
                }
                // actual error
                if (response.responseText) {
                    track.handleError(response);
                    track.comet_working = false;
                    console.error("HTTP status code: ", response.response.status); //
                    return response;
                }
                // everything else
                else {
                    track.handleError({responseText: '{ "error": "Server connection error" }'});
                    return;
                }
            });
        
        },
    
        _handleUpdate: function(response) {
            var track = this.track;
            for (var i in response) {
                var changeData = response[i];
                if (changeData.operation == "ADD") {
                    if (changeData.sequenceAlterationEvent) {
                        track.getSequenceTrack().annotationsAddedNotification(changeData.features);
                    }
                    else {
                        track.annotationsAddedNotification(changeData.features);
                    }
                }
                else if (changeData.operation == "DELETE") {
                    if (changeData.sequenceAlterationEvent) {
                        track.getSequenceTrack().annotationsDeletedNotification(changeData.features);
                    }
                    else {
                        track.annotationsDeletedNotification(changeData.features);
                    }
                }
                else if (changeData.operation == "UPDATE") {
                    if (changeData.sequenceAlterationEvent) {
                        track.getSequenceTrack().annotationsUpdatedNotification(changeData.features);
                    }
                    else {
                        track.annotationsUpdatedNotification(changeData.features);
                    }
                }
            }
            track.changed();
        }
    });

    return AnnotTrackSocket;
} );

