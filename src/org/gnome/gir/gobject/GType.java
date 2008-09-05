
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.jna.FromNativeContext;
import com.sun.jna.Function;
import com.sun.jna.NativeLong;


public class GType extends NativeLong {
	private static final long serialVersionUID = 1L;

    private static final Map<Class<?>,Function> ifaceGetTypeMap 
    	= new ConcurrentHashMap<Class<?>, Function>();
    
    public static final void registerIface(Class<?> klass, Function getType) {
    	ifaceGetTypeMap.put(klass, getType);
    };
    
    public static GType getIfaceGType(Class<?> klass) {
    	Function f = ifaceGetTypeMap.get(klass);
    	if (f == null)
    		return INVALID;
    	NativeLong result = (NativeLong) f.invoke(NativeLong.class, null);
    	return new GType(result.longValue());
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
