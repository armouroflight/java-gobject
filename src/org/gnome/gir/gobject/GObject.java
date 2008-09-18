
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.gnome.gir.gobject.GObjectAPI.GParamSpec;
import org.gnome.gir.gobject.GObjectAPI.GToggleNotify;
import org.gnome.gir.gobject.GObjectAPI.GWeakNotify;

import com.sun.jna.Callback;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 * This is an abstract class providing some GObject-like facilities in a common 
 * base class.  Not intended for direct use.
 */
public abstract class GObject extends RefCountedObject {
    private static final Map<GObject, Boolean> strongReferences = new ConcurrentHashMap<GObject, Boolean>();

    private final IntPtr objectID = new IntPtr(System.identityHashCode(this));
    
    /* Hold a strong Java reference between this proxy object and any signal
     * handlers installed.  Often this would be done anyways, but if you're
     * just calling System.out.println in a callback, it would otherwise
     * be elgible for GC.
     */
    private Map<Long,Callback> signalHandlers = new HashMap<Long, Callback>();
    
    /**
     * A tagging interface to mark classes which are GObject property bags.
     * @author walters
     *
     */
    public static interface GProperties {};
    
    /**
     * A tagging interface used in the code generator - if a method returns an interface,
     * we have it extend this interface so we know it's a GObject. 
     * @author walters
     */
    public static interface GObjectProxy {};

    /**
     * The core GObject initializer function, intended for invocation from
     * return values of unmanaged code.
     * @param init
     */
    public GObject(Initializer init) { 
        super(init.needRef ? initializer(init.ptr, false, init.ownsHandle) : init);
        if (init.ownsHandle) {
            strongReferences.put(this, Boolean.TRUE);
            /* The toggle reference is our primary means of memory management between
             * this Proxy object and the GObject.
             */
            GObjectAPI.gobj.g_object_add_toggle_ref(init.ptr, toggle, objectID);
            if (!init.needRef) {
                unref();
            }
            
            GObjectAPI.gobj.g_object_weak_ref(this, weakNotify, null);
        }
    }
    
    private static Initializer getInitializer(GType gtype, Object[] args) {
    	Object[] newArgs;
    	String firstProp = null;
    	if (args != null && args.length > 0) {
    		if ((args.length % 2) != 0)
    			throw new IllegalArgumentException("Number of construct parameters must be even");
    		firstProp = (String) args[0];
    		newArgs = new Object[args.length];
        	System.arraycopy(args, 1, newArgs, 0, args.length);
        	newArgs[args.length-1] = null;
    	} else {
    		newArgs = new Object[1];
    		newArgs[0] = null;
    	}
    	return new Initializer(GObjectAPI.gobj.g_object_new(gtype, firstProp, newArgs));
    }
    
    protected GObject(GType gtype, Object[] args) {
    	this(getInitializer(gtype, args));
    }
    
    private static Initializer getInitializer(GType gtype, Map<String,Object> args) {
    	String firstProp = null;
    	Object[] newArgs = new Object[args.size()*2];
    	int i = 0;
    	Iterator<Map.Entry<String, Object>> it = args.entrySet().iterator();
    	if (it.hasNext()) {
    		Map.Entry<String, Object> entry = it.next();
    		firstProp = entry.getKey();
    		newArgs[i] = entry.getValue();
    		i++;
    	}
    	while (it.hasNext()) {
    		Map.Entry<String, Object> entry = it.next();    		
    		newArgs[i] = entry.getKey();
    		newArgs[i+1] = entry.getValue();
    		i += 2;
    	}
    	newArgs[i] = null;
    	return new Initializer(GObjectAPI.gobj.g_object_new(gtype, firstProp, newArgs));
    }
    
    protected GObject(GType gtype, Map<String,Object> args) {
    	this(getInitializer(gtype, args));
    }
    
    /**
     * Sets the value of a <tt>GObject</tt> property.
     *
     * @param property The property to set.
     * @param data The value for the property.  This must be of the type expected
     * by gstreamer.
     */
    public void set(String property, Object data) {
        GParamSpec propertySpec = findProperty(property);
        if (propertySpec == null) {
            throw new IllegalArgumentException("Unknown property: " + property);
        }
        final GType propType = propertySpec.value_type;
        
        GValue propValue = new GValue();
        GValueAPI.gvalue.g_value_init(propValue, propType);
        propValue.set(data);
        GObjectAPI.gobj.g_object_set_property(this, property, propValue);
        GValueAPI.gvalue.g_value_unset(propValue); // Release any memory
    }
    
    /**
     * Gets the current value of a <tt>GObject</tt> property.
     *
     * @param property The name of the property to get.
     *
     * @return A java value representing the <tt>GObject</tt> property value.
     */
    public Object get(String property) {
        GObjectAPI.GParamSpec propertySpec = findProperty(property);
        if (propertySpec == null) {
            throw new IllegalArgumentException("Unknown property: " + property);
        }
        final GType propType = propertySpec.value_type;
        GValue propValue = new GValue();
        GValueAPI.gvalue.g_value_init(propValue, propType);
        GObjectAPI.gobj.g_object_get_property(this, property, propValue);
        return propValue.toJava();
    }
    
    protected void disposeNativeHandle(Pointer ptr) {
        GObjectAPI.gobj.g_object_remove_toggle_ref(ptr, toggle, objectID);
    }
    @Override
    protected void ref() {
    	GObjectAPI.gobj.g_object_ref(this);
    }

    @Override
    protected void unref() {
    	GObjectAPI.gobj.g_object_unref(this);
    }
    protected void invalidate() {
        try {
            // Need to increase the ref count before removing the toggle ref, so 
            // ensure the native object is not destroyed.
            if (ownsHandle.get()) {
                ref();

                // Disconnect the callback.
                GObjectAPI.gobj.g_object_remove_toggle_ref(handle(), toggle, objectID);
            }
            strongReferences.remove(this);
        } finally { 
            super.invalidate();
        }
    }
 
    public synchronized long connect(String signal, Callback closure) {
        NativeLong connectID = GSignalAPI.gsignal.g_signal_connect_data(GObject.this, 
                signal, closure, null, null, 0);
        if (connectID.intValue() == 0) {
            throw new IllegalArgumentException(String.format("Failed to connect signal '%s'", signal));
        }
        long id = connectID.longValue();
        signalHandlers.put(id, closure);
        return id;
    }
    
    private interface NotifyCallback extends Callback {
    	public void onNotify(GObject object, GParamSpec param, Pointer data);
    }
    
    public synchronized long connectNotify(final String propName, final Callback callback) {
    	NotifyCallback trampoline = new NotifyCallback() {
			@Override
			public void onNotify(GObject object, GParamSpec param, Pointer data) {
				Method[] methods = callback.getClass().getDeclaredMethods();
				if (methods.length != 1)
					throw new RuntimeException(String.format("Callback %s must declare exactly one method", callback.getClass()));
				Method meth = methods[0];
				Class<?>[] params = meth.getParameterTypes();
				if (params.length != 2)
					throw new RuntimeException(String.format("Callback %s entry must have exactly two parameters", callback.getClass()));
				Object propValue = get(propName);
				try {
					methods[0].invoke(callback, new Object[] { object, propValue });
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
    	};
    	return connect("notify::" + propName, trampoline);
    }
    
    public synchronized void disconnect(String signal, long id) {
    	Callback cb = signalHandlers.get(id);
    	if (cb == null)
    		throw new IllegalArgumentException("Invalid signal handler id:" + id);
    	GSignalAPI.gsignal.g_signal_handler_disconnect(GObject.this, new NativeLong(id));
    	signalHandlers.remove(id);
    }
    
    public static GObject objectFor(Pointer ptr, Class<? extends GObject> defaultClass) {
        return GObject.objectFor(ptr, defaultClass, true);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends GObject> T objectFor(Pointer ptr, Class<T> defaultClass, boolean needRef) {
        return NativeObject.objectFor(ptr, defaultClass, needRef);        
    }

    private GObjectAPI.GParamSpec findProperty(String propertyName) {
        return GObjectAPI.gobj.g_object_class_find_property(handle().getPointer(0), propertyName);
    }
    /*
     * Hooks to/from native disposal
     */
    private static final GToggleNotify toggle = new GToggleNotify() {
        public void callback(Pointer data, Pointer ptr, boolean is_last_ref) {            
            /*
             * Manage the strong reference to this instance.  When this is the last
             * reference to the underlying object, remove the strong reference so
             * it can be garbage collected.  If it is owned by someone else, then make
             * it a strong ref, so the java GObject for the underlying C object can
             * be retained for later retrieval
             */
            GObject o = (GObject) NativeObject.instanceFor(ptr);
            if (o == null) {
                return;
            }
            if (is_last_ref) {
                strongReferences.remove(o);
            } else {
                strongReferences.put(o, Boolean.TRUE);
            }
        }
    };
    
    private static final GWeakNotify weakNotify = new GWeakNotify() {
		@Override
		public void callback(Pointer data, Pointer obj) {
			GObject o = (GObject) NativeObject.instanceFor(obj);
			// Clear out the signal handler references
			if (o == null)
				return;
			synchronized (o) {
				o.signalHandlers = null;
			}
		}
    };    
}
