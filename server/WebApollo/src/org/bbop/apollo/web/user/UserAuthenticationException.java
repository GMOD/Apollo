package org.bbop.apollo.web.user;

public class UserAuthenticationException extends Exception {

	private static final long serialVersionUID = 1L;

	public UserAuthenticationException(String message) {
		super(message);
	}
	
	public UserAuthenticationException(Throwable e) {
		super(e);
	}
	
}
