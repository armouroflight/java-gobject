package org.gnome.gir.gobject;

import com.sun.jna.Pointer;

public interface GenericGList {
	public abstract Pointer getData();
	public abstract GenericGList getNext();
	public abstract void free();		
}
