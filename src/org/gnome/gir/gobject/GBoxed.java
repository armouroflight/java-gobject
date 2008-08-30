package org.gnome.gir.gobject;

import com.sun.jna.PointerType;

public abstract class GBoxed extends PointerType {
	public abstract GType getType();
}
