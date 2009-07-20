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

package gobject.runtime;

import gobject.internals.GObjectAPI;
import gobject.internals.GSignalAPI;
import gobject.internals.GTypeMapper;
import gobject.internals.IntPtr;
import gobject.internals.NativeObject;
import gobject.internals.GObjectAPI.GObjectStruct;
import gobject.internals.GObjectAPI.GParamSpec;
import gobject.internals.GObjectAPI.GToggleNotify;
import gobject.internals.GObjectAPI.GWeakNotify;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import com.sun.jna.Callback;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.TypeMapper;

/**
 * Provides the base Java peer for a native <tt>GObject</tt>.
 * <p>
 * <h3>Constructors</h3>
 * GObject has a powerful property mechanism, and object authors can specify properties
 * which are accessible at object construction time.  This is mapped as two different
 * kinds of constructors, one which takes a Map, and one which takes an Array of pairs.
 * <p>
 * For example, using the array {@link GObject(Object[])} constructor:
 * <pre>
 *   Window w = new Window(new Object[] { "type", Gtk.WindowType.TOPLEVEL, "opacity", 0.5 });
 * </pre>
 * <p>
 * And, using the map {@link GObject(Map)} constructor:
 * <pre>
 *   Window w = new Window(new HashMap<String,Object>() {
 *     {
 *       put("type", Gtk.WindowType.TOPLEVEL);
 *       put("opacity", 0.5);
 *     }
 *   });
 * </pre>
 * <h3>Signals</h3>
 * Each GObject signal is mapped to two Java components.  First, the signal signature (prototype) is exposed as
 * a Java interface.  Second, a method named "connect" taking an instance of that interface is added to the class.
 * <p>
 * A typical signal connection looks like:
 * <pre>
 *   somewidget.connect(new Show() {
 *     public void onShow(Widget w) {
 *         System.err.println("Widget " + w + " is shown");
 *     }
 *   });
 * </pre>
 * <h3>Properties</h3>
 * Each GObject property has up to 4 corresponding Java components.  If the property is readable, then
 * a "get&lt;PropertyName&gt;" method is generated.  If the property is writable,a "set&lt;PropertyName&gt;(value)" method is generated.
 * Finally, because GObject supports notification of property changes, in a similar fashion to signals a pair of a notification interface
 * and connect method are generated.
 * <p>
 * Connecting a notification for changes of window title value, then setting it.
 * <pre>
 *   window.connectNotify(new TitleNotify() {
 *     public void onTitleNotify(Window win) {
 *         System.err.println("Title of window changed to: " + win.getTitle());
 *     });
 *   }
 * 
 *   window.setTitle("My Window");
 * </pre>
 * @see <a href="../../gtk-doc/html/gobject/gobject-The-Base-Object-Type.html">native GObject</a>  
 */
public abstract class GObject extends NativeObject {
	private static final Map<GObject, Boolean> strongReferences = new ConcurrentHashMap<GObject, Boolean>();

	private final IntPtr objectID = new IntPtr(System.identityHashCode(this));
	private boolean disposed = false;

	/*
	 * Hold a strong Java reference between this proxy object and any signal
	 * handlers installed. Often this would be done anyways, but if you're just
	 * calling System.out.println in a callback, it would otherwise be eligible
	 * for GC.
	 */
	private Map<Long, Callback> signalHandlers = new HashMap<Long, Callback>();

	private static final void debugMemory(GObject obj, String fmt, Object... args) {
		if (NativeObject.Internals.debugMemory) {
			Object[] newArgs = new Object[args.length + 2];
			System.arraycopy(args, 0, newArgs, 1, args.length);
			newArgs[0] = obj;
			if (obj != null) {
				GObjectStruct objStruct = new GObjectAPI.GObjectStruct(obj);
				newArgs[newArgs.length - 1] = objStruct.ref_count;
			} else {
				newArgs[newArgs.length - 1] = "<null>";
			}
			NativeObject.Internals.debugMemory(fmt, newArgs);
		}
	}

	private static final void debugMemoryFinal(GObject obj, String fmt, Object... args) {
		if (!NativeObject.Internals.debugMemoryFinalization)
			return;
		debugMemory(obj, fmt, args);
	}

	/**
	 * Tagging interface for a native GInterface, which is exposed by JGIR as a
	 * Java interface.
	 * 
	 * @see <a href="../../gtk-doc/html/gobject/gtype-non-instantiable-classed.html">native GInterface</a>
	 */
	public static interface GObjectProxy {
	};

	/**
	 * The core GObject initializer function, intended for invocation from
	 * return values of unmanaged code.
	 * 
	 * @param init
	 */
	public GObject(Initializer init) {
		super(init);

		strongReferences.put(this, Boolean.TRUE);

		/*
		 * Floating refs are just a convenience for C; we always want only
		 * strong nonfloating refs for objects which have a JVM peer.
		 */
		boolean wasFloating = GObjectAPI.gobj.g_object_is_floating(this);
		if (wasFloating) {
			debugMemory(this, "SINK AND TOGGLE %s %s%n");
			GObjectAPI.gobj.g_object_ref_sink(this);
		} else {
			debugMemory(this, "INIT TOGGLE %s %s%n");
		}

		/*
		 * The toggle reference is our primary means of memory management
		 * between this Proxy object and the GObject.
		 */
		GObjectAPI.gobj.g_object_add_toggle_ref(init.ptr, toggle, objectID);

		/*
		 * The weak notify is just a convenient hook into object destruction so
		 * we can clear out our signal handlers and strong ref; see below.
		 */
		GObjectAPI.gobj.g_object_weak_ref(init.ptr, weakNotify, null);

		/*
		 * Normally we have a strong reference given to us by constructors,
		 * GValue property gets, etc. So here we unref, leaving the toggle
		 * reference we just added.
		 * 
		 * An example case where we don't own a ref are C convenience getters -
		 * need to ensure those are annotated with (transfer none).
		 */
		if (init.ownsRef) {
			GObjectAPI.gobj.g_object_unref(this);
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
			newArgs[args.length - 1] = null;
		} else {
			newArgs = new Object[1];
			newArgs[0] = null;
		}
		return new Initializer(GObjectAPI.gobj.g_object_new(gtype, firstProp, newArgs));
	}

	protected GObject(GType gtype, Object[] args) {
		this(getInitializer(gtype, args));
	}

	private static Initializer getInitializer(GType gtype, Map<String, Object> args) {
		String firstProp = null;
		Object[] newArgs = new Object[args.size() * 2];
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
			newArgs[i + 1] = entry.getValue();
			i += 2;
		}
		newArgs[i] = null;
		return new Initializer(GObjectAPI.gobj.g_object_new(gtype, firstProp, newArgs));
	}

	protected GObject(GType gtype, Map<String, Object> args) {
		this(getInitializer(gtype, args));
	}

	/**
	 * Sets the value of a <tt>GObject</tt> property.
	 * 
	 * @param property
	 *            The property to set.
	 * @param data
	 *            The value for the property. This must be an instance of a
	 *            class which maps to the corresponding GType.
	 */
	public void set(String property, Object data) {
		GParamSpec propertySpec = findProperty(property);
		if (propertySpec == null) {
			throw new IllegalArgumentException("Unknown property: " + property);
		}
		final GType propType = propertySpec.value_type;

		GValue propValue = new GValue(propType, data);
		GObjectAPI.gobj.g_object_set_property(this, property, propValue);
		propValue.unset();
	}

	/**
	 * Gets the current value of a <tt>GObject</tt> property.
	 * 
	 * @param property
	 *            The name of the property to get.
	 * 
	 * @return A java value representing the <tt>GObject</tt> property value.
	 */
	public Object get(String property) {
		GObjectAPI.GParamSpec propertySpec = findProperty(property);
		if (propertySpec == null) {
			throw new IllegalArgumentException("Unknown property: " + property);
		}
		final GType propType = propertySpec.value_type;
		GValue propValue = new GValue(propType);
		GObjectAPI.gobj.g_object_get_property(this, property, propValue);
		return propValue.unboxAndUnset();
	}

	private static void handleDispose(GObject object) {
		if (object.disposed)
			return;
		/* Make sure we're not holding a hidden strong ref anymore */
		strongReferences.remove(object);
		/* Clear out our signal handler references */
		object.signalHandlers = null;
		object.disposed = true;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		/* If the native object already went away, we have nothing to do here. */
		if (disposed)
			return;
		debugMemoryFinal(this, "REMOVING TOGGLE AND WEAK %s %s%n");
		/*
		 * Remove the weak ref notifier. We no longer care about this native
		 * object (for now). If we see a pointer from it again, we'll create a
		 * new proxy at that time.
		 */
		GObjectAPI.gobj.g_object_weak_unref(getNativeAddress(), weakNotify, null);
		/*
		 * Take away the toggle reference; this may or may not cause destruction
		 * of the native object.
		 */		
		GObjectAPI.gobj.g_object_remove_toggle_ref(getNativeAddress(), toggle, objectID);		
	}

	public synchronized long connect(String signal, Callback closure) {
		NativeLong connectID = GSignalAPI.gsignal.g_signal_connect_data(GObject.this, signal, closure, null, null, 0);
		if (connectID.intValue() == 0) {
			throw new IllegalArgumentException(String.format("Failed to connect signal '%s'", signal));
		}
		long id = connectID.longValue();
		signalHandlers.put(id, closure);
		return id;
	}

	private interface NotifyCallback extends Callback {
		public final TypeMapper TYPE_MAPPER = GTypeMapper.getInstance();

		public void onNotify(GObject object, GParamSpec param, Pointer data);
	}

	public synchronized long connectNotify(final String propName, final Callback callback) {
		/*
		 * FIXME - need to hold this trampoline's lifecycle to the signal
		 * connection
		 */
		NotifyCallback trampoline = new NotifyCallback() {

			public void onNotify(GObject object, GParamSpec param, Pointer data) {
				Method[] methods = callback.getClass().getDeclaredMethods();
				if (methods.length != 1)
					throw new RuntimeException(String.format("Callback %s must declare exactly one method", callback
							.getClass()));
				Method meth = methods[0];
				meth.setAccessible(true);
				Class<?>[] params = meth.getParameterTypes();
				if (params.length != 2)
					throw new RuntimeException(String.format("Callback %s entry must have exactly two parameters",
							callback.getClass()));
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

	public synchronized void disconnect(long id) {
		Callback cb = signalHandlers.get(id);
		if (cb == null)
			throw new IllegalArgumentException("Invalid signal handler id:" + id);
		GSignalAPI.gsignal.g_signal_handler_disconnect(GObject.this, new NativeLong(id));
		signalHandlers.remove(id);
	}

	private GObjectAPI.GParamSpec findProperty(String propertyName) {
		return GObjectAPI.gobj.g_object_class_find_property(getNativeAddress().getPointer(0), propertyName);
	}

	/*
	 * Hooks to/from native disposal. These callbacks are global statics, and
	 * should never themselves be GC'd.
	 */
	private static final GToggleNotify toggle = new GToggleNotify() {
		public void callback(Pointer data, Pointer ptr, boolean is_last_ref) {
			/*
			 * Manage the strong reference to this instance. When this is the
			 * last reference to the underlying object, remove the strong
			 * reference so it can be garbage collected. If it is owned by
			 * someone else, then make it a strong ref, so the java GObject for
			 * the underlying C object can be retained for later retrieval
			 */
			GObject o = (GObject) Internals.instanceFor(ptr);
			if (o == null) {
				return;
			}
			debugMemory(o, "TOGGLE NOTIFY %s %s %s%n", is_last_ref);
			if (is_last_ref) {
				strongReferences.remove(o);
			} else {
				strongReferences.put(o, Boolean.TRUE);
			}
		}
	};

	private static final GWeakNotify weakNotify = new GWeakNotify() {

		public void callback(Pointer data, Pointer obj) {
			GObject o = (GObject) Internals.instanceFor(obj);
			NativeObject.Internals.debugMemoryFinal("WEAK %s target=%s%n", obj, o);
			if (o == null)
				return;
			synchronized (o) {
				handleDispose(o);
			}
		}
	};
}
