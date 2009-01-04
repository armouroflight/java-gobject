package gobject.runtime;

import gobject.internals.GBoxedAPI;
import gobject.internals.GTypeMapper;
import gobject.internals.RegisteredType;

import java.lang.reflect.Constructor;


import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import com.sun.jna.TypeMapper;

public abstract class GBoxed extends PointerType implements RegisteredType {
	
	protected GType gtype = GType.INVALID;
	
	/* Should not use this constructor */
	public GBoxed() {
	}
	
	public GBoxed(GType gtype, Pointer ptr) {
		super(ptr);
		this.gtype = gtype;
	}
	
	public GBoxed(Pointer ptr, TypeMapper typeMapper) {
		super(ptr);
		this.gtype = GType.fromClass(getClass());
	}	
	
	public GBoxed(GType gtype, Pointer ptr, TypeMapper typeMapper) {
		this(gtype, ptr);
	}
	
	protected void free() {
		if (gtype != GType.INVALID)
			GBoxedAPI.gboxed.g_boxed_free(gtype, this.getPointer());
	}
	
	/**
	 * Return the GType associated with this boxed.  Not intended for public use.
	 */
	public GType getGType() {
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
	
	private static RegisteredType boxedFor(Pointer ptr, Class<?> klass, GType gtype) {
		try {
			Constructor<?> ctor = klass.getConstructor(new Class<?>[] { GType.class, Pointer.class, TypeMapper.class });
			ctor.setAccessible(true);
			return (RegisteredType) ctor.newInstance(new Object[] { gtype, ptr, GTypeMapper.getInstance() });
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
	// Called from the compiler
	public static RegisteredType boxedFor(Pointer ptr, Class<?> klass) {
		return boxedFor(ptr, klass, GType.fromClass(klass));
	}
	
	public static RegisteredType boxedFor(GType gtype, Pointer ptr) {
		if (ptr == null)
			return null;
		Class<?> boxedKlass = GType.lookupProxyClass(gtype);
		if (boxedKlass != null) {
			return boxedFor(ptr, boxedKlass, gtype);
		} else {
			return new AnonBoxed(gtype, ptr);
		}
	}
}