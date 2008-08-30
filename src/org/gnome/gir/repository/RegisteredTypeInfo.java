package org.gnome.gir.repository;


public class RegisteredTypeInfo extends BaseInfo {
	protected RegisteredTypeInfo(Initializer init) {
		super(init);
	}
	
	public String getTypeInit() {
		return GIntrospectionAPI.gi.g_registered_type_info_get_type_init(this);
	}
}
