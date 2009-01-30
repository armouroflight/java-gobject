package gobject.runtime;

import gobject.internals.GBoxedAPI;
import gobject.internals.GObjectAPI;
import gobject.internals.RegisteredType;

import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;
import com.sun.jna.Union;

/**
 * Peer object for a native "boxed" union type. This class extends the 
 * JNA {@link com.sun.jna.Union} class, and may be manipulated using methods
 * associated with that class.
 * 
 * @see com.sun.jna.Union 
 * @see <a href="../../gtk-doc/html/gobject/gobject-Boxed-Types.html">Boxed Types</a>
 */
public abstract class BoxedUnion extends Union implements RegisteredType {
	
	private final GType gtype;
	private final boolean isNative;
	
	protected BoxedUnion(TypeMapper mapper) {
		super(mapper);
		gtype = GType.INVALID;
		isNative = false;
	}
	
	protected BoxedUnion(Pointer pointer, TypeMapper mapper) {
		super(mapper);
		useMemory(pointer);
		this.gtype = GType.fromClass(getClass());
		isNative = true;
	}	
	
	protected BoxedUnion(GType gtype, Pointer pointer, TypeMapper mapper) {
		super(mapper);
		useMemory(pointer);		
		this.gtype = gtype;
		isNative = true;
	}
	
	/**
	 * Return the GType associated with this boxed.  Not intended for public use.
	 */
	public GType getGType() {
		return gtype;
	}	

	protected void free() {
		GBoxedAPI.gboxed.g_boxed_free(gtype, this.getPointer());
	}
	
	@Override
	public void finalize() throws Throwable {
		if (isNative)
			free();
		super.finalize();
	}
	
	@Override
	public String toString() {
		return GObjectAPI.gobj.g_type_name(gtype) + "(" + super.toString() + ")";
	}	
}
