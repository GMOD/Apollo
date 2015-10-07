<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Apollo Web Service APIs</title>
    <asset:javascript src="vendor/angular.min.js"/>
    <asset:javascript src="vendor/angular-strap.min.js"/>
    <asset:javascript src="vendor/angular-strap.tpl.min.js"/>
    <asset:javascript src="vendor/ui-bootstrap-custom-0.13.4.js"/>
    <asset:javascript src="vendor/ui-bootstrap-custom-tpls-0.13.4.js"/>
    <asset:stylesheet src="ui-bootstrap-custom-0.13.4-csp.css"/>


    <asset:javascript src="WebServicesController.js"/>
</head>

<body>

<g:render template="../layouts/reportHeader"/>

<div id="list-featureEvent" class="content scaffold-list" role="main">
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>

    <div class="row" ng-app="WebServicesApp">

        <div class="col-sm-offset-1" ng-controller="WebServicesController as ctrl"
             data-root-url="${application.contextPath}">

            {{ctrl.rootUrl}}
            Services: {{ctrl.apis.length}}

            %{--<div class="jumbotron">--}%
            %{--{{ctrl.apis}}--}%
            %{--</div>--}%


            %{--TODO: simulate formatting:--}%
            %{--http://loic911.github.io/restapidoc/demo/sample/demo.html?doc_url=http://loic911.github.io/restapidoc/demo/sample/restapidoc.json#--}%
            %{--http://loic911.github.io/restapidoc/#@RestApiResponseObject--}%

            <accordion ng-cloak>
                <accordion-group ng-repeat="api in ctrl.apis" is-open="true">
                    <accordion-heading>
                        {{api.name}}
                        <span class="small">{{api.description}}</span>

                    </accordion-heading>

                    Methods: {{api.methods.length}}
                    <ul>
                        <li ng-repeat="method in api.methods">
                            <div class="info">
                                {{method.methodName}} {{method.description}}
                            </div>

                            <div class="info">
                                Consumes: {{method.consumes}} {{method.verb}}

                                <ul>
                                    <li ng-repeat="param in method.queryparameters">
                                        {{param.name}}
                                        {{param.description}}
                                        {{param.required}}
                                        {{param.type}}
                                    </li>
                                </ul>
                            </div>
                            <div class="response">
                                Response {{method.produces}}
                                %{--{{method.response}} --}%
                            </div>

                            <br/>
                            %{--{{method}}--}%
                        </li>
                    </ul>

                </accordion-group>

            </accordion>
        </div>
    </div>
</div>

</body>
</html>
