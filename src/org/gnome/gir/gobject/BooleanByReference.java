package org.gnome.gir.gobject;

import com.sun.jna.ptr.ByReference;

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
