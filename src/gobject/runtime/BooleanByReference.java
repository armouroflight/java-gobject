package gobject.runtime;

import com.sun.jna.ptr.ByReference;

/**
 * A simple class for passing Boolean values by reference,
 * similar to the other JNA classes.
 */
public class BooleanByReference extends ByReference {

	protected BooleanByReference() {
		this(false);
	}

	public BooleanByReference(boolean b) {
		super(4);
		setValue(b);
	}
	
	public void setValue(boolean b) {
		getPointer().setInt(0, b ? 1 : 0);
	}

	public boolean getValue() {
		return getPointer().getInt(0) != 0;
	}
}
