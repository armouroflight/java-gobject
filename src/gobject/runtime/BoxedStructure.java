package gobject.runtime;

import gobject.internals.GBoxedAPI;
import gobject.internals.GObjectAPI;
import gobject.internals.RegisteredType;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.TypeMapper;

/**
 * Peer object for a native "boxed" structure type. This class extends the 
 * JNA {@link com.sun.jna.Structure} class, and may be manipulated using methods
 * associated with that class.
 * 
 * @see com.sun.jna.Structure
 * @see <a href="../../gtk-doc/html/gobject/gobject-Boxed-Types.html">Boxed Types</a>
 */
public abstract class BoxedStructure extends Structure implements RegisteredType {

	private final GType gtype;
	private final boolean isNative;
	
	protected BoxedStructure(TypeMapper mapper) {
		super(mapper);
		gtype = GType.INVALID; // Should not be used
		isNative = false;
	}	

	protected BoxedStructure(Pointer pointer, TypeMapper mapper) {
		super(mapper);
		useMemory(pointer);
		this.gtype = GType.fromClass(getClass());
		isNative = true;
	}
	
	protected BoxedStructure(GType gtype, Pointer pointer, TypeMapper mapper) {
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
		return String.format("BoxedStructure<%s>(%s)", GObjectAPI.gobj.g_type_name(gtype), super.toString());
	}
}
