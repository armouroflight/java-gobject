package org.gnome.gir.repository;

import org.gnome.gir.gobject.NativeEnum;

public enum FieldInfoFlags implements NativeEnum{
	IS_READABLE,
	IS_WRITABLE;

	@Override
	public int getNative() {
		return 1 << ordinal();
	}
	
}