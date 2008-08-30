
package org.gnome.gir.gobject;

public class GErrorException extends Exception {
	private static final long serialVersionUID = 1L;
	private int domain;
	private int code;
	private String message;
	public GErrorException() {
	}
	public GErrorException(GErrorStruct error) {
		super(error.message);
		this.domain = error.getDomain();
		this.code = error.getCode();
		this.message = error.getMessage();
	}
	
	public int getDomain() {
		return domain;
	}
	
	public int getCode() {
		return code;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
}