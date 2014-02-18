package gobject.runtime;

import java.util.Map;


/**
 * A kind of {@link GObject} which in native code has a "floating" reference.
 * This distinction is irrelevant to managed Java code - all GObjects have
 * any floating references immediately "sunk".
 * <p>
 * @see <a href="../../gtk-doc/html/gobject/gobject-The-Base-Object-Type.html#floating-ref">floating references</a>
 * @see <a href="../../gtk-doc/html/gobject/gobject-The-Base-Object-Type.html#GInitiallyUnowned">native GInitiallyUnowned</a>
 */
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
	
	protected GInitiallyUnowned(GType gtype, Map<String,Object> args) {
		super(gtype, args);
	}	
}
