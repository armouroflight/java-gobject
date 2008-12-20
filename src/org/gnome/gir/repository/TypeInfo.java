
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

package org.gnome.gir.repository;

import com.sun.jna.PointerType;

public class TypeInfo extends PointerType {
	public boolean isPointer() {
		return Repository.getNativeLibrary().g_type_info_is_pointer(this);
	}
	public TypeTag getTag() {
		return Repository.getNativeLibrary().g_type_info_get_tag(this);
	}
	public TypeInfo getParamType(int n) {
		return Repository.getNativeLibrary().g_type_info_get_param_type(this, n);
	}
	public BaseInfo getInterface() {
		return Repository.getNativeLibrary().g_type_info_get_interface(this);
	}		
	public int getArrayLength() {
		return Repository.getNativeLibrary().g_type_info_get_array_length(this);		
	}
	public int getArrayFixedSize() {
		return Repository.getNativeLibrary().g_type_info_get_array_fixed_size(this);		
	}	
	public boolean isZeroTerminated() {
		return Repository.getNativeLibrary().g_type_info_is_zero_terminated(this);		
	}
	public int getNErrorDomains() {
		return Repository.getNativeLibrary().g_type_info_get_n_error_domains(this);		
	}
	public ErrorDomainInfo getErrorDomainInfo(int n) {
		return Repository.getNativeLibrary().g_type_info_get_error_domain(this, n);
	}
	
	public String toString() {
		return String.format("<TypeInfo tag=%s pointer=%s>", getTag(), isPointer());
	}
}