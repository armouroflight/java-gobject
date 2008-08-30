
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
	public boolean isZeroTerminated() {
		return Repository.getNativeLibrary().g_type_info_is_zero_terminated(this);		
	}
	public int getNErrorDomains() {
		return Repository.getNativeLibrary().g_type_info_get_n_error_domains(this);		
	}
	public ErrorDomainInfo getErrorDomainInfo(int n) {
		return Repository.getNativeLibrary().g_type_info_get_error_domain(this, n);
	}
}