package org.bbop.apollo.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bbop.apollo.editor.AnnotationEditor.AnnotationEditorException;
import org.bbop.apollo.tools.seq.search.SequenceSearchToolException;
import org.bbop.apollo.web.AnnotationEditorServiceManager.AnnotationEditorServiceException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class AnnotationEditorService
 */
@WebServlet("/AnnotationEditorService")
public class AnnotationEditorService extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
     * @see HttpServlet#HttpServlet()
     */
    public AnnotationEditorService() {
        super();
    }

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		AnnotationEditorServiceManager.setServletContext(getServletContext());
	}

	@Override
	public void destroy() {
		AnnotationEditorServiceManager.getInstance().destroy();
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession(false);
		try {
			response.setContentType("application/json");
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
			AnnotationEditorServiceManager.getInstance().processRequest(request.getInputStream(), out, session);
			out.flush();
		} catch (JSONException e) {
			try {
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		} catch (AnnotationEditorException e) {
			try {
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
		}
			catch (JSONException e2) {
			}
		} catch (AnnotationEditorServiceException e) {
			try {
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		} catch (SQLException e) {
			try {
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		} catch (SequenceSearchToolException e) {
			try {
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		} catch (Exception e) {
			try {
				sendError(response, HttpServletResponse.SC_BAD_REQUEST, new JSONObject().put("error", e.getMessage()).toString());
			}
			catch (JSONException e2) {
			}
		}
	}
	
	private void sendError(HttpServletResponse response, int statusCode, String message) {
		try {
			response.getOutputStream().print(message);
			response.setStatus(statusCode);
		}
		catch (Exception e) {
		}
	}

}
