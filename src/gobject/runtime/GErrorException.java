
/* 
 * Copyright (c) 2008 Colin Walters <walters@verbum.org>
 * 
 * This file is part of java-gobject-introspection.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the 
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, 
 * Boston, MA  02111-1307  USA.
 *
 */

package gobject.runtime;

import gobject.internals.GErrorStruct;

/**
 * Exception thrown by native methods which use the {@code GError} mechanism.
 * 
 * @see <a href="../../gtk-doc/html/glib/glib-Error-Reporting.html">GError</a>
 */
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