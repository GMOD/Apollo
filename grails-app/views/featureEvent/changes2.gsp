<%@ page import="org.bbop.apollo.FeatureEvent" %>
<%@ page import="org.bbop.apollo.history.FeatureEventView" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <g:set var="entityName" value="${message(code: 'featureEvent.label', default: 'FeatureEvent')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
    <asset:javascript src="vendor/angular-1.3.17.js"/>
    <asset:javascript src="vendor/ui-bootstrap-custom-0.13.4.js"/>
    <asset:javascript src="vendor/ui-bootstrap-custom-tpls-0.13.4.js"/>
    <asset:javascript src="ReportController.js"/>
</head>

<body>

<g:render template="../layouts/reportHeader"/>

<div id="list-featureEvent" class="content scaffold-list" role="main">
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>


    <section class="first" ng-app="ReportApp">

        <div id="reportController" class="form-group" ng-controller="ReportController"
             data-organism-id="${organismInstance.id}">
            <g:hasErrors bean="${organismInstance}">
                <div class="alert alert-danger">
                    <g:renderErrors bean="${organismInstance}" as="list"/>
                </div>
            </g:hasErrors>

            <div class="row">
                <div class="col-md-4">
                    <p class="input-group">
                        <input id='startDate' datepicker-popup="{{format}}" type="fullDate" class="form-control"
                               datepicker-options="dateOptions" datepicker-popup ng-model="startDate"
                               is-open="openedStart" close-text="Close"/>
                        <span class="input-group-btn">
                            <button type="button" class="btn btn-default" ng-click="openStart($event)"><i
                                    class="glyphicon glyphicon-calendar"></i></button>
                        </span>
                    </p>
                </div>

                <div class="col-md-4">
                    <p class="input-group">
                        <input id='endDate' datepicker-popup="{{format}}" type="fullDate" class="form-control"
                               datepicker-options="dateOptions" datepicker-popup ng-model="endDate" is-open="openedEnd"
                               close-text="Close"/>
                        <span class="input-group-btn">
                            <button type="button" class="btn btn-default" ng-click="openEnd($event)"><i
                                    class="glyphicon glyphicon-calendar"></i></button>
                        </span>
                    </p>
                </div>

            </div>

            <div ng-show="showTable">
                From server: '{{report.testValue}}'
            </div>

            <div ng-hide="showTable">
                Loading . . .
            </div>

            <div ng-show="showTable">
                <table>
                    <thead>
                    <tr>
                        <th>
                            Operation
                        </th>
                    </tr>
                    </thead>
                    <tbody>

                    <tr ng-repeat="change in report.changes" >
                        <td>{{change.operation}}</td>
                    </tr>

                    </tbody>
                </table>
            </div>
        </div>
    </section>
</body>
</html>
