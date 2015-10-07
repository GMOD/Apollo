<%@ page import="org.bbop.apollo.FeatureEvent" %>
<%@ page import="org.bbop.apollo.history.FeatureEventView" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Apollo Web Service APIs</title>
    <asset:javascript src="vendor/angular.min.js"/>
    <asset:javascript src="vendor/angular-strap.min.js"/>
    <asset:javascript src="vendor/angular-strap.tpl.min.js"/>
    <asset:javascript src="vendor/angular-route.js"/>
    <asset:javascript src="vendor/ui-bootstrap-custom-0.13.1.js"/>
    <asset:javascript src="vendor/ui-bootstrap-custom-tpls-0.13.1.js"/>

    <asset:javascript src="WebServicesController.js"/>
</head>

<body>

<g:render template="../layouts/reportHeader"/>

<div id="list-featureEvent" class="content scaffold-list" role="main">
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>


    <section class="row" ng-app="WebServicesApp">

        <div id="reportController" class="col-sm-offset-1" ng-controller="WebServicesController as ctrl"
             data-root-url="${application.contextPath}">

            {{ctrl.rootUrl}}
            {{ctrl.apis.length}}

            <div class="jumbotron">
            {{ctrl.apis}}
        </div>


            <ul>
                <li ng-repeat="api in ctrl.apis">
                    {{api.name}}::{{api.description}}::
                    Methods = {{api.methods.length}}
                    <ul>
                        <li ng-repeat="method in api.methods">
                            Method: {{method}}
                        </li>
                    </ul>
                </li>
            </ul>

            <accordian ng-cloak>
                <accordion-group ng-repeat="api in ctrl.apis">
                    <accordion-heading>
                        One
                        {{api.method}}
                        %{--<i class="pull-right glyphicon"--}%
                        %{--ng-class="{'glyphicon-chevron-down': status.open, 'glyphicon-chevron-right': !status.open}"></i>--}%
                    </accordion-heading>
                    %{--{{api}}--}%
                </accordion-group>

                <div>
                    <ul>
                        <li ng-repeat="method in api.methods">{{method}}</li>
                    </ul>
                    asdfasdf
                </div>
            </accordian>
        </div>
    </section>
</div>

</body>
</html>
