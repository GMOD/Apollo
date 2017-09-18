package org.bbop.apollo

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class NcbiProxyServiceController {

    private static String SEARCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
    // only returns title if xml, not json
    private static String FETCH_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?retmode=xml";
    /**
     * Request URL:http://demo.genomearchitect.org/WebApolloDemoStaging/ProxyService?proxy=eutils&operation=fetch&db=pubmed&id=PMC3013679

     * @return
     */
    def index() {
        String db = params.db
        String id = params.id
        String operation = params.operation
        String urlString

        switch (operation) {
            case "search":
                urlString = SEARCH_URL + "?db=" + db + "&term=" + id + "[uid]"
                break;
            case "fetch":
                urlString = FETCH_URL + "&db=" + db + "&id=" + id + "";
//                urlString = FETCH_URL + "?db=" + db + "&id=" + id+"&retmode=json"
                break;
            default:
                throw new AnnotationException("EUtils operation ${operation} unknown")
                break;
        }

        URL url = new URL(urlString)
//        String returnText = url.text

        // TODO: make
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        JSONObject returnObject = FormatUtil.convertFromXMLToJSON(docBuilder.parse(url.openStream()))
        render returnObject as JSON
    }
}
