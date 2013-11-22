package org.bbop.apollo.web.user;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface UserAuthentication {

	public void generateUserLoginPage(HttpServlet servlet, HttpServletRequest request, HttpServletResponse response) throws ServletException;
	
	public String validateUser(HttpServletRequest request, HttpServletResponse response) throws UserAuthenticationException;
	
	public String getUserLoginPageURL();
	
	public String getAddUserURL();
}
