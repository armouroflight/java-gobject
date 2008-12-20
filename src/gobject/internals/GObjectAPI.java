
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
import gobject.runtime.GValue;

import java.util.HashMap;


import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 */
public interface GObjectAPI extends Library {
    static GObjectAPI gobj = GNative.loadLibrary("gobject-2.0", GObjectAPI.class, new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
		}
	});
    
    GType g_object_get_type();
    void g_object_set_property(GObject obj, String property, GValue data);
    void g_object_get_property(GObject obj, String property, GValue data);
    void g_object_set(GObject obj, String propertyName, Object... data);
    void g_object_get(GObject obj, String propertyName, Object... data);
    Pointer g_object_new(GType object_type, String firstParam, Object... args);
    
    interface GClosureNotify extends Callback {
        void callback(Pointer data, Pointer closure);
    }
    NativeLong g_signal_connect_data(GObject obj, String signal, Callback callback, Pointer data,
            GClosureNotify destroy_data, int connect_flags);
    void g_signal_handler_disconnect(GObject obj, NativeLong id);
    boolean g_object_is_floating(GObject obj);
    interface GToggleNotify extends Callback {
        void callback(Pointer data, Pointer obj, boolean is_last_ref);
    }
    void g_object_add_toggle_ref(Pointer object, GToggleNotify notify, IntPtr data);
    void g_object_remove_toggle_ref(Pointer object, GToggleNotify notify, IntPtr data);
    interface GWeakNotify extends Callback {
        void callback(Pointer data, Pointer obj);
    }
    void g_object_weak_ref(Pointer object, GWeakNotify notify, Pointer data);
    void g_object_weak_unref(Pointer object, GWeakNotify notify, Pointer data);
    Pointer g_object_ref(GObject object);
    void g_object_ref_sink(GObject object);
    void g_object_unref(GObject object);

    GParamSpec g_object_class_find_property(GObjectClass oclass, String property_name);
    GParamSpec g_object_class_find_property(Pointer oclass, String property_name);
    GQuark g_quark_try_string(String string);
    GQuark g_quark_from_static_string(String string);
    GQuark g_quark_from_string(String string);
    String g_quark_to_string(GQuark quark);

    String g_intern_string(String string);
    String g_intern_static_string(String string);
    
    void g_type_init();
    void g_type_init_with_debug_flags(int flags);
    String g_type_name(GType type);
    //GQuark                g_type_qname                   (GType            type);
    GType g_type_from_name(String name);
    GType g_type_parent(GType type);
    GType g_type_fundamental(GType type);
    int g_type_depth(GType type);
    Pointer g_type_create_instance(GType type);
    void g_type_free_instance(Pointer instance);
    
    Pointer g_type_interface_peek(Pointer g_class, GType type);
    
    GType g_type_register_static(GType parent_type, String type_name,
        GTypeInfo info, /* GTypeFlags */ int flags);
    GType g_type_register_static(GType parent_type, Pointer type_name,
        GTypeInfo info, /* GTypeFlags */ int flags);
    GType g_type_register_static_simple(GType parent_type, String type_name,
        int class_size, GClassInitFunc class_init, int instance_size,
        GInstanceInitFunc instance_init, /* GTypeFlags */ int flags);
    GType g_type_register_static_simple(GType parent_type, Pointer type_name,
        int class_size, GClassInitFunc class_init, int instance_size,
        GInstanceInitFunc instance_init, /* GTypeFlags */ int flags);
    /* 
     * Basic Type Structures
     */
    public static final class GTypeClass extends com.sun.jna.Structure {

        /*< private >*/
        public volatile GType g_type;
    }

    public static final class GTypeInstance extends com.sun.jna.Structure {

        /*< private >*/
        public volatile Pointer g_class;
    }                  
    
    static class GObjectStruct extends com.sun.jna.Structure {
        public volatile GTypeInstance g_type_instance;
        public volatile int ref_count;
        public volatile Pointer qdata;
        public GObjectStruct() {}
        public GObjectStruct(GObject obj) {
            useMemory(obj.getNativeAddress());
            read();
        }
    }
    static public class GObjectConstructParam {
        public volatile Pointer spec;
        public volatile Pointer value;
    }
    public static final class GObjectClass extends com.sun.jna.Structure {
        public volatile GTypeClass g_type_class;
        public volatile Pointer construct_properties;
        public Constructor constructor;
        public SetProperty set_property;
        public GetProperty get_property;
        public Dispose dispose;
        public Finalize finalize;
        public volatile Pointer dispatch_properties_changed;
        public Notify notify;
        public volatile byte[] p_dummy = new byte[8 * Pointer.SIZE];
        
        public static interface Constructor extends Callback {
            public Pointer callback(GType type, int n_construct_properties, 
                    GObjectConstructParam properties);
        };
        public static interface SetProperty extends Callback {
            public void callback(GObject object, int property_id, Pointer value, Pointer spec);
        }
        public static interface GetProperty extends Callback {
            public void callback(GObject object, int property_id, Pointer value, Pointer spec);
        }
        public static interface Dispose extends Callback {
            public void callback(GObject object);
        }
        public static interface Finalize extends Callback {
            public void callback(GObject object);
        }
        public static interface Notify extends Callback {
            public void callback(GObject object, Pointer spec);
        }

        public GObjectClass() {}
        public GObjectClass(Pointer ptr) {
            useMemory(ptr);
            read();
        }
    }
    
    public static final class GInitiallyUnownedClass extends Structure {
    	private GInitiallyUnownedClass() {}
    }
    
    public static interface GBaseInitFunc extends Callback {
        public void callback(Pointer g_class);
    }

    public static interface GBaseFinalizeFunc extends Callback {
        public void callback(Pointer g_class);
    }

    public static interface GClassInitFunc extends Callback {
        public void callback(Pointer g_class, Pointer class_data);
    }

    public static interface GClassFinalizeFunc extends Callback {
        public void callback(Pointer g_class, Pointer class_data);
    }
    public static interface GInstanceInitFunc extends Callback {
        void callback(GTypeInstance instance, Pointer g_class);
    }    
    public static final class GTypeInfo extends com.sun.jna.Structure {
        public GTypeInfo() { 
            clear();
        }
        public GTypeInfo(Pointer ptr) { 
            useMemory(ptr); 
            read();
        }
        /* interface types, classed types, instantiated types */
        public short class_size;
        public GBaseInitFunc base_init;
        public GBaseFinalizeFunc base_finalize;
        /* interface types, classed types, instantiated types */
        public GClassInitFunc class_init;
        public GClassFinalizeFunc class_finalize;
        public Pointer class_data;
        /* instantiated types */
        public short instance_size;
        public short n_preallocs;
        
        public GInstanceInitFunc instance_init;

        /* value handling */
        public volatile /* GTypeValueTable */ Pointer value_table;                
    }
    
    public static final class GTypeFundamentalInfo extends Structure
    {
      public int type_flags;
    };
    
    public static final class GInterfaceInfo extends Structure
    {
      public Pointer     interface_init;
      public Pointer interface_finalize;
      public Pointer interface_data;
    };
    
    public static final class GTypeInterface extends Structure {
      /*< private >*/
      public GType g_type;         /* iface type */
      public GType g_instance_type;
    };
    
    public static final class GTypeValueTable
    {
      public Pointer value_init;
      public Pointer value_free;
      public Pointer value_copy;
      /* varargs functionality (optional) */
      public Pointer value_peek_pointer;
      public String collect_format;
      public Pointer collect_value;
      public String lcopy_format;
      public Pointer lcopy_value;
    };
    
    
    public static class GParamSpec extends com.sun.jna.Structure {
        public volatile GTypeInstance g_type_instance;
        public volatile String g_name;
        public volatile /* GParamFlags */ int g_flags;
        public volatile GType value_type;
        public volatile GType owner_type;
        /*< private >*/
        public volatile Pointer _nick;
        public volatile Pointer _blurb;
        public volatile Pointer qdata;
        public volatile int ref_count;
        public volatile int param_id;      /* sort-criteria */
        
        public GParamSpec() {}
    }
    
    public static class GParameter /* auxillary structure for _setv() variants */
    {
      String name;
      GValue value;
    };
    
    public interface GTypeDebugFlags {
    	public static final int NONE = 0;
    	public static final int OBJECTS = 1 << 0;
    	public static final int SIGNALS = 1 << 1;
    	public static final int MASK = 0x03;    
    }
    
    /**
     * GEnumClass:
     * @g_type_class: the parent class
     * @minimum: the smallest possible value.
     * @maximum: the largest possible value.
     * @n_values: the number of possible values.
     * @values: an array of #GEnumValue structs describing the 
     *  individual values.
     * 
     * The class of an enumeration type holds information about its 
     * possible values.
     */
    public static final class GEnumClass extends Structure
    {
      public GTypeClass  g_type_class;

      /*< public >*/  
      public int	      minimum;
      public int	      maximum;
      public int	      n_values;
      public GEnumValue[]  values;
    };
    /**
     * GFlagsClass:
     * @g_type_class: the parent class
     * @mask: a mask covering all possible values.
     * @n_values: the number of possible values.
     * @values: an array of #GFlagsValue structs describing the 
     *  individual values.
     * 
     * The class of a flags type holds information about its 
     * possible values.
     */
    public static final class GFlagsClass extends Structure
    {
      public GTypeClass   g_type_class;
      
      /*< public >*/  
      public int	       mask;
      public int	       n_values;
      public GFlagsValue[] values;
    };
    /**
     * GEnumValue:
     * @value: the enum value
     * @value_name: the name of the value
     * @value_nick: the nickname of the value
     * 
     * A structure which contains a single enum value, it's name, and it's
     * nickname.
     */
    public static final class GEnumValue extends Structure
    {
      public int	 value;
      public String value_name;
      public String value_nick;
    };
    /**
     * GFlagsValue:
     * @value: the flags value
     * @value_name: the name of the value
     * @value_nick: the nickname of the value
     * 
     * A structure which contains a single flags value, it's name, and it's
     * nickname.
     */
    public static final class GFlagsValue extends Structure
    {
      public int	 value;
      public String value_name;
      public String value_nick;
    };
    
    /**
     * GTypeQuery:
     * @type: the #GType value of the type.
     * @type_name: the name of the type.
     * @class_size: the size of the class structure.
     * @instance_size: the size of the instance structure.
     * 
     * A structure holding information for a specific type. It is
     * filled in by the g_type_query() function.
     */
    public static final class GTypeQuery extends Structure
    {
      public GType		type;
      public String type_name;
      public int		class_size;
      public int		instance_size;
    };
    
    
}
