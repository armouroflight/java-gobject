package gobject.runtime;

import com.sun.jna.FromNativeContext;
import com.sun.jna.NativeMapped;

/**
 * Base class for multi-valued bitfields, or flags.  In native code,
 * this value is represented as an integral type.
 * <p>
 * Each flag is represented by a constant associated with the class.  Typical
 * usage is to create a new instance with the varargs constructor {@link GFlags(int...)},
 * passed to native code.
 * <p>
 * For example:
 * <pre>
 *   File f = GioGlobals.fileNewForPath("/home/username/Documents"); 
 *   FileMonitor fm = f.monitorFile(new FileMonitorFlags(FileMonitorFlags.WATCH_MOUNTS), null);
 * </pre> 
 */
public abstract class GFlags implements NativeMapped {
	private int value = 0;
	
	public GFlags() {
	}
	
	@SuppressWarnings("unused")
	private GFlags(int value) {
		this.value = value;
	}
	
	/**
	 * Create a flag object which holds the union of all specified individual flag values.
	 * 
	 * @param flags List of flag values
	 */
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
	
	/**
	 * Returns the native integral value, i.e. the bitwise and of the individual
	 * flag values.
	 */	
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
	public final Object fromNative(Object nativeValue, FromNativeContext context) {
		try {
			return context.getTargetType().getConstructor(new Class<?>[] { int[].class })
				.newInstance(new Object[] { new int[] { (Integer) nativeValue } });
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public final Class<?> nativeType() {
		return Integer.class;
	}

	public final Object toNative() {
		return value;
	}
}
