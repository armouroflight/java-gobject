package org.gnome.gir.repository;


public class EnumInfo extends RegisteredTypeInfo {
	protected EnumInfo(Initializer init) {
		super(init);
	}	
	public ValueInfo[] getValueInfo() {
		int n = GIntrospectionAPI.gi.g_enum_info_get_n_values(this);
		ValueInfo[] ret= new ValueInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_enum_info_get_value(this, i);
		return ret;
	}
}
