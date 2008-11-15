package org.gnome.gir.repository;


public class InterfaceInfo extends RegisteredTypeInfo {
	protected InterfaceInfo(Initializer init) {
		super(init);
	}
	
	public BaseInfo[] getPrerequisites() {
		int n = GIntrospectionAPI.gi.g_interface_info_get_n_prerequisites(this);
		BaseInfo[] ret= new BaseInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_interface_info_get_prerequisite(this, i);
		return ret;
	}
	
	public PropertyInfo[] getProperties() {
		int n = GIntrospectionAPI.gi.g_interface_info_get_n_properties(this);
		PropertyInfo[] ret= new PropertyInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_interface_info_get_property(this, i);
		return ret;
	}	
	
	public FunctionInfo[] getMethods() {
		int n = GIntrospectionAPI.gi.g_interface_info_get_n_methods(this);
		FunctionInfo[] ret= new FunctionInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_interface_info_get_method(this, i);
		return ret;
	}
	
	public SignalInfo[] getSignals() {
		int n = GIntrospectionAPI.gi.g_interface_info_get_n_signals(this);
		SignalInfo[] ret= new SignalInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_interface_info_get_signal(this, i);
		return ret;
	}
	
	public VFuncInfo[] getVFuncs() {
		int n = GIntrospectionAPI.gi.g_interface_info_get_n_vfuncs(this);
		VFuncInfo[] ret= new VFuncInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_interface_info_get_vfunc(this, i);
		return ret;
	}
	
	public ConstantInfo[] getContants() {
		int n = GIntrospectionAPI.gi.g_interface_info_get_n_constants(this);
		ConstantInfo[] ret= new ConstantInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_interface_info_get_constant(this, i);
		return ret;
	}		
}
