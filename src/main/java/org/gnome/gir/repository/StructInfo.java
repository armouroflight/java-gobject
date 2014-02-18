package org.gnome.gir.repository;


public class StructInfo extends RegisteredTypeInfo {
	protected StructInfo(Initializer init) {
		super(init);
	}		
	public FieldInfo[] getFields() {
		int n = GIntrospectionAPI.gi.g_struct_info_get_n_fields(this);
		FieldInfo[] ret = new FieldInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_struct_info_get_field(this, i);
		return ret;
	}
	
	public FunctionInfo[] getMethods() {
		int n = GIntrospectionAPI.gi.g_struct_info_get_n_methods(this);
		FunctionInfo[] ret = new FunctionInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_struct_info_get_method(this, i);
		return ret;
	}
	
	public boolean isGTypeStruct() {
		return GIntrospectionAPI.gi.g_struct_info_is_gtype_struct(this);
	}
}
