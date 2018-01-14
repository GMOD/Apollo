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
%{--<head>--}%
<title>${organism} Chromosome Browser</title>
<script type="application/javascript" src="../js/svg.js"></script>
%{--</head>--}%
%{--<asset:link rel="shortcut icon" href="webapollo_favicon.ico" type="image/x-icon"/>--}%
%{--<link rel="shortcut icon" href="${assetPath(src: 'favicon.ico')}" type="image/x-icon">--}%

%{--<g:include view="google_analytics.gsp"/>--}%
%{--<g:layoutHead/>--}%
%{--</head>--}%

%{--<ul>--}%
<g:set var="max" value="${sequences.first()}"/>
<g:set var="min" value="${sequences.last()}"/>
%{--Max [${max.length}]--}%
%{--Min [${min.length}]--}%
%{--<g:each in="${sequences}" var="seq">--}%
%{--<li>${seq.name}</li>--}%
%{--</g:each>--}%

%{--${array}--}%
%{--</ul>--}%

<div id="drawing"></div>

<script>
    var draw = SVG('drawing').size(880, 400);
    var sequenceIndex = 0
    var maxHeight = 250;
    var blockHeight = 5;
    var radius = 10;
    var rectWidth = 20;
    var spacing = 30;
    var maxLength = ${max.length};
    var lengthPerBlock = Math.round( maxLength / maxHeight) ;

</script>



<g:each in="${sequences}" var="seq">
    <script>
        var sequenceLinkString = '../annotator/loadLink?loc=${seq.name}:0..${seq.length}&organism=${organism.id}';
        console.log(sequenceLinkString);
        var sequenceLink = draw.link(sequenceLinkString);
        var label = sequenceLink.text('${seq.name}');

        var textMove = 20;
        label.move(sequenceIndex * spacing + spacing / 2.0 - textMove, maxHeight + 80).font({
            fill: 'gray',
            family: 'Arial',
            size: 12
        });
        label.transform({rotation: -90});
        // label.move(10);

        var height = (maxHeight * ${seq.length /  max.length});

        var numBlocks = height / blockHeight;
        for (var blockIndex = 0; blockIndex < numBlocks; ++blockIndex) {
            var loc = '${seq.name}:' + (blockIndex * blockHeight * lengthPerBlock) + '..' + ((blockIndex + 1) * blockHeight * lengthPerBlock);
            var link = draw.link('../annotator/loadLink?loc=' + loc+'&organism=${organism.id}');
            var rect = link.rect(rectWidth, blockHeight);
            // rect.radius(radius);
            // rect.move(count * spacing + spacing / 2.0, maxHeight - height);
            rect.move(sequenceIndex * spacing + spacing / 2.0, maxHeight - blockIndex * blockHeight);
            rect.fill({
                color: 'gray'
            }).stroke({color: 'black'});
        }

        sequenceIndex += 1;

    </script>
</g:each>
