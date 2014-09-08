package org.bbop.apollo.web;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
