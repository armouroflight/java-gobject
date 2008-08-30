package org.gnome.gir.gobject;


public abstract class GInitiallyUnowned extends GObject {

	public GInitiallyUnowned(Initializer init) {
		super(init);
	}
	
	@Override
	protected void ref() {
		GObjectAPI.gobj.g_object_ref_sink(this);
	}

	protected GInitiallyUnowned(GType gtype, Object[] args) {
		super(gtype, args);
	}

}
