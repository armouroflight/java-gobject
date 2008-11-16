package org.gnome.gir.gobject;


public abstract class GInitiallyUnowned extends GObject {
	/*
	 * Note - memory management for this class is handled inside GObject, we
	 * check whether an object is floating there.
	 */
	public GInitiallyUnowned(Initializer init) {
		super(init);
	}

	protected GInitiallyUnowned(GType gtype, Object[] args) {
		super(gtype, args);
	}
}
