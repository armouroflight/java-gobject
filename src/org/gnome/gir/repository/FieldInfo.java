
package org.gnome.gir.repository;

import com.sun.jna.PointerType;

public class FieldInfo extends PointerType {
	public FieldInfoFlags getFlags() {
		return Repository.getNativeLibrary().g_field_info_get_flags(this);
	}
	
	public int getSize() {
		return Repository.getNativeLibrary().g_field_info_get_size(this);
	}
	
	public int getOffset() {
		return Repository.getNativeLibrary().g_field_info_get_offset(this);
	}
	
	public TypeInfo getType() {
		return Repository.getNativeLibrary().g_field_info_get_type(this);		
	}
}