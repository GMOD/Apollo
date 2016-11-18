<%--
  Created by IntelliJ IDEA.
  User: nathandunn
  Date: 11/17/16
  Time: 4:05 PM
--%>

<g:each var="google_analytics_id" in="${grailsApplication.config.apollo.google_analytics}">
    <script>
        (function (i, s, o, g, r, a, m) {
            i['GoogleAnalyticsObject'] = r;
            i[r] = i[r] || function () {
                        (i[r].q = i[r].q || []).push(arguments)
                    }, i[r].l = 1 * new Date();
            a = s.createElement(o),
                    m = s.getElementsByTagName(o)[0];
            a.async = 1;
            a.src = g;
            m.parentNode.insertBefore(a, m)
        })(window, document, 'script', '//www.google-analytics.com/analytics.js', 'ga');

        ga('create', '${google_analytics_id}', 'auto');
        ga('send', 'pageview');

    </script>
</g:each>
