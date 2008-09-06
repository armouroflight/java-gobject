package org.gnome.gir.gobject;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.TypeMapper;


public abstract class BoxedStructure extends Structure implements RegisteredType {

	private boolean isNative = false;
	
	protected BoxedStructure(TypeMapper mapper) {
		super(mapper);
	}	
	
	public BoxedStructure(Pointer pointer) {
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
}
