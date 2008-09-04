package org.gnome.gir.gobject;

import com.sun.jna.FromNativeContext;
import com.sun.jna.NativeMapped;

public class GQuark implements NativeMapped {
    private final int value;
    public GQuark(int value) {
        this.value = value;
    }
    public int intValue() {
        return value;
    }
    
    public GQuark valueOf(String quark) {
        return GObjectAPI.gobj.g_quark_from_string(quark);
    }
    
    @Override
    public String toString() {
        return GObjectAPI.gobj.g_quark_to_string(this);
    }
	@Override
	public Object fromNative(Object nativeValue, FromNativeContext context) {
		return new GQuark((Integer) nativeValue);
	}
	@Override
	public Class<?> nativeType() {
		return Integer.class;
	}
	@Override
	public Object toNative() {
		return Integer.valueOf(value);
	}
}
