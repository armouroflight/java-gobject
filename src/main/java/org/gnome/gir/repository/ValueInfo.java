package org.gnome.gir.repository;


public class ValueInfo extends BaseInfo {
	protected ValueInfo(Initializer init) {
		super(init);
	}	
	public long getValue() {
		return Repository.getNativeLibrary().g_value_info_get_value(this).longValue();
	}
}
