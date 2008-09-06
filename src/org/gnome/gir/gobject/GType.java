
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
import java.util.Map;

import org.gnome.gir.repository.BaseInfo;
import org.gnome.gir.repository.Repository;

import com.sun.jna.FromNativeContext;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;


public class GType extends NativeLong {
	private static final long serialVersionUID = 1L;

    private static final Map<GType,Class<?>> classTypeMap 
    	= new HashMap<GType, Class<?>>();
    
	public static final String dynamicNamespace = "org/gnome/gir/dynamic/";

	public static final Map<String,String> overrides = new HashMap<String,String>() {
		private static final long serialVersionUID = 1L;

		{
			put("GLib.Value", "org/gnome/gir/gobject/GValue");
			put("GLib.List", "org/gnome/gir/gobject/GList");
			put("GLib.SList", "org/gnome/gir/gobject/GSList");

			put("GLib.Closure", "org/gnome/gir/gobject/GClosure");
			put("GLib.Quark", "org/gnome/gir/gobject/GQuark");
			put("GLib.TimeVal", "org/gnome/gir/gobject/GTimeVal");
			put("GLib.Scanner", "org/gnome/gir/gobject/GScanner");
			put("GLib.OptionContext", "org/gnome/gir/gobject/GOptionContext");
			put("GLib.OptionGroup", "org/gnome/gir/gobject/GOptionGroup");
			put("GLib.OptionEntry", "org/gnome/gir/gobject/GOptionEntry");
			put("GLib.String", "org/gnome/gir/gobject/GString");	
			put("GLib.Callback", "com/sun/jna/Callback");
			put("GLib.Mutex", "org/gnome/gir/gobject/GLibAPI$GMutex");
			put("GLib.StaticRecMutex", "org/gnome/gir/gobject/GLibAPI$GStaticRecMutex");			
			
			String[] glibPointerUnmapped = new String[] { "Mutex", "Cond", "FreeFunc", "DestroyNotify", "MarkupParser",
					"SpawnChildSetupFunc", "SourceFunc", "Node", "CompareFunc", "KeyFile", "PtrArray", "Func",
					"ThreadPool", "Source", "CompareDataFunc", "Array" };
			for (String unmapped : glibPointerUnmapped)
				put("GLib." + unmapped, "com/sun/jna/Pointer");
			String[] glibIntegerUnmapped = new String[] { "SpawnFlags", "SeekType", };
			for (String unmapped : glibIntegerUnmapped)
				put("GLib." + unmapped, "java/lang/Integer");			
			
			put("GObject.ParamSpec", "org/gnome/gir/gobject/GObjectAPI$GParamSpec");			
			put("GObject.Object", "org/gnome/gir/gobject/GObject");
			put("GObject.InitiallyUnowned", "org/gnome/gir/gobject/GInitiallyUnowned");					
			put("GObject.Type", "org/gnome/gir/gobject/GType");
			put("GObject.Value", "org/gnome/gir/gobject/GValue");			
			put("GObject.TypePlugin", "org/gnome/gir/gobject/GTypePlugin");
			put("GObject.TypeModule", "org/gnome/gir/gobject/GTypeModule");	
			put("GObject.TypeClass", "org/gnome/gir/gobject/GObjectAPI$GTypeClass");			
			put("GObject.TypeQuery", "org/gnome/gir/gobject/GObjectAPI$GTypeQuery");
			put("GObject.TypeInfo", "org/gnome/gir/gobject/GObjectAPI$GTypeInfo");
			put("GObject.InterfaceInfo", "org/gnome/gir/gobject/GObjectAPI$GInterfaceInfo");
			put("GObject.TypeValueTable", "org/gnome/gir/gobject/GObjectAPI$GTypeValueTable");			
			put("GObject.TypeFundamentalInfo", "org/gnome/gir/gobject/GObjectAPI$GTypeFundamentalInfo");			
			put("GObject.Class", "org/gnome/gir/gobject/GObjectAPI$GObjectClass");
			put("GObject.InitiallyUnownedClass", "org/gnome/gir/gobject/GObjectAPI$GInitiallyUnownedClass");			
			put("GObject.TypeDebugFlags", "org/gnome/gir/gobject/GObjectAPI$GTypeDebugFlags");
			put("GObject.TypeInstance", "org/gnome/gir/gobject/GObjectAPI$GTypeInstance");
			put("GObject.TypeInterface", "org/gnome/gir/gobject/GObjectAPI$GTypeInterface");			
			put("GObject.String", "org/gnome/gir/gobject/GString");
			put("GObject.Closure", "org/gnome/gir/gobject/GClosure");			
			put("GObject.SignalInvocationHint", "org/gnome/gir/gobject/GSignalAPI$GSignalInvocationHint");			
			put("GObject.EnumValue", "org/gnome/gir/gobject/GObjectAPI$GEnumValue");
			put("GObject.EnumClass", "org/gnome/gir/gobject/GObjectAPI$GEnumClass");			
			put("GObject.FlagsValue", "org/gnome/gir/gobject/GObjectAPI$GFlagsValue");
			put("GObject.FlagsClass", "org/gnome/gir/gobject/GObjectAPI$GFlagsClass");
			
			String[] gobjectUnmapped = new String[] { "Callback", "BaseInitFunc", "InstanceInitFunc", 
					"SignalAccumulator", "ClosureMarshal", "ClassInitFunc", "SignalEmissionHook",
					"IOChannel", "Date", "BaseFinalizeFunc", "ClassFinalizeFunc" };
			for (String unmapped : gobjectUnmapped)
				put("GObject." + unmapped, "com/sun/jna/Pointer");
			String[] gobjectIntegerUnmapped = new String[] { "SignalFlags", "ConnectFlags", "SignalMatchType", 
					"TypeFlags", "ParamFlags"  };
			for (String unmapped : gobjectIntegerUnmapped)
				put("GObject." + unmapped, "java/lang/Integer");				
			
			for (String name : new String[] { "Context" }) {
				put("Cairo." + name, "com/sun/jna/Pointer");
			}
		}
	};

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
    
    /* If we haven't yet seen a GType, we do a full search of the repository.  This
     * is VERY slow right now, so it's cached.
     */
    private static synchronized final Class<?> lookupProxyClass(NativeLong g_type) {
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
    
    public static final Class<?> lookupProxyClass(Pointer ptr) {
    	Pointer g_class = ptr.getPointer(0);
    	NativeLong g_type = g_class.getNativeLong(0);
    	return lookupProxyClass(g_type);
    };
    
    /**
     * Find the associated GType of a class.
     * @param klass
     * @return
     */
    public static final <T extends RegisteredType> GType of(Class<T> klass) {
    	try {
    		Method m = klass.getMethod("getGType", new Class<?>[] {});
    		return (GType) m.invoke(null, new Object[] {});
    	} catch (Exception e) {
    		throw new RuntimeException(e);
    	}
    }
    
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
            return cache[(int)value >> 2];
        }
        return new GType(value);
    }
    public static GType valueOf(Class<?> javaType) {
        if (Integer.class == javaType || int.class == javaType) {
            return INT;
        } else if (Long.class == javaType || long.class == javaType) {
            return INT64;
        } else if (Float.class == javaType || float.class == javaType) {
            return FLOAT;
        } else if (Double.class == javaType || double.class == javaType) {
            return DOUBLE;
        } else if (String.class == javaType) {
            return STRING;
        } else {
            throw new IllegalArgumentException("No GType for " + javaType);
        }
    }
    @Override
    public Object fromNative(Object nativeValue, FromNativeContext context) {
        return valueOf(((Number) nativeValue).longValue());
    }
    
    public String toString() {
    	return "GType(" + GObjectAPI.gobj.g_type_name(this) + ")";
    }
}
