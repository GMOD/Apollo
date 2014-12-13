<%@ page import="org.bbop.apollo.Organism" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main">

    <g:set var="entityName" value="${message(code: 'sequence.label', default: 'Sequence')}"/>
    <title><g:message code="default.list.label" args="[entityName]"/></title>
</head>

<body>
<a href="#list-track" class="skip" tabindex="-1"><g:message code="default.link.skip.label"
                                                            default="Skip to content&hellip;"/></a>


<script>
    //var table;
    $(function () {
        $("#login_dialog").dialog({
            draggable: false,
            modal: true,
            autoOpen: false,
            resizable: false,
            closeOnEscape: false
        });
        $("#data_adapter_dialog").dialog({
            draggable: false,
            modal: true,
            autoOpen: false,
            resizable: false,
            closeOnEscape: false
        });
        $("#search_sequences_dialog").dialog({
            draggable: true,
            modal: true,
            autoOpen: false,
            resizable: false,
            closeOnEscape: false,
            width: "auto"
        });

        $(".adapter_button").button({icons: {primary: "ui-icon-folder-collapsed"}});
        $("#checkbox_menu").menu({});
        $("#menu").menubar({
                    autoExpand: false,
                    select: function (event, ui) {
                        $(".ui-state-focus").removeClass("ui-state-focus");
                    },
                    position: {
                        within: $('#frame').add(window).first()
                    }
                }
        );
        $("#checkbox_option").change(function () {
            update_checked(this.checked);
        });
        $("#check_all").click(function () {
            update_checked(true);
        });
        $("#check_none").click(function () {
            update_checked(false);
        });
        $("#check_displayed").click(function () {
            $(".track_select").prop("checked", true);
        });
        $("#unassigned-group-filter").change(function () {
            if (this.checked) {
                $("#group-filter").val("Unassigned");
            }
            else {
                $("#group-filter").val("");
            }
        });
        $(".track_select").click(function () {
            var allChecked = true;
            $(".track_select").each(function () {
                if (!$(this).prop("checked")) {
                    allChecked = false;
                    return false;
                }
            });
            $("#checkbox_option").prop("checked", allChecked);
        });
        <g:if test="${username==null}">
        login();
        </g:if>
        <g:if test="${username!=null}">
        createListener();
        </g:if>
        $("#logout_item").click(function () {
            logout();
        });
        $(".data_adapter").click(function () {
            var tracks = new Array();
            $(".track_select").each(function () {
                if ($(this).prop("checked")) {
                    tracks.push($(this).attr("id"));
                }
            });
            write_data($(this).text(), tracks, $(this).attr("_options"));
        });

        $("#select_tracks").click(function () {
            window.location = "sequences";
        });

        $("#genes").click(function () {
            window.location = "genes.jsp";
        });

        $("#search_sequence_item").click(function () {
            open_search_dialog();
        });
        $("#user_manager_item").click(function () {
            open_user_manager_dialog();
        });
        $("#web_services_api").click(function () {
            window.open('web_services/web_service_api.html', '_blank');
        });
        $("#previous-page").click(function () {
            var offset = parseInt($("#offset").val());
            if (offset != 0) {
                offset = offset - ${maximum};
                if (offset < 0) offset = 0;
                $("#offset").val(offset);
                $('.search-button').click();
            }

        });
        $("#next-page").click(function () {
            var offset = parseInt($("#offset").val());
//        if(offset!='0'){
            offset = offset + ${maximum};
            $("#offset").val(offset);
            $('.search-button').click();
//        }
        });
        $("#apollo_users_guide").click(function () {
            window.open('http://genomearchitect.org/web_apollo_user_guide', '_blank');
        });
        $("#delete_selected_item").click(function () {
            delete_selected_items();
        });
        %{--<g:each var="status" items="${allStatusList}">--}%
        %{--$("#change_status_selected_item-${status.replaceAll(" ", "_")}").click(function () {--}%
        %{--change_status_selected_items('${status}');--}%
        %{--});--}%
        %{--</g:each>--}%
        cleanup_user_item();
    });
</script>


<div class="nav" role="navigation">
    <ul>
        <li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
        <li><g:link class="create" action="create"><g:message code="default.new.label"
                                                              args="[entityName]"/></g:link></li>
    </ul>
</div>

<div id="header">
    <ul id="menu">
        <li><a href="http://genomearchitect.org/" target="_blank"><img id="logo" src="images/ApolloLogo_100x36.png"
                                                                       onload="cleanup_logo()" alt=""/></a></li>
        <%--<li><a id="file_item">File</a>--%>
        <%--<ul id="file_menu">--%>
        <%--<li><a id="export_menu">Export</a>--%>
        <%--<ul>--%>
        <%--<li><a class='none'>N/A</a></li>--%>
        <%--</ul>--%>
        <%--</li>--%>
        <%--</ul>--%>
        <%--</li>--%>

        <li><a id="view_item">View</a>
            <ul id="view_menu">
                <li><a id="select_tracks">Sequences</a></li>
            </ul>
        </li>

        <li><a id="tools_item">Tools</a>
            <ul id="tools_menu">
                <li><a id="search_sequence_item">Search sequence</a></li>
            </ul>
        </li>


        <g:if test="${isAdmin}">
            <li><a id="admin_item">Admin</a>
                <ul id="admin_menu">
                    <li><a id='user_manager_item'>Manage users</a></li>
                    <%--<li type='separator'></li>--%>
                    <%--<li><a id='delete_selected_item'>Delete selected</a></li>--%>
                    <%--<li><a>Change status of selected</a>--%>
                    <%--<ul>--%>
                    <%--<c:forEach var="status" items="${allStatusList}">--%>
                    <%--<li><a class='none'--%>
                    <%--id="change_status_selected_item-${status.replaceAll(" ","_")}">${status}--%>
                    <%--</a></li>--%>
                    <%--</c:forEach>--%>
                    <%--</ul>--%>
                    <%--</li>--%>
                </ul>
            </li>
        </g:if>

    <%--</ul>--%>
    <%--</li>--%>
        <g:if test="${username!=null}">
            <li><a id="user_item"><span class='usericon'></span>${username}
            </a>
                <ul id="user_menu">
                    <li><a id="logout_item">Logout</a></li>
                </ul>
            </li>
        </g:if>
        <li><a id="help_item">Help</a>
            <ul id="help_menu">
                <li><a id='web_services_api'>Web Services API</a></li>
                <li><a id='apollo_users_guide'>Apollo User's Guide</a></li>
                %{--<li><g:include page="version.jsp"></jsp:include></li>--}%
            </ul>
        </li>
    </ul>
</div>

<div id="list-track" class="content scaffold-list" role="main">
    <h1><g:message code="default.list.label" args="[entityName]"/></h1>
    <g:select name="organism" from="${org.bbop.apollo.Organism.list()}"
              optionValue="commonName"/>
    <br/>
    <br/>
    <g:if test="${flash.message}">
        <div class="message" role="status">${flash.message}</div>
    </g:if>
    <table>
        <thead>
        <tr>

            %{--<th><g:message code="track.organism.label" default="Organism" /></th>--}%

            <g:sortableColumn property="name" title="${message(code: 'track.name.label', default: 'Name')}"/>
            <g:sortableColumn property="organism.name"
                              title="${message(code: 'track.name.label', default: 'Organism')}"/>

        </tr>
        </thead>
        <tbody>
        <g:each in="${sequenceInstanceList}" status="i" var="sequenceInstance">
            <tr class="${(i % 2) == 0 ? 'even' : 'odd'}">

                <td><g:link action="show"
                            id="${sequenceInstance.id}">${fieldValue(bean: sequenceInstance, field: "name")}</g:link></td>
                <td>${sequenceInstance.organism.commonName}
                <g:link uri="">Browse</g:link>
                </td>

            </tr>
        </g:each>
        </tbody>
    </table>

    <div class="pagination">
        <g:paginate total="${trackInstanceCount ?: 0}"/>
    </div>
</div>
</body>
</html>
