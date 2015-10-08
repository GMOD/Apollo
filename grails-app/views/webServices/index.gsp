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

            %{--<div class="page-header">--}%
            <div class="alert alert-warning">
                <h3 style="display: inline;">BASE URL:
                <a href="${request.secure ? 'https' : 'http'}://${request.serverName}${request.serverPort != 80 ? ":" + request.serverPort : ''}${request.contextPath}">
                ${request.secure ? 'https' : 'http'}://${request.serverName}${request.serverPort != 80 ? ":" + request.serverPort : ''}${request.contextPath}
                    </a>
                </h3>
            </div>
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

                    %{--Methods: {{api.methods.length}}--}%
                    <accordion>
                        <accordion-group ng-repeat="method in api.methods" is-open="true">

                            <accordion-heading>
                                {{method.methodName}}
                                <span class="small">{{method.description}}</span>

                            </accordion-heading>

                            <table class="table table-condensed table-striped table-bordered">
                                <tbody>
                                <tr>
                                    <th style="width:15%;">Path</th>
                                    <td>{{method.path}}</td>
                                </tr>
                                <tr>
                                    <th style="width:15%;">Description</th>
                                    <td>{{method.description}}</td>
                                </tr>
                                <tr>
                                    <th style="width:15%;">Method</th>
                                    <td>{{method.consumes}} {{method.verb}}</td>
                                </tr>
                                <tr>
                                    <th colspan="2">Parameters</th>
                                </tr>
                                <tr ng-repeat="param in method.queryparameters">
                                    <td style="width:15%">
                                        <code>
                                            {{param.name}}
                                        </code>
                                    </td>
                                    <td>
                                        %{--<span ng-show="param.required" class="badge badge-success">Required</span>--}%
                                        %{--<span ng-hide="param.required" class="badge badge-info">Optional</span>--}%
                                        <span class="badge badge-important">{{param.type}}</span>
                                        {{param.description}}
                                    </td>
                                </tr>
                                <tr>
                                    <th colspan="2">Response</th>
                                </tr>
                                <tr>
                                    <th style="width:15%;">Produces</th>
                                    <td>{{method.produces}}</td>
                                </tr>
                                </tbody>
                            </table>

                        </accordion-group>
                    </accordion>

                </accordion-group>

            </accordion>
        </div>
    </div>
</div>

</body>
</html>
