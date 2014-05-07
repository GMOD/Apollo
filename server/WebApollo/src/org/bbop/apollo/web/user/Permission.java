package org.bbop.apollo.web.user;

public class Permission {

	public final static int NONE = 0x0;
	public final static int READ = 0x1;
	public final static int WRITE = 0x2;
	public final static int PUBLISH = 0x4;
	public final static int USER_MANAGER = 0x8;
	public final static int ADMIN = USER_MANAGER;

	public static int getValueForPermission(String permission) {
		if (permission.equalsIgnoreCase("none")) {
			return NONE;
		}
		if (permission.equalsIgnoreCase("read")) {
			return READ;
		}
		if (permission.equalsIgnoreCase("write")) {
			return WRITE;
		}
		if (permission.equalsIgnoreCase("publish")) {
			return PUBLISH;
		}
		if (permission.equalsIgnoreCase("user_manager")) {
			return USER_MANAGER;
		}
		if (permission.equalsIgnoreCase("admin")) {
			return ADMIN;
		}
		return -1;
	}
}
