package org.bbop.apollo.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bbop.apollo.web.proxy.AbstractProxy;
import org.bbop.apollo.web.proxy.AbstractProxy.ProxyException;
import org.bbop.apollo.web.proxy.EUtilsProxy;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class ProxyService
 */
@WebServlet("/ProxyService")
public class ProxyService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static Map<String, AbstractProxy> proxies;
	
    public ProxyService() throws ProxyException {
        super();
        proxies = new HashMap<String, AbstractProxy>();
        proxies.put("eutils", new EUtilsProxy());
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String proxyName = request.getParameter("proxy");
		if (proxyName == null) {
			sendError(response, "Missing proxy parameter");
			return;
		}
		AbstractProxy proxy = proxies.get(proxyName);
		if (proxy == null) {
			sendError(response, "Unknown proxy: " + proxyName);
			return;
		}
		try {
			proxy.fetch(request.getParameterMap(), response);
		} catch (ProxyException e) {
			sendError(response, "Error running proxy: " + e.getMessage());
			return;
		}
	}
	
	private void sendError(HttpServletResponse response, String message) {
		try {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", message).toString());
		}
		catch (JSONException e) {
		}
		catch (IOException e) {
		}
	}

}
