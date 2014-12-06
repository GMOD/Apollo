<!DOCTYPE html>
<html ng-app="AnnotatorApplication">

<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width">

    <title>UI.Layout : holy grail demo</title>


    %{--<asset:javascript library="jquery" plugin="jquery"/>--}%
    <asset:javascript src="annotator/controllers/Annotator.js"/>
    <asset:stylesheet src="annotator.css"/>
</head>

<body>

<div ui-layout  >
    <div class=" html-back" ></div>

    <div class="left-cell" ui-layout="{flow : 'column'}" >
        <div>
            <form>
                <table>
                    <tr>
                        <th>A</th>
                        <th>B</th>
                        <th>B</th>
                    </tr>
                    <tr>
                    <td>1</td>
                        <td>2</td>
                        <td>3</td>
                    </tr>
                </table>
            </form>

        </div>
        <div class=" js-back" ></div>
        <div class=" css-back" ></div>
    </div>

    <div class=" css-back" ></div>
</div>

<!-- Le javascript -->
%{--<script type="application/javascript" src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.10/angular.min.js"></script>--}%
<script>
    angular.module('AnnotatorApplication', ['ui.layout']);
</script>
</body>

</html>