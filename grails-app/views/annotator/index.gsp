<!-- The DOCTYPE declaration above will set the     -->
<!-- browser's rendering engine into                -->
<!-- "Standards Mode". Replacing this declaration   -->
<!-- with a "Quirks Mode" doctype is not supported. -->

<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>

    <meta name="layout" content="annotator2">
    %{--<meta name="layout" content="main"/>--}%
    <title>Annotator</title>

    <asset:javascript src="spring-websocket"/>

    <script type="text/javascript" language="javascript" src="annotator.nocache.js"></script>
    <link rel="stylesheet" href="styles.css">
    <link rel="stylesheet" href="footer.css">
    <script src="http://code.jquery.com/jquery-latest.min.js" type="text/javascript"></script>
    <script src="script.js"></script>
    <script>
        %{--rootUrl: '${applicationContext.servletContext.getContextPath()}'--}%
        var Options = {
            showFrame: '${params.showFrame  && params.showFrame == 'true' ? 'true' : 'false' }'
            ,userId: '${userKey}'
           ,top: "10"
          ,topUnit: "PCT" // PX, EM, PC, PT, IN, CM
          ,height: "80"
           ,heightUnit: "PCT" // PX, EM, PC, PT, IN, CM
        };
    </script>
</head>



<body style="background-color: white;">


    <?php include('header.php') ?>
  <div class='header'>
    <div class="navbar-header">
      <a class="logo navbar-btn pull-left" href="/" title="Home">
        <img src="https://i5k.nal.usda.gov/sites/all/themes/i5k_bootstrap/usda-logo.svg" alt="Home">
      </a>
      <a class="name navbar-brand" href="/" title="Home">i5k Workspace@NAL</a>      
    </div>
    <div id='cssmenu'><?php print display_menus(); ?> </div>
 </div> 

<center>
  <div id="footer" style="position: absolute; left: 0px; bottom: 0px; height: 65px; width: 100%; background-color: rgb(46,58,64); border-top: 1px solid rgb(105, 105, 105);">
<div class="container">
    <div class="region region-footer">
    <section id="block-block-2" class="block block-block clearfix">

      
  <div style="font-size: 85%; font-color: orange; ">
	<a href="http://www.nal.usda.gov">NAL Home</a> | <a href="http://www.usda.gov" target="_blank">USDA.gov</a> | <a href="http://www.ars.usda.gov" target="_blank">Agricultural Research Service</a> | <a href="http://www.usda.gov/wps/portal/usda/usdahome?navid=PLAIN_WRITING">Plain Language</a> | <a href="http://www.ars.usda.gov/Services/docs.htm?docid=1398" target="_blank">FOIA</a> | <a href="http://www.usda.gov/wps/portal/usda/usdahome?navtype=FT&amp;navid=ACCESSIBILITY_STATEM" target="_blank">Accessibility Statement</a> | <a href="http://www.ars.usda.gov/Main/docs.htm?docid=8040" target="_blank">Information Quality</a> | <a href="http://www.ars.usda.gov/disclaim.html#Privacy" target="_blank">Privacy Policy</a> | <a href="http://www.usda.gov/wps/portal/usda/usdahome?navtype=FT&amp;navid=NON_DISCRIMINATION" target="_blank">Non-Discrimination Statement</a> | <a href="http://www.usa.gov" target="_blank">USA.gov</a> | <a href="http://www.whitehouse.gov" target="_blank">White House</a>
<div>
<p>Please cite the use of our resources: <a href="http://nar.oxfordjournals.org/content/43/D1/D714" target="blank"> doi: 10.1093/nar/gku983</a></p>
</div>
</div>

</section> <!-- /.block -->
  </div>
  </div>
</div>
</center>

<!-- RECOMMENDED if your web app will not function without JavaScript enabled -->
<noscript>
    <div style="width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif">
        Your web browser must have JavaScript enabled
        in order for this application to display correctly.
    </div>
</noscript>

%{--Version <g:meta name="app.version"/>--}%
%{--Built with Grails <g:meta name="app.grails.version"/>--}%

</body>
</html>
