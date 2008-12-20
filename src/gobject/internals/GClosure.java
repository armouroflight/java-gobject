package gobject.internals;

import gobject.runtime.GBoxed;

import com.sun.jna.Pointer;


public abstract class GClosure extends GBoxed {
	public GClosure(Pointer ptr) {
		super(GBoxedAPI.gboxed.g_closure_get_type(), ptr);
	}
}
