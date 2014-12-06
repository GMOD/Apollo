<!DOCTYPE html>
<html ng-app="x">

<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width">

    <title>UI.Layout : holy grail demo</title>

    <link rel="stylesheet" href="https://rawgithub.com/angular-ui/ui-layout/v0.0.0/ui-layout.css">
    %{--<link rel="stylesheet" href="style.css">--}%
    <style>
    /* Styles go here */


    .html-back{
        background : #eee url("http://placehold.it/400x300/eee/666&text=HTML") no-repeat center;

    }
    .css-back{
        background : #eee url("http://placehold.it/400x300/eee/666&text=CSS") no-repeat center;

    }
    .js-back{
        background : #eee url("http://placehold.it/400x300/eee/666&text=JS") no-repeat center;
    }

    </style>
</head>

<body>

<div ui-layout  >
    <div class=" html-back" ></div>

    <div ui-layout="{flow : 'column'}" >
        <div class=" html-back" ></div>
        <div class=" js-back" ></div>
        <div class=" css-back" ></div>
    </div>

    <div class=" css-back" ></div>
</div>

<!-- Le javascript -->
<script type="application/javascript" src="https://ajax.googleapis.com/ajax/libs/angularjs/1.2.10/angular.min.js"></script>
<script type="application/javascript" src="https://rawgithub.com/angular-ui/ui-layout/v0.0.0/ui-layout.min.js"></script>
<script>
    angular.module('x', ['ui.layout']);
</script>
</body>

</html>