package gobject.internals;

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

	public Object fromNative(Object nativeValue, FromNativeContext context) {
		return new GQuark((Integer) nativeValue);
	}

	public Class<?> nativeType() {
		return Integer.class;
	}

	public Object toNative() {
		return Integer.valueOf(value);
	}
}
