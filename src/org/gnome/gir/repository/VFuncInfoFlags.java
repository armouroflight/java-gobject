/**
 * 
 */
package org.gnome.gir.repository;

import org.gnome.gir.gobject.NativeEnum;

public enum VFuncInfoFlags implements NativeEnum {
	MUST_CHAIN_UP,
	MUST_OVERRIDE,
	MUST_NOT_OVERRIDE;

	@Override
	public int getNative() {
		return 1 << ordinal();
	}
}