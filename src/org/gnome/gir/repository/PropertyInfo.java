package org.gnome.gir.repository;


public class PropertyInfo extends BaseInfo {

	protected PropertyInfo(Initializer init) {
		super(init);
	}

	public int getFlags() {
		return GIntrospectionAPI.gi.g_property_info_get_flags(this);
	}
	
	public TypeInfo getType() {
		return GIntrospectionAPI.gi.g_property_info_get_type(this);
	}
}
