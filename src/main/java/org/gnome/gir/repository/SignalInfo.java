package org.gnome.gir.repository;


public class SignalInfo extends CallableInfo {
	protected SignalInfo(Initializer init) {
		super(init);
	}	
	public int getFlags() {
		return GIntrospectionAPI.gi.g_signal_info_get_flags(this);
	}
	
	public VFuncInfo getClassClosure() {
		return GIntrospectionAPI.gi.g_signal_info_get_class_closure(this);
	}
	
	public boolean trueStopsEmit() {
		return GIntrospectionAPI.gi.g_signal_info_true_stops_emit(this);
	}
}
