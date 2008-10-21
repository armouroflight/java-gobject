package org.gnome.gir.repository;


public class ObjectInfo extends RegisteredTypeInfo {
	protected ObjectInfo(Initializer init) {
		super(init);
	}	
	
	public String getTypeName() {
		return GIntrospectionAPI.gi.g_object_info_get_type_name(this);
	}
	
	public ObjectInfo getParent() {
		return GIntrospectionAPI.gi.g_object_info_get_parent(this);
	}
	
	public boolean isAbstract() {
		return GIntrospectionAPI.gi.g_object_info_get_abstract(this);
	}
	
	public boolean isAssignableFrom(ObjectInfo other) {
		if (other == this)
			return true;
		BaseInfo otherBase = other;
		while (true) {
			if (otherBase == null)
				return false;
			if (otherBase.getNamespace().equals(getNamespace()) && 
					otherBase.getName().equals(getName()))
				return true;
			otherBase = ((ObjectInfo)otherBase).getParent();
		}
	}
	
	public FunctionInfo[] getMethods() {
		int n = GIntrospectionAPI.gi.g_object_info_get_n_methods(this);
		FunctionInfo[] ret= new FunctionInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_object_info_get_method(this, i);
		return ret;
	}
	
	public SignalInfo[] getSignals() {
		int n = GIntrospectionAPI.gi.g_object_info_get_n_signals(this);
		SignalInfo[] ret= new SignalInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_object_info_get_signal(this, i);
		return ret;
	}
	
	public PropertyInfo[] getProperties() {
		int n = GIntrospectionAPI.gi.g_object_info_get_n_properties(this);
		PropertyInfo[] ret= new PropertyInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_object_info_get_property(this, i);
		return ret;
	}	
	
	public InterfaceInfo[] getInterfaces() {
		int n = GIntrospectionAPI.gi.g_object_info_get_n_interfaces(this);
		InterfaceInfo[] ret= new InterfaceInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = GIntrospectionAPI.gi.g_object_info_get_interface(this, i);
		return ret;
	}		

	public String getTypeInit() {
		return GIntrospectionAPI.gi.g_object_info_get_type_init(this);
	}
}
