

import com.sun.jna.FromNativeContext;

public enum TestEnum implements com.sun.jna.NativeMapped {
	FOO,
	BAR;

	private static int offset = 1;
	
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
		return Integer.valueOf(ordinal()+offset);
	}
}
