%{--<head>--}%
%{--<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">--}%
%{--<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">--}%
%{--<title><g:layoutTitle default="Apollo"/></title>--}%
%{--<meta name="viewport" content="width=device-width, initial-scale=1.0">--}%

%{--<link rel="apple-touch-icon" href="${assetPath(src: 'apple-touch-icon.png')}">--}%
%{--<link rel="apple-touch-icon" sizes="114x114" href="${assetPath(src: 'apple-touch-icon-retina.png')}">--}%
%{--<asset:stylesheet src="application.css"/>--}%
%{--<asset:javascript src="application.js"/>--}%
%{--<script src="https://cdnjs.com/libraries/svg.js"></script>--}%
<script type="application/javascript" src="../js/svg.js"></script>
%{--<asset:link rel="shortcut icon" href="webapollo_favicon.ico" type="image/x-icon"/>--}%
%{--<link rel="shortcut icon" href="${assetPath(src: 'favicon.ico')}" type="image/x-icon">--}%

%{--<g:include view="google_analytics.gsp"/>--}%
%{--<g:layoutHead/>--}%
%{--</head>--}%

<ul>
    <g:set var="max" value="${sequences.first()}"/>
    <g:set var="min" value="${sequences.last()}"/>
    Max [${max.length}]
    Min [${min.length}]
    <g:each in="${sequences}" var="seq">
        <li>${seq.name}</li>
    </g:each>

    ${array}
</ul>

<div id="drawing"></div>

<script>
    var draw = SVG('drawing').size(300, 130)
    var count = 0
    // var ellipse = draw.ellipse(150, 100).fill('#f06').move(20, 20)
</script>


<g:each in="${sequences}" var="seq">
    <script>
        var rect = draw.rect(20 , 100);
        rect.radius(10);
        rect.move(count * 30);
        count += 1 ;
    </script>
</g:each>
