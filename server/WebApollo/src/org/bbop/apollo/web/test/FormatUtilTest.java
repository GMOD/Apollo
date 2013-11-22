package org.bbop.apollo.web.test;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bbop.apollo.web.util.FormatUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class FormatUtilTest extends TestCase {
	
	public void testConvertFromXMLToJSON() throws ParserConfigurationException, SAXException, IOException, JSONException {
		Document xml = getXMLDocument();
		JSONObject json = FormatUtil.convertFromXMLToJSON(xml);
		System.out.println(json);
	}

	private Document getXMLDocument() throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
//		return db.parse("testSupport/efetch_error.xml");
		return db.parse("testSupport/mapping.xml");
	}
	
}
