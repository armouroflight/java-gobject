package org.gnome.gir.compiler;

import com.sun.jna.FromNativeContext;
import com.sun.jna.NativeMapped;

public enum TestEnum implements NativeMapped {
	FOO,
	BAR;

	private TestEnum() {}
	
	@Override
	public Object fromNative(Object nativeValue, FromNativeContext context) {
		Integer val = (Integer) nativeValue;
		return values()[val];
	}

	@Override
	public Class<?> nativeType() {
		return Integer.class;
	}

	@Override
	public Object toNative() {
		return Integer.valueOf(ordinal());
	}
}
