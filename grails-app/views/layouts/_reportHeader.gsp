<nav class="navbar navbar-default">
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
                        %{--<li>Organism Annotations</li>--}%
                        <li><g:link action="report" controller="organism">Organism Annotations</g:link></li>
                    </ul>
                </div>
            </div>

        </div>
    </div>
</nav>
