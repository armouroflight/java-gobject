package org.gnome.gir.repository;

import com.sun.jna.PointerType;

public class ErrorDomainInfo extends PointerType {
	String getQuark() {
		return Repository.getNativeLibrary().g_error_domain_info_get_quark(this);
	}
	
	InterfaceInfo getInterfaceInfo() {
		return Repository.getNativeLibrary().g_error_domain_info_get_codes(this);
	}
}
