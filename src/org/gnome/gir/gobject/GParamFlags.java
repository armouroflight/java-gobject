package org.gnome.gir.gobject;

public enum GParamFlags implements NativeEnum {
	READABLE,
	WRITABLE,
	CONSTRUCT,
	CONSTRUCT_ONLY,
	LAX_VALIDATION,
	STATIC_NAME,
	STATIC_NICK,
	STATIC_BLURB;
	
	public int getNative() {
		return 1 << this.ordinal();
	};
}
