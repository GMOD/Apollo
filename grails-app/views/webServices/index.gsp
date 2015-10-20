<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">
    <title>Apollo Web Service APIs</title>
    <asset:javascript src="vendor/jquery-1.11.1.min.js"/>
    <asset:javascript src="vendor/jquery-ui-1.11.2.custom/jquery-ui.min.js"/>
    <asset:javascript src="vendor/angular.min.js"/>
    <asset:javascript src="vendor/angular-strap.min.js"/>
    <asset:javascript src="vendor/angular-strap.tpl.min.js"/>
    <asset:javascript src="vendor/ui-bootstrap-custom-0.13.4.js"/>
    <asset:javascript src="vendor/ui-bootstrap-custom-tpls-0.13.4.js"/>
    <asset:stylesheet src="ui-bootstrap-custom-0.13.4-csp.css"/>


    %{--<asset:javascript src="restapidoc/restapidoc.json"/>--}%
    <asset:javascript src="WebServicesController.js"/>
</head>

<body>

<g:render template="../layouts/reportHeader"/>

%{--<jumbotron>--}%
<div class="page-header" style="margin-left: 30px;">
    <h3>Web Service API</h3>


    %{--<h4>Examples</h4>--}%
    Here are a
    <a href="https://github.com/GMOD/Apollo/tree/master/docs/web_services/examples">number of examples web services scripts</a> in different languages including shell, groovy (Java), and perl.


    %{--<h4>Notes about connecting to https</h4>--}%

    %{--TODO--}%
    %{--<p>--}%
        %{--It is advisable if sending out secure passwords over the global internet that you use https.--}%

        %{--If you use https, you will need to either:--}%
    %{--</p>--}%
    %{--<ul>--}%
        %{--<li>Add the web service to the CA Store <a href="">More info here.</a></li>--}%
        %{--<li>Have the client okay specific server connections <a href=""></a></li>--}%
        %{--<li>Use curl <a href=""></a></li>--}%
    %{--</ul>--}%

    %{--<h4></h4>--}%
</div>
%{--The Apollo Web Service API is a JSON-based REST API to interact with the annotations and other services of Web Apollo. Both the request and response JSON objects can contain feature information that are based on the Chado schema. We use the web services API for several scripting examples and also use them in the Web Apollo JBrowse plugin, and this document provides details on the parameters for each API.--}%
%{--What is the Web Service API?--}%
%{--For a given Web Apollo server url (e.g., https://localhost:8080/apollo or any other Web Apollo site on the web), the Web Service API allows us to make requests to the various "controllers" of the application and perform operations.--}%
%{--The controllers that are available for Web Apollo include the AnnotationEditorController, the OrganismController, the IOServiceController for downloads of data, and the UserController for user management.--}%
%{--Most API requests will take:--}%
%{--<div class="">--}%
%{--The proper url (e.g., to get features from the AnnotationEditorController, we can send requests to http://localhost/apollo/annotationEditor/getFeatures)--}%
%{--username - an authorized user--}%
%{--password - a password--}%
%{--organism - (if applicable) the "common name" of the organism for the operation -- will also pull from the "user preferences" if none is specified.--}%
%{--track/sequence - (if applicable) reference sequence name (shown in sequence panel / genomic browse)--}%
%{--</div>--}%
%{--uniquename - (if applicable) the uniquename is a UUID used to guarantee a unique ID--}%
%{--</jumbotron>--}%

<div id="list-featureEvent" class="content scaffold-list" role="main">
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>


    <div ng-app="WebServicesApp">

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

            %{--<div style="margin-bottom: 10px;">--}%
            %{--<button ng-click="$scope.accordion.firstGroupOpen = true;">Expand / Collapse All</button>--}%
            %{--</div>--}%
            %{--<button ng-click="ctrl.expandAll=false">Collapse all</button>--}%

            %{--TODO: simulate formatting:--}%
            %{--http://loic911.github.io/restapidoc/demo/sample/demo.html?doc_url=http://loic911.github.io/restapidoc/demo/sample/restapidoc.json#--}%
            %{--http://loic911.github.io/restapidoc/#@RestApiResponseObject--}%

            <accordion ng-cloak class="col-sm-11">
                <accordion-group ng-repeat="api in ctrl.apis" is-open="status.open">
                    <accordion-heading>
                        {{api.name}}
                        <span class="small">{{api.description}}</span>

                        <i class="pull-right glyphicon"
                           ng-class="{'glyphicon-chevron-down': status.open, 'glyphicon-chevron-right': !status.open}"></i>
                        <span class="pull-right badge badge-default">{{api.methods.length}}</span>

                    </accordion-heading>

                    %{--Methods: {{api.methods.length}}--}%
                    <accordion>
                        <accordion-group ng-repeat="method in api.methods" is-open="inner.open">

                            <accordion-heading>
                                {{method.methodName}}
                                <span class="small">{{method.description}}</span>

                                <i class="pull-right glyphicon"
                                   ng-class="{'glyphicon-chevron-down': inner.open, 'glyphicon-chevron-right': !inner.open}"></i>
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
