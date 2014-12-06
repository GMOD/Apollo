//= require annotator/app

angular.module('AnnotatorApplication').controller('AnnotatorController', function ($scope, $rootScope, $http, $location) {

    $scope.data = "asdfg";

    $scope.tabs = [
        { title: 'A',active:'true',disabled:'false',content:'dude' }
        ,{ title: 'B',active:'true',disabled:'false',content:'what?' }
    ];

    $scope.pingSelf = function(){
  console.log('ping');
    };


});
