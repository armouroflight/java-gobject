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

import java.util.HashMap;
import java.util.Map;

import org.gnome.gir.repository.BaseInfo;
import org.gnome.gir.repository.Repository;

import com.sun.jna.FromNativeContext;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 * The fundamental runtime type information of the <tt>GObject</tt> library. In native code,
 * a GType is an opaque integral type.  All subclasses of {@link GObject} and {@link GBoxed} have associated
 * native GTypes.
 * <p>
 * This class provides an API for accessing fundamental GTypes, as well as
 * looking up the GType associated with a mapped GObject.
 * <p>
 * @see <a href="../../gtk-doc/html/gobject/gobject-Type-Information.html">native GType</a>
 */
public class GType extends NativeLong {
	private static final long serialVersionUID = 1L;

	private static final Map<GType, Class<?>> classTypeMap = new HashMap<GType, Class<?>>();

	/* Namespace for generated classes - highly prominent, avoid changing */
	public static final String dynamicNamespace = "gobject/introspection/";
	/* Internals - not guaranteed public */
	public static final String internalsNamespace = "gobject/internals/";
	/* Prominent public API - should try very hard to avoid changing */
	public static final String runtimeNamespace = "gobject/runtime/";

	public static final Map<String, String> overrides = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;

		{
			put("GLib.Value", internalsNamespace + "GValue");

			put("GLib.MainContext", internalsNamespace + "GMainContext");
			put("GLib.Closure", internalsNamespace + "GClosure");
			put("GLib.Quark", internalsNamespace + "GQuark");
			put("GLib.TimeVal", internalsNamespace + "GTimeVal");
			put("GLib.Scanner", internalsNamespace + "GScanner");
			put("GLib.OptionContext", internalsNamespace + "GOptionContext");
			put("GLib.OptionGroup", internalsNamespace + "GOptionGroup");
			put("GLib.OptionEntry", internalsNamespace + "GOptionEntry");
			put("GLib.String", internalsNamespace + "GString");
			put("GLib.Callback", "com/sun/jna/Callback");
			put("GLib.Mutex", internalsNamespace + "GlibAPI$GMutex");
			put("GLib.StaticRecMutex", internalsNamespace + "GlibAPI$GStaticRecMutex");
			put("GLib.IOFunc", internalsNamespace + "GIOFunc");
			put("GLib.SourceFunc", internalsNamespace + "GSourceFunc");

			String[] glibPointerUnmapped = new String[] { "Mutex", "Cond", "FreeFunc", "DestroyNotify", "MarkupParser",
					"SpawnChildSetupFunc", "Node", "CompareFunc", "KeyFile", "PtrArray", "Func", "ThreadPool",
					"Source", "CompareDataFunc", "Array", "Data", "DataSet", "Date", "IOChannel", "Regex",
					"MappedFile", "GList", "GSList", "PollFD", "ByteArray" };
			for (String unmapped : glibPointerUnmapped)
				put("GLib." + unmapped, internalsNamespace + "UnmappedPointer");
			String[] glibIntegerUnmapped = new String[] { "SpawnFlags", "SeekType", "IOCondition", "RegexMatchFlags", "ThreadPriority" };
			for (String unmapped : glibIntegerUnmapped)
				put("GLib." + unmapped, "java/lang/Integer");

			put("GObject.Object", runtimeNamespace + "GObject");
			put("GObject.Callback", "com/sun/jna/Callback");
			put("GObject.InitiallyUnowned", runtimeNamespace + "GInitiallyUnowned");
			put("GObject.Type", runtimeNamespace + "GType");
			put("GObject.Value", runtimeNamespace + "GValue");
			put("GObject.ParamSpec", internalsNamespace + "GObjectAPI$GParamSpec");
			put("GObject.Parameter", internalsNamespace + "GObjectAPI$GParameter");
			put("GObject.TypePlugin", internalsNamespace + "GTypePlugin");
			put("GObject.TypeModule", internalsNamespace + "GTypeModule");
			put("GObject.TypeClass", internalsNamespace + "GObjectAPI$GTypeClass");
			put("GObject.TypeQuery", internalsNamespace + "GObjectAPI$GTypeQuery");
			put("GObject.TypeInfo", internalsNamespace + "GObjectAPI$GTypeInfo");
			put("GObject.InterfaceInfo", internalsNamespace + "GObjectAPI$GInterfaceInfo");
			put("GObject.TypeValueTable", internalsNamespace + "GObjectAPI$GTypeValueTable");
			put("GObject.TypeFundamentalInfo", internalsNamespace + "GObjectAPI$GTypeFundamentalInfo");
			put("GObject.ObjectClass", internalsNamespace + "GObjectAPI$GObjectClass");
			put("GObject.InitiallyUnownedClass", internalsNamespace + "GObjectAPI$GInitiallyUnownedClass");
			put("GObject.TypeDebugFlags", internalsNamespace + "GObjectAPI$GTypeDebugFlags");
			put("GObject.TypeInstance", internalsNamespace + "GObjectAPI$GTypeInstance");
			put("GObject.TypeInterface", internalsNamespace + "GObjectAPI$GTypeInterface");
			put("GObject.String", internalsNamespace + "GString");
			put("GObject.HashTable", internalsNamespace + "GHashTable");
			put("GObject.Closure", internalsNamespace + "GClosure");
			put("GObject.SignalInvocationHint", internalsNamespace + "GSignalAPI$GSignalInvocationHint");
			put("GObject.EnumValue", internalsNamespace + "GObjectAPI$GEnumValue");
			put("GObject.EnumClass", internalsNamespace + "GObjectAPI$GEnumClass");
			put("GObject.FlagsValue", internalsNamespace + "GObjectAPI$GFlagsValue");
			put("GObject.FlagsClass", internalsNamespace + "GObjectAPI$GFlagsClass");

			String[] gobjectUnmapped = new String[] { "BaseInitFunc", "InstanceInitFunc", "SignalAccumulator",
					"ClosureMarshal", "ClassInitFunc", "SignalEmissionHook", "IOChannel", "Date", "BaseFinalizeFunc",
					"ClassFinalizeFunc", "ValueArray" };
			for (String unmapped : gobjectUnmapped)
				put("GObject." + unmapped, internalsNamespace + "UnmappedPointer");
			String[] gobjectIntegerUnmapped = new String[] { "SignalFlags", "ConnectFlags", "SignalMatchType",
					"TypeFlags", "ParamFlags", "IOCondition" };
			for (String unmapped : gobjectIntegerUnmapped)
				put("GObject." + unmapped, "java/lang/Integer");

			put("Gio.AsyncReadyCallback", runtimeNamespace + "AsyncReadyCallback");
			put("Gio.AsyncResult", runtimeNamespace + "AsyncResult");			
			
			for (String name : new String[] { "Context" }) {
				put("Cairo." + name, internalsNamespace + "UnmappedPointer");
			}
		}
	};
	
	public static boolean isMapped(String namespace, String name) {
		String key = namespace + "." + name;
		String val = GType.overrides.get(key);
		return val != null;
	}

	public static String getInternalNameMapped(String namespace, String name) {
		String key = namespace + "." + name;
		String val = GType.overrides.get(key);
		if (val != null)
			return val;
		if (namespace.equals("GLib") || namespace.equals("GObject"))
			throw new RuntimeException(String.format("Unmapped internal ns=%s name=%s", namespace, name));
		return getInternalName(namespace, name);
	}

	public static String getInternalName(String namespace, String name) {
		String caps = name.substring(0, 1).toUpperCase() + name.substring(1);
		return dynamicNamespace + namespace + "/" + caps;
	}

	public static String getPublicNameMapped(String namespace, String name) {
		return getInternalNameMapped(namespace, name).replace('/', '.');
	}

	public static final void registerProxyClass(GType gtype, Class<?> klass) {
		classTypeMap.put(gtype, klass);
	};

	/*
	 * If we haven't yet seen a GType, we do a full search of the repository.
	 * This is VERY slow right now, so it's cached.
	 */
	public static synchronized final Class<?> lookupProxyClass(NativeLong g_type) {
		Class<?> klass = classTypeMap.get(g_type);
		if (klass != null)
			return klass;
		BaseInfo info = Repository.getDefault().findByGType(g_type);
		if (info == null)
			return null;
		String klassName = getPublicNameMapped(info.getNamespace(), info.getName());
		try {
			klass = Class.forName(klassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		classTypeMap.put(new GType(g_type.longValue()), klass);
		return klass;
	}

	public final Class<?> lookupProxyClass() {
		return lookupProxyClass((NativeLong) this);
	}

	public static final GType objectPeekType(Pointer ptr) {
		Pointer g_class = ptr.getPointer(0);
		NativeLong g_type = g_class.getNativeLong(0);
		return valueOf(g_type.longValue());
	}

	public static final Class<?> lookupProxyClass(Pointer ptr) {
		GType gtype = objectPeekType(ptr);
		return lookupProxyClass(gtype);
	};

	public static final void init() {
		GObjectAPI.gobj.g_type_init();
	}

	private static final GType[] cache;
	static {
		cache = new GType[21];
		for (int i = 0; i < cache.length; ++i) {
			cache[i] = new GType(i << 2);
		}
	};
	public static final GType INVALID = initFundamental(0);
	public static final GType NONE = initFundamental(1);
	public static final GType INTERFACE = initFundamental(2);
	public static final GType CHAR = initFundamental(3);
	public static final GType UCHAR = initFundamental(4);
	public static final GType BOOLEAN = initFundamental(5);
	public static final GType INT = initFundamental(6);
	public static final GType UINT = initFundamental(7);
	public static final GType LONG = initFundamental(8);
	public static final GType ULONG = initFundamental(9);
	public static final GType INT64 = initFundamental(10);
	public static final GType UINT64 = initFundamental(11);
	public static final GType ENUM = initFundamental(12);
	public static final GType FLAGS = initFundamental(13);
	public static final GType FLOAT = initFundamental(14);
	public static final GType DOUBLE = initFundamental(15);
	public static final GType STRING = initFundamental(16);
	public static final GType POINTER = initFundamental(17);
	public static final GType BOXED = initFundamental(18);
	public static final GType PARAM = initFundamental(19);
	public static final GType OBJECT = initFundamental(20);

	private static GType initFundamental(int v) {
		return valueOf(v << 2);
	}

	GType(long t) {
		super(t);
	}

	public GType() {
		super(0);
	}

	public static GType valueOf(long value) {
		if (value >= 0 && (value >> 2) < cache.length) {
			return cache[(int) value >> 2];
		}
		return new GType(value);
	}

	public static GType fromClass(Class<?> klass) {
		try {
			return (GType) klass.getDeclaredMethod("getGType", new Class<?>[] {}).invoke(null, new Object[] {});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static GType fromInstance(Object obj) {
		if (obj instanceof Integer) {
			return INT;
		} else if (obj instanceof Long) {
			return INT64;
		} else if (obj instanceof Float) {
			return FLOAT;
		} else if (obj instanceof Double) {
			return DOUBLE;
		} else if (obj instanceof String) {
			return STRING;
		} else if (obj instanceof GObject || obj instanceof GObject.GObjectProxy) {
			return objectPeekType(((GObject) obj).getNativeAddress());
		} else if (obj instanceof GBoxed) {
			return ((GBoxed) obj).getGType();
		} else if (obj instanceof BoxedStructure || obj instanceof BoxedUnion) {
			GType type;
			if (obj instanceof BoxedStructure)
				type = ((BoxedStructure) obj).getGType();
			else
				type = ((BoxedUnion) obj).getGType();
			if (type.equals(GType.INVALID)) {
				type = fromClass(obj.getClass());
			}
			return type;
		} else {
			throw new IllegalArgumentException(String.format("Unhandled GType lookup for object %s", obj));
		}
	}

	public GType getFundamental() {
		return GObjectAPI.gobj.g_type_fundamental(this);
	}

	@Override
	public Object fromNative(Object nativeValue, FromNativeContext context) {
		return valueOf(((Number) nativeValue).longValue());
	}

	public GType getParent() {
		return GObjectAPI.gobj.g_type_parent(this);
	}

	public String toString() {
		return "GType(" + GObjectAPI.gobj.g_type_name(this) + ")";
	}
}
