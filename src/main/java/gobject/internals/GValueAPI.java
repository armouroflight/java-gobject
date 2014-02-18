
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
 * Copyright (c) 2008 Wayne Meissner
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


import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 *
 * @author wayne
 */
public interface GValueAPI extends Library {
    static GValueAPI gvalue = GNative.loadLibrary("gobject-2.0", GValueAPI.class, new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
		}
	});

    Pointer g_value_init(GValue value, GType g_type);
    GValue g_value_reset(GValue value);
    void g_value_unset(Pointer pointer);
    void g_value_set_char(GValue value, byte v_char);
    byte g_value_get_char(GValue value);
    void g_value_set_uchar(GValue value, byte v_uchar);
    byte g_value_get_uchar(GValue value);
    void g_value_set_boolean(GValue value, boolean v_boolean);
    boolean g_value_get_boolean(GValue value);
    void g_value_set_int(GValue value, int v_int);
    int g_value_get_int(GValue value);
    void g_value_set_uint(GValue value, int v_int);
    int g_value_get_uint(GValue value);
    void g_value_set_long(GValue value, NativeLong v_long);
    NativeLong g_value_get_long(GValue value);
    void g_value_set_ulong(GValue value, NativeLong v_long);
    NativeLong g_value_get_ulong(GValue value);
    void g_value_set_int64(GValue value, long v_int64);
    long g_value_get_int64(GValue value);
    void g_value_set_uint64(GValue value, long v_uint64);
    long g_value_get_uint64(GValue value);
    void g_value_set_float(GValue value, float v_float);
    float g_value_get_float(GValue value);
    void g_value_set_double(GValue value, double v_double);
    double g_value_get_double(GValue value);
    void g_value_set_enum(GValue value, int v_enum);
    int g_value_get_enum(GValue value);
    void g_value_set_string(GValue value, String v_string);
    void g_value_set_static_string (GValue value, String v_string);
    String g_value_get_string(GValue value);
    boolean g_value_type_compatible(GType src_type, GType dest_type);
    boolean g_value_type_transformable(GType src_type, GType dest_type);
    boolean g_value_transform(GValue src_value, GValue dest_value);
    
    void g_value_set_object(GValue value, GObject v_object);
    /* Should not use this one - _dup_object will be handled correctly, just use that */
    /* GObject g_value_get_object(GValue value); */
    GObject g_value_dup_object(GValue value);
    
    void g_value_set_boxed(GValue value, Pointer boxed);
    /* Do not use - 
     * Pointer g_value_get_boxed(GValue value); 
     * */
   
    Pointer g_value_dup_boxed(GValue value);    
}
