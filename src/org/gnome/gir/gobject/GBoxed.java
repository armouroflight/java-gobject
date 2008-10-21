package org.gnome.gir.gobject;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;

public abstract class GBoxed extends PointerType implements RegisteredType {
	public static Pointer getPointerFor(Object data) {
		Pointer ptr;
		/* Since we're denaturing it here, we need to ensure that the structure
		 * is written to native memory.
		 */
		if (data instanceof BoxedStructure) {
			ptr = ((BoxedStructure) data).getPointer();
		} else if (data instanceof BoxedUnion) {
			ptr = ((BoxedUnion) data).getPointer();	
		} else if (data instanceof GBoxed) {
			ptr = ((GBoxed) data).getPointer();
		} else {	
			throw new RuntimeException("Invalid unboxed object " + data);
		}
		if (data instanceof Structure)
			((Structure) data).write();		
		return ptr;
	}
	
	public static void postInvokeRead(Object data) {
		if (!(data instanceof Structure))
			return;
		((Structure) data).read();
	}
}
