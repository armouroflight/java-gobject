
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

package gobject.internals;

import gobject.runtime.GObject;
import gobject.runtime.GType;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.gnome.gir.repository.BaseInfo;

import com.sun.jna.Pointer;

/**
 *
 */
public abstract class NativeObject extends Handle {
    private final Pointer handle;
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
        return initializer(ptr, false);
    }
    protected static Initializer initializer(Pointer ptr, boolean needRef) {
        if (ptr == null) {
            throw new IllegalArgumentException("Invalid native pointer");
        }
        return new Initializer(ptr, needRef);
    }
    /** Creates a new instance of NativeObject */
    protected NativeObject(final Initializer init) {
        if (init == null) {
            throw new IllegalArgumentException("Initializer cannot be null");
        }
        nativeRef = new NativeRef(this);
        this.handle = init.ptr;
        
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

    @Override
    protected void finalize() throws Throwable {
        instanceMap.remove(handle, nativeRef);
        super.finalize();
    }
    
    protected Object nativeValue() {
        return getNativeAddress();
    }

    public Pointer getNativeAddress() {
        return handle;
    }
    
    public static class Internals {

        public static final boolean debugMemory;
        public static final boolean debugMemoryFinalization;        
        
        static {
        	String debug = System.getProperty("jgir.debugMemory");
        	debugMemoryFinalization = debug != null ? debug.equals("final") : false; 
        	debugMemory = debug != null ? debug.equals("all") : false;
        }
        
        public static void debugMemory(String fmt, Object...args) {
        	if (!debugMemory)
        		return;
        	System.err.printf(fmt, args);
        }
        
        public static void debugMemoryFinal(String fmt, Object...args) {
        	if (!(debugMemoryFinalization || debugMemory))
        		return;
        	System.err.printf(fmt, args);
        }        
    	
		public static NativeObject instanceFor(Pointer ptr) {
		    WeakReference<NativeObject> ref = NativeObject.instanceMap.get(ptr);
		    
		    //
		    // If the reference was there, but the object it pointed to had been collected, remove it from the map
		    //
		    if (ref != null && ref.get() == null) {
		        NativeObject.instanceMap.remove(ptr);
		    }
		    return ref != null ? ref.get() : null;
		}

		private static Class<?> getStubClassFor(Class<?> proxyClass) {
			Class<?>[] declared = proxyClass.getDeclaredClasses();
			for (Class<?> c: declared) {
				if (c.getName().endsWith("$AnonStub"))
					return c;
			}
			throw new RuntimeException("Couldn't find Stub for interface: " + proxyClass);
		}		

	    public static <T extends NativeObject> T objectFor(Pointer ptr, Class<T> cls, boolean ownsRef) {
			return objectFor(ptr, cls, ownsRef, true);
		}
	    
	    @SuppressWarnings("unchecked")
		public static <T extends NativeObject> T objectFor(Pointer ptr, Class<T> cls, boolean ownsRef, boolean peekGType) {
	        // Ignore null pointers
	        if (ptr == null) {
	            return null;
	        }
	        NativeObject obj = null;
	        if (BaseInfo.class.isAssignableFrom(cls)) {
	        	obj = BaseInfo.newInstanceFor(ptr);
	        	if (obj != null && ownsRef)
	        		((RefCountedObject) obj).unref();
	        } else if (GObject.class.isAssignableFrom(cls) || GObject.GObjectProxy.class.isAssignableFrom(cls)) {
	        	obj = Internals.instanceFor(ptr);
	        	if (obj != null && ownsRef)
	        		GObjectAPI.gobj.g_object_unref((GObject)obj);
	        }
	        if (obj != null) {
	        	if (!cls.isInstance(obj))
	        		throw new RuntimeException(String.format("returned obj %s (%s) not instanceof %s", obj, obj.getClass(), cls));	        	
	            return cls.cast(obj);
	        }
	        
	        boolean expectedGObject = GObject.class.isAssignableFrom(cls);
	        boolean expectedGInterface = GObject.GObjectProxy.class.isAssignableFrom(cls);
	         	
	        if (peekGType && (expectedGObject || expectedGInterface)) {
		        /* Read the g_class field to find the most exact class match */	
	        	cls = classFor(ptr, cls);
	        	/* If it's abstract or an interface, pull out the stub. */
	        	if (cls.isInterface() || ((cls.getModifiers() & Modifier.ABSTRACT) != 0))
	        		cls = (Class<T>) Internals.getStubClassFor(cls);
	        }        
	        /* Ok, let's try to find an Initializer constructor
	         */
	        try {
	            Constructor<T> constructor = cls.getDeclaredConstructor(Initializer.class);
	            constructor.setAccessible(true);
	            T retVal = constructor.newInstance(initializer(ptr, ownsRef));
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
        return getClass().getSimpleName() + "(" + getNativeAddress() + ")";
    }
}
