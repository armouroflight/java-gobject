package gobject.runtime;

import com.sun.jna.FromNativeContext;
import com.sun.jna.NativeMapped;

/**
 * Base class for multi-valued bitfields, or flags.  Converted
 * between an integer type when passed to native code.
 */
public abstract class GFlags implements NativeMapped {
	private int value = 0;
	
	public GFlags() {
	}
	
	@SuppressWarnings("unused")
	private GFlags(int value) {
		this.value = value;
	}
	
	public GFlags(int...flags) {
		add(flags);
	}
	
	public final void add(int...flags) {
		for (int flag : flags)
			value |= flag;
	}
	
	public final void remove(int...flags) {
		int val = 0;
		for (int flag : flags)
			val += flag;
		value &= ~val;
	}
	
	public final int getValue() {
		return value;
	}
	
	public final boolean contains(int...flags) {
		for (int flag : flags)
			if ((value & flag) == 0)
				return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public final Object fromNative(Object nativeValue, FromNativeContext context) {
		try {
			return context.getTargetType().getConstructor(new Class<?>[] { int[].class })
				.newInstance(new Object[] { new int[] { (Integer) nativeValue } });
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final Class<?> nativeType() {
		return Integer.class;
	}

	@Override
	public final Object toNative() {
		return value;
	}
}
