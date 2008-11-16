
/* 
 * Copyright (c) 2008 Colin Walters <walters@verbum.org>
 * 
 * This file is part of java-gobject-introspection.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the 
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, 
 * Boston, MA  02111-1307  USA.
 *
 */
/* 
 * Copyright (c) 2007 Wayne Meissner
 * 
 * This file was originally part of gstreamer-java; modified for use in
 * jgir.  By permission of author, this file has been relicensed from LGPLv3
 * to the license of jgir; see below.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the 
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, 
 * Boston, MA  02111-1307  USA. 
 */

package org.gnome.gir.gobject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.gnome.gir.repository.BaseInfo;

import com.sun.jna.Pointer;

/**
 *
 */
public abstract class NativeObject extends Handle {
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicBoolean valid = new AtomicBoolean(true);
    private final Pointer handle;
    protected final AtomicBoolean ownsHandle = new AtomicBoolean(false);
    private final NativeRef nativeRef;
    
    private static final ConcurrentMap<Pointer, NativeRef> instanceMap = new ConcurrentHashMap<Pointer, NativeRef>();    
    
    static class NativeRef extends WeakReference<NativeObject> {
        public NativeRef(NativeObject obj) {
            super(obj);
        }
    }    
    
    /*
     * The default for new objects is to not need a refcount increase, and that
     * they own the native object.  Special cases can use the other constructor.
     */
    protected static Initializer initializer(Pointer ptr) {
        return initializer(ptr, false, true);
    }
    protected static Initializer initializer(Pointer ptr, boolean needRef, boolean ownsHandle) {
        if (ptr == null) {
            throw new IllegalArgumentException("Invalid native pointer");
        }
        return new Initializer(ptr, needRef, ownsHandle);
    }
    /** Creates a new instance of NativeObject */
    protected NativeObject(final Initializer init) {
        if (init == null) {
            throw new IllegalArgumentException("Initializer cannot be null");
        }
        nativeRef = new NativeRef(this);
        this.handle = init.ptr;
        this.ownsHandle.set(init.ownsHandle);
        
        //
        // Only store this object in the map if we can tell when it has been disposed 
        // (i.e. must be at least a GObject - MiniObject and other NativeObject subclasses
        // don't signal destruction, so it is impossible to know if the instance 
        // is stale or not
        //
        if (GObject.class.isAssignableFrom(getClass())) {
            instanceMap.put(init.ptr, nativeRef);
        }
        
    }
    
    abstract protected void disposeNativeHandle(Pointer ptr);
    
    public void dispose() {
        if (!disposed.getAndSet(true)) {
            instanceMap.remove(handle, nativeRef);
            if (ownsHandle.get()) {
                disposeNativeHandle(handle);
            }
            valid.set(false);
        }
    }
    
    protected void invalidate() {
        instanceMap.remove(handle(), nativeRef);
        disposed.set(true);
        ownsHandle.set(false);
        valid.set(false);
    }
    
    @Override
    protected void finalize() throws Throwable {
        try {
            dispose();
        } finally {
            super.finalize();
        }
    }
    protected Object nativeValue() {
        return handle();
    }
    protected Pointer handle() {
        if (!valid.get()) {
            throw new IllegalStateException("Native object has been disposed");
        }
        return handle;
    }
    public Pointer getNativeAddress() {
        return handle;
    }
    protected boolean isDisposed() {
        return disposed.get();
    }
    protected static NativeObject instanceFor(Pointer ptr) {
        WeakReference<NativeObject> ref = instanceMap.get(ptr);
        
        //
        // If the reference was there, but the object it pointed to had been collected, remove it from the map
        //
        if (ref != null && ref.get() == null) {
            instanceMap.remove(ptr);
        }
        return ref != null ? ref.get() : null;
    }
    public static <T extends NativeObject> T objectFor(Pointer ptr, Class<T> cls, boolean ownsRef) {
        return objectFor(ptr, cls, ownsRef, true);
    }
    
    private static Class<?> getStubClassFor(Class<?> proxyClass) {
    	Class<?>[] declared = proxyClass.getDeclaredClasses();
    	for (Class<?> c: declared) {
    		if (c.getName().endsWith("$AnonStub"))
    			return c;
    	}
    	throw new RuntimeException("Couldn't find Stub for interface: " + proxyClass);
    }
    
	public static <T extends NativeObject> T objectFor(Pointer ptr, Class<T> cls, boolean ownsRef, boolean ownsHandle) {
		return objectFor(ptr, cls, ownsRef, ownsHandle, true);
	}
    
    @SuppressWarnings("unchecked")
	public static <T extends NativeObject> T objectFor(Pointer ptr, Class<T> cls, boolean ownsRef, boolean ownsHandle, boolean peekGType) {
        // Ignore null pointers
        if (ptr == null) {
            return null;
        }
        NativeObject obj = null;
        if (BaseInfo.class.isAssignableFrom(cls))
        	obj = BaseInfo.newInstanceFor(ptr);
        else if (GObject.class.isAssignableFrom(cls) || GObject.GObjectProxy.class.isAssignableFrom(cls))
        	obj = NativeObject.instanceFor(ptr);
        if (obj != null && cls.isInstance(obj)) {
            if (ownsRef) {
                ((RefCountedObject) obj).unref(); // Lose the extra ref that we expect functions to add by default
            }
            return cls.cast(obj);
        }
       

        /* Special-case GObject.GObjectProxy here - these are interface values
         * for which we don't know of a current concrete class.
         */
        if (cls.isInterface() && GObject.GObjectProxy.class.isAssignableFrom(cls)) {
    		cls = (Class<T>) getStubClassFor(cls);        	
        }
        /* For GObject, read the g_class field to find
         * the most exact class match
         */        	
        else if (peekGType && GObject.class.isAssignableFrom(cls)) {
        	cls = classFor(ptr, cls);
        	/* If it's abstract, pull out the stub */
        	if ((cls.getModifiers() & Modifier.ABSTRACT) != 0)
        		cls = (Class<T>) getStubClassFor(cls);
        }        
        /* Ok, let's try to find an Initializer constructor
         */
        try {
            Constructor<T> constructor = cls.getDeclaredConstructor(Initializer.class);
            constructor.setAccessible(true);
            T retVal = constructor.newInstance(initializer(ptr, ownsRef, ownsHandle));
            //retVal.initNativeHandle(ptr, refAdjust > 0, ownsHandle);
            return retVal;
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }

    }
    
    @SuppressWarnings("unchecked")
	protected static Class<?> lookupProxyChain(GType gtype) {
    	Class<?> ret = null;
    	while (ret == null && !gtype.equals(GType.OBJECT)) {
    		ret = GType.lookupProxyClass(gtype);
    		gtype = gtype.getParent();
    	}
    	return ret;
    }
    
    @SuppressWarnings("unchecked")
	protected static <T extends NativeObject> Class<T> classFor(Pointer ptr, Class<T> defaultClass) {
    	GType gtype = GType.objectPeekType(ptr);
    	Class<?> cls = lookupProxyChain(gtype);
    	return (cls != null && defaultClass.isAssignableFrom(cls)) ? (Class<T>) cls : defaultClass; 
    }    

    @Override
    public boolean equals(Object o) {
        return o instanceof NativeObject && ((NativeObject) o).handle.equals(handle);
    }
    
    @Override
    public int hashCode() {
        return handle.hashCode();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + handle() + ")";
    }
    
    //
    // No longer want to garbage collect this object
    //
    public void disown() {
        ownsHandle.set(false);
    }
}
