package org.bbop.apollo.web.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CannedComments {
	
	private Map<String, Collection<String> > cannedComments;
	
	public CannedComments(InputStream config) throws ParserConfigurationException, SAXException, IOException {
		cannedComments = new HashMap<String, Collection<String> >();
		parseConfiguration(config);
	}
	
	private void parseConfiguration(InputStream config) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(config);
		Element cannedCommentsNode = (Element)doc.getElementsByTagName("canned_comments").item(0);
		if (cannedCommentsNode != null) {
			NodeList cannedCommentList = cannedCommentsNode.getElementsByTagName("comment");
			if (cannedCommentList != null) {
				for (int i = 0; i < cannedCommentList.getLength(); ++i) {
					Collection<String> featureTypes = new ArrayList<String>();
					Element commentNode = (Element)cannedCommentList.item(i);
					String featureTypeAttr = commentNode.getAttribute("feature_type");
					if (featureTypeAttr.length() > 0) {
						featureTypes.add(featureTypeAttr);
					}
					String featureTypesAttr = commentNode.getAttribute("feature_types");
					if (featureTypesAttr.length() > 0) {
						for (String featureType : featureTypesAttr.split(",")) {
							featureTypes.add(featureType);
						}
					}
					for (String type : featureTypes) {
						Collection<String> cannedCommentsForFeatureType = cannedComments.get(type);
						if (cannedCommentsForFeatureType == null) {
							cannedCommentsForFeatureType = new ArrayList<String>();
							cannedComments.put(type, cannedCommentsForFeatureType);
						}
						cannedCommentsForFeatureType.add(commentNode.getTextContent());
					}
				}
			}
		}
	}
	
	public Collection<String> getCannedCommentsForType(String type) {
		return cannedComments.get(type);
	}
	
}