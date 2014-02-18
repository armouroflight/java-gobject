
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
/**
 * 
 */
package org.gnome.gir.repository;

import com.sun.jna.Pointer;
import com.sun.jna.Union;

public class Argument extends Union {
	  public Byte v_int8;
	  public Short v_int16;
	  public Integer v_int32;
	  public Long v_int64;
	  public Float v_float;
	  public Double v_double;
	  public String v_string;
	  public Pointer v_pointer;
	  
	  public Object toJava(TypeTag tag) {
		switch (tag) {
		case BOOLEAN:
			setType(Integer.class);
			read();
			return (boolean) (v_int32 != 0);
		case INT:
		case INT32:
		case UINT32:
			setType(Integer.class);
			read();
			return v_int32;
		case INT64:
		case UINT64:
			setType(Long.class);
			read();
			return v_int64;
		case UTF8:
		case FILENAME:
			setType(String.class);
			read();
			return v_string;
		case DOUBLE:
			return v_double;
		default:
			throw new RuntimeException("Unhandled constant with tag " + tag);
		}
	  }
}