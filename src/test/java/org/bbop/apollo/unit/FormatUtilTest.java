package org.bbop.apollo.unit;

import junit.framework.TestCase;
import org.bbop.apollo.web.util.FormatUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class FormatUtilTest extends TestCase {
    
    public void testConvertFromXMLToJSON() throws ParserConfigurationException, SAXException, IOException, JSONException {
        Document xml = getXMLDocument();
        JSONObject json = FormatUtil.convertFromXMLToJSON(xml);
        System.out.println(json);
    }

    private Document getXMLDocument() throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
//        return db.parse("testSupport/efetch_error.xml");
        return db.parse("src/test/resources/testSupport/mapping.xml");
    }
    
}
