package org.gnome.gir.gobject;

import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;
import com.sun.jna.Union;


public abstract class BoxedUnion extends Union implements RegisteredType {
	
	private boolean isNative = false;
	
	protected BoxedUnion(TypeMapper mapper) {
		super(mapper);
	}
	
	public BoxedUnion(Pointer pointer) {
		Pointer retptr = GBoxedAPI.gboxed.g_boxed_copy(GType.of(this.getClass()), pointer);		
		useMemory(retptr);
		isNative = true;
	}

	protected void free() {
		GBoxedAPI.gboxed.g_boxed_free(GType.of(this.getClass()), this.getPointer());
	}
	
	@Override
	public void finalize() throws Throwable {
		if (isNative)
			free();
		super.finalize();
	}
	
	@Override
	public String toString() {
		return GObjectAPI.gobj.g_type_name(GType.of(this.getClass())) + "(" + super.toString() + ")";
	}	
}
