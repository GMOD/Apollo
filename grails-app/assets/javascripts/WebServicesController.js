var app = angular.module('WebServicesApp', ['ui.bootstrap']);
app.controller('WebServicesController', function ($scope, $http, $attrs) {
    var ctrl = this;
    ctrl.rootUrl = $attrs.rootUrl;
    ctrl.expandAll = false ;

    ctrl.load = function () {

        //{ "basePath": "http://localhost:8080/apollo", "apis": [{ "methods": [{ "headers": [], "bodyobject": null, "jsondocId": "b2d5ba4f-3993-46b9-9dba-ef11c808764c", "consumes": [], "response": { "mapKeyObject": "", "jsondocId": "2d0d8925-c0ad-4d2e-9ad0-2452b1cff842", "mapValueObject": "", "object": "organism" }, "pathparameters": [ { "jsondocId": "50ffb4fb-bfee-4a27-b7c6-264ec12707d7", "description": "", "name": "username", "allowedvalues": [], "format": "", "required": "true", "type": "email" }, { "jsondocId": "5ffb7e9c-13c3-45f7-b35c-b64514e2a58e", "description": "", "name": "password", "allowedvalues": [], "format": "", "required": "true", "type": "email" } ], "apierrors": [], "verb": "DELETE", "description": "Remove an organism", "queryparameters": [], "path": "/organism/delete/{id}.json", "produces": ["application/json"], "methodName": "deleteOrganism" }], "jsondocId": "26bd4a9f-b8e2-426a-9cd2-a9939b78c7e7", "description": "Methods for managing Organisms", "name": "Organism Services" }], "objects": [], "version": "0.1.1" }
        $http.get(ctrl.rootUrl + "/restApiDoc/api").success(function (data, status, headers, config) {
            ctrl.apis = data.apis;
        });
    };
    ctrl.load();

}) ;
