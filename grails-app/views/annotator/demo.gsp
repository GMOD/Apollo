<!-- The DOCTYPE declaration above will set the     -->
<!-- browser's rendering engine into                -->
<!-- "Standards Mode". Replacing this declaration   -->
<!-- with a "Quirks Mode" doctype is not supported. -->

<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>

    <meta name="layout" content="main">
    %{--<meta name="layout" content="main"/>--}%
    <title>Annotator</title>

</head>

<body style="background-color: white;">

<div style="padding: 20px;margin: 30px;">
    <div class="dropdown">
        <button class="btn btn-sm dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown"
                aria-expanded="true">
            Export
            <span class="caret"></span>
        </button>
        <ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu1">
            <li role="presentation"><a role="menuitem" tabindex="-1" href="#">GFF3</a></li>
            <li role="presentation"><a role="menuitem" tabindex="-1" href="#">FASTA</a></li>
            <li role="presentation"><a role="menuitem" tabindex="-1" href="#">Chado</a></li>
        </ul>
    </div>
</div>

<br/>
<br/>
<br/>
<br/>

</body>
</html>
