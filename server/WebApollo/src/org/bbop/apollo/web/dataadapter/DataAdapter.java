package org.bbop.apollo.web.dataadapter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.bbop.apollo.web.config.ServerConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public abstract class DataAdapter {
	
	public void init(ServerConfiguration serverConfiguration, String configPath, String basePath) throws DataAdapterException {
	}

	public abstract void read(List<String> tracks, Map<String, String[]> parameters, HttpServletResponse response) throws IOException;
	
	public abstract void write(List<String> tracks, Map<String, String[]> parameters, HttpServletResponse response) throws IOException;

	protected String getParameter(Map<String, String[]> parameters, String parameter) {
		String[] p = parameters.get(parameter);
		if (p == null) {
			return null;
		}
		return p[0];
	}
	
	protected Document getXMLDocument(String basePath, String configPath) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(basePath + "/" + configPath);
	}
	
	public class DataAdapterException extends Exception {
		
		public DataAdapterException(String message) {
			super(message);
		}
		
	}

}
