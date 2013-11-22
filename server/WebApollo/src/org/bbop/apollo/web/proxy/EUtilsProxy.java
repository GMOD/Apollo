package org.bbop.apollo.web.proxy;

import java.util.Map;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bbop.apollo.web.util.FormatUtil;

public class EUtilsProxy extends AbstractProxy {

	private static String SEARCH_URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";
	private static String FETCH_URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?retmode=xml";
	private DocumentBuilder docBuilder;
	
	public EUtilsProxy() throws ProxyException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			docBuilder = dbf.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) {
			throw new ProxyException(e.getMessage());
		}
	}
	
	@Override
	public void fetch(Map<String, String[]> parameters, HttpServletResponse response) throws ProxyException {
		String db = getParameter(parameters, "db");
		if (db == null) {
			throw new ProxyException("Missing db parameter");
		}
		String id = getParameter(parameters, "id");
		if (id == null) {
			throw new ProxyException("Missing id parameter");
		}
		String operation = getParameter(parameters, "operation");
		if (operation == null) {
			throw new ProxyException("Missing operation parameter");
		}
		URL url;
		try {
			if (operation.equals("search")) {
				url = new URL(SEARCH_URL + "?db=" + db + "&term=" + id + "[uid]");
			}
			else if (operation.equals("fetch")) {
				url = new URL(FETCH_URL + "&db=" + db + "&id=" + id);
			}
			else {
				throw new ProxyException("Invalid operation: " + operation);
			}
			response.getWriter().print(FormatUtil.convertFromXMLToJSON(docBuilder.parse(url.openStream())).toString());
		}
		catch (Exception e) {
			throw new ProxyException(e.getMessage());
		}
	}

}
