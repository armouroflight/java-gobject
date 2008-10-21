package org.gnome.gir.gobject;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public abstract class GBoxed extends PointerType implements RegisteredType {
	public static Pointer getPointerFor(Object data) {
		Pointer ptr;
		if (data instanceof BoxedStructure)
			ptr = ((BoxedStructure) data).getPointer();
		else if (data instanceof BoxedUnion)
			ptr = ((BoxedUnion) data).getPointer();
		else if (data instanceof GBoxed)
			ptr = ((GBoxed) data).getPointer();
		else	
			throw new RuntimeException("Invalid unboxed object " + data);
		return ptr;
	}
}
