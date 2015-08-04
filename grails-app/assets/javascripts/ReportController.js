var app = angular.module('ReportApp', ['ui.bootstrap', 'ui.bootstrap.datepicker']);
app.controller('ReportController', function ($scope, $http, $attrs) {

    $scope.currentDate = new Date();
    $scope.organismId = $attrs.organismId;
    $scope.startDate = new Date();
    $scope.endDate = new Date();
    // DATE RELATED CODE
    var tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);

    var afterTomorrow = new Date();
    afterTomorrow.setDate(tomorrow.getDate() + 2);

    $scope.today = function () {
        startDate = new Date();
    };

    $scope.clear = function () {
        startDate = null;
    };

    $scope.startDate = new Date();
    $scope.events =
        [
            {
                date: tomorrow,
                status: 'full'
            },
            {
                date: afterTomorrow,
                status: 'partially'
            }
        ];

    $scope.openStart = function ($event) {
        $event.preventDefault();
        $event.stopPropagation();

        $scope.openedStart = true;
    };

    $scope.openEnd = function ($event) {
        $event.preventDefault();
        $event.stopPropagation();

        $scope.openedEnd = true;
    };

    $scope.dateOptions = {
        formatYear: 'yy',
        startingDay: 1
    };

    $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'dd.MM.yyyy', 'shortDate','fullDate'];
    $scope.format = $scope.formats[4];

    // DATE RELATED CODE


    $scope.load = function () {
        $scope.showTable = false;

        var startDate = $scope.currentDate.toISOString().substring(0, $scope.currentDate.toISOString().indexOf("T"));
        var endDate = $scope.currentDate.toISOString().substring(0, $scope.currentDate.toISOString().indexOf("T"));

        // TODO: apollo should use a more generic link from the server !!!

        $http.get('/apollo/featureEvent/changesValues/' + $scope.organismId + '?startDate=' + startDate + "&endDate=" + endDate)
            .success(function (data, status, headers, config) {
                $scope.report = angular.copy(data);
                $scope.showTable = true;
            });
    };

    $scope.today();
    $scope.load()

});

