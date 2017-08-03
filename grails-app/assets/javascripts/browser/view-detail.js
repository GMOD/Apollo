var app = angular.module('track-detail-app', []);
app.controller('TrackViewDetailController', function($scope, $http, $attrs) {
    var ctrl = this;

    ctrl.load = function() {

        $http.get('../track/loadSequence/?track=' + $attrs.track+'&organism='+$attrs.organism+'&sequence='+$attrs.sequence+'&name='+$attrs.name)
            .success(function(data) {
                //alert('return success');
                ctrl.data = data ;
            })
            .error(function(data) {
                //alert('return error: '+data);
                ctrl.errorstring = data;
            });

    };


    ctrl.load();

});
