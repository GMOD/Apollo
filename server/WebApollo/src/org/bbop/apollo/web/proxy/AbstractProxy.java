package org.bbop.apollo.web.proxy;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

public abstract class AbstractProxy {

	public abstract void fetch(Map<String, String[]> parameters, HttpServletResponse response) throws ProxyException;
	
	protected String getParameter(Map<String, String[]> parameters, String parameter) {
		String[] p = parameters.get(parameter);
		if (p == null) {
			return null;
		}
		return p[0];
	}
	
	public class ProxyException extends Exception {
		
		private static final long serialVersionUID = 1L;

		public ProxyException(String message) {
			super(message);
		}
		
	}
	
}
