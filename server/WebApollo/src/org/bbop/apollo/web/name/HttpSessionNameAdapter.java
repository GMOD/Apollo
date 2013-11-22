package org.bbop.apollo.web.name;

import javax.servlet.http.HttpSession;

import org.bbop.apollo.editor.AbstractAnnotationSession;

public class HttpSessionNameAdapter extends AbstractNameAdapter {

	private HttpSession httpSession;
	private AbstractAnnotationSession annotationSession;
	
	public HttpSessionNameAdapter(HttpSession session, AbstractAnnotationSession annotationSession) {
		this.httpSession = session;
		this.annotationSession = annotationSession;
	}
	
	@Override
	public String generateUniqueName() {
		int counter = Integer.parseInt(httpSession.getAttribute("uniquenameCounter").toString());
		String uniqueName;
		do {
			++counter;
			uniqueName = httpSession.getId().toString() + "-" + counter;
		} while (annotationSession.getFeatureByUniqueName(uniqueName) != null);
		httpSession.setAttribute("uniquenameCounter", counter);
		return httpSession.getId().toString() + "-" + counter;
	}

}
