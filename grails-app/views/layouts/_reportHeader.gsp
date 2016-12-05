<%@ page import="org.codehaus.groovy.grails.web.json.JSONArray" %>
%{--<nav class="navbar navbar-default">--}%
    <div class="apollo-header row">
        <div class="col-lg-10">
            <g:link uri="/"><asset:image src="ApolloLogo_100x36.png"/></g:link>
            <perms:admin>
                <div class="btn-group" role="group">
                    <button class="btn btn-default dropdown-toggle glyphicon glyphicon-list-alt" data-toggle="dropdown">
                        <div style="font-family: Arial, Helvetica, sans-serif; font-weight: bold;display: inline;">
                        Reports
                            </div>
                        <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu apollo-dropdown">
                        <g:each in="${grailsApplication.config.apollo.administrativePanel}" var="report">
                            <g:if test="${report.type == 'report'}">
                                <li><g:link uri="${report.link}">${report.label}</g:link></li>
                            </g:if>
                        </g:each>
                    </ul>
                </div>
            </perms:admin>
        </div>

        %{--<div class="col-lg-4 col-lg-offset-2">--}%
        <div class="pull-lg-right">
            <shiro:user>
            %{----}%
                <h4>
                    <div class="label label-primary">
                        <shiro:principal/>
                    </div>

                    <div class="btn btn-info">
                        <g:set var="targetUri" value="/${controllerName}/${actionName}"/>
                        <g:link class="glyphicon glyphicon-log-out" action="logout" controller="login"
                                params="[targetUri: targetUri]">
                            %{--Logout--}%
                        </g:link>
                    </div>
                </h4>
            %{--<a href="Login?operation=logout" type="button" class="fa fa-icon-signout">Signout</a>--}%
            </shiro:user>
        </div>

    </div>
%{--</nav>--}%
