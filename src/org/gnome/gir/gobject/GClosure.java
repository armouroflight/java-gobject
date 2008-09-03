package org.gnome.gir.gobject;


public abstract class GClosure extends GBoxed {
	public GType getType() {
		return GBoxedAPI.gboxed.g_closure_get_type();
	}
}
