package org.bbop.apollo.web.name;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;

import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;

import org.bbop.apollo.editor.session.AnnotationSession;

public class HttpSessionTimeStampNameAdapter extends AbstractNameAdapter {

	private HttpSession httpSession;
	private AnnotationSession annotationSession;
	private MessageDigest digest;
	private static Random rng = new SecureRandom();
	
	public HttpSessionTimeStampNameAdapter(HttpSession session, AnnotationSession annotationSession) {
		this.httpSession = session;
		this.annotationSession = annotationSession;
		try {
			digest = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String generateUniqueName() {
		String uniqueName;
		do {
			uniqueName = DatatypeConverter.printHexBinary(digest.digest((httpSession.getId() + System.nanoTime() + rng.nextLong()).getBytes()));
		}
		while (annotationSession.getFeatureByUniqueName(uniqueName) != null);
		return uniqueName;
	}

}
