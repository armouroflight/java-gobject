package org.gnome.gir.gobject;

import java.lang.reflect.Constructor;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.TypeMapper;

public abstract class GBoxed extends PointerType implements RegisteredType {
	
	protected GType gtype = GType.INVALID;
	
	public GBoxed(GType gtype, Pointer ptr) {
		super(ptr);
		this.gtype = gtype;
	}
	
	protected void free() {
		GBoxedAPI.gboxed.g_boxed_free(gtype, this.getPointer());
	}
	
	/**
	 * Return the GType associated with this boxed.  Not intended for public use.
	 */
	GType getGType() {
		return gtype;
	}
	
	@Override
	public void finalize() throws Throwable {
		free();
		super.finalize();
	}		
	
	/* A stub class we return for boxed reference types that we don't know about */
	private static class AnonBoxed extends GBoxed {

		public AnonBoxed(GType gtype, Pointer ptr) {
			super(gtype, ptr);			
		}	
	}
	
	public static Pointer getPointerFor(Object data) {
		Pointer ptr;
		if (data instanceof BoxedStructure) {
			ptr = ((BoxedStructure) data).getPointer();
		} else if (data instanceof BoxedUnion) {
			ptr = ((BoxedUnion) data).getPointer();	
		} else if (data instanceof GBoxed) {
			ptr = ((GBoxed) data).getPointer();
		} else {	
			throw new RuntimeException("Invalid unboxed object " + data);
		}
		/* Since we're denaturing it here, we need to ensure that the structure
		 * is written to native memory.
		 */		
		if (data instanceof Structure)
			((Structure) data).write();		
		return ptr;
	}
	
	public static void postInvokeRead(Object data) {
		if (!(data instanceof Structure))
			return;
		((Structure) data).read();
	}
	
	public static Object boxedFor(GType gtype, Pointer ptr) {
		Class<?> boxedKlass = GType.lookupProxyClass(gtype);
		if (boxedKlass != null && Structure.class.isAssignableFrom(boxedKlass)) {
			try {
				Constructor<?> ctor = boxedKlass.getDeclaredConstructor(new Class<?>[] { GType.class, Pointer.class, TypeMapper.class });
				ctor.setAccessible(true);
				return ctor.newInstance(new Object[] { gtype, ptr, GTypeMapper.getInstance() });
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			return new AnonBoxed(gtype, ptr);
		}
	}
}
