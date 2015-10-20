var app = angular.module('WebServicesApp', ['ui.bootstrap']);
app.controller('WebServicesController', function ($scope, $http, $attrs) {
    var ctrl = this;
    ctrl.rootUrl = $attrs.rootUrl;
    ctrl.expandAll = false;

    ctrl.load = function () {

        $http.get(ctrl.rootUrl + "/js/restapidoc/restapidoc.json").success(function (data, status, headers, config) {
            ctrl.apis = data.apis;
        });
    };
    ctrl.load();

});
