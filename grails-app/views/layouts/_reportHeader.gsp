<%@ page import="org.codehaus.groovy.grails.web.json.JSONArray" %>
<nav class="navbar navbar-default">
    <div class="apollo-header row">
        <asset:image src="ApolloLogo_100x36.png"/>
    </div>

    <div class="container-fluid">
        <!-- Brand and toggle get grouped for better mobile display -->
        <div class="navbar-header">
            <div class="input-prepend">
                <a class="navbar-brand glyphicon glyphicon-home" href="${createLink(uri: '/')}">Home</a>

                <div class="btn btn-group">
                    <button class="btn dropdown-toggle glyphicon glyphicon-list-alt " data-toggle="dropdown">
                        Reports
                        <span class="caret"></span>
                    </button>
                    <ul class="dropdown-menu">
                        <g:each in="${grailsApplication.config.apollo.administrativePanel}" var="admin">
                            <g:if test="${admin.type=='report'}">
                                <li><g:link uri="${admin.link}">${admin.label}</g:link></li>
                            </g:if>
                        %{--<li><g:link action="report" controller="organism">Organism Annotations</g:link></li>--}%
                        %{--<li><g:link action="report" controller="annotator">Annotation Report</g:link></li>--}%
                        %{--<li><g:link action="systemInfo" controller="home">System Info</g:link></li>--}%
                        </g:each>
                    </ul>
                </div>
            </div>

        </div>
    </div>
</nav>
