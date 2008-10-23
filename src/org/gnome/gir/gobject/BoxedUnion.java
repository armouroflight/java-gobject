package org.gnome.gir.gobject;

import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;
import com.sun.jna.Union;


public abstract class BoxedUnion extends Union implements RegisteredType {
	
	private final GType gtype;
	private final boolean isNative;
	
	protected BoxedUnion(TypeMapper mapper) {
		super(mapper);
		gtype = GType.INVALID;
		isNative = false;
	}
	
	protected BoxedUnion(GType gtype, Pointer pointer, TypeMapper mapper) {
		super(mapper);
		useMemory(pointer);		
		this.gtype = gtype;
		isNative = true;
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
