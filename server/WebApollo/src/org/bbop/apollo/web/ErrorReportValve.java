package org.bbop.apollo.web;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

public class ErrorReportValve extends org.apache.catalina.valves.ErrorReportValve {

	@Override
	protected void report(Request request, Response response, Throwable throwable) {
		if (response.getStatus() != HttpServletResponse.SC_BAD_REQUEST) { 
			super.report(request, response, throwable);
			return;
		}
		try {
			response.getReporter().write(response.getMessage());
		}
		catch (IOException e) {
		}
	}

}
