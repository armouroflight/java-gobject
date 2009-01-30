
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

import gobject.internals.EnumMapper;
import gobject.internals.GValueAPI;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

/**
 * Managed peer representing a native {@code GValue} structure.  A GValue is the pair of
 * a value with its {@link GType}.  
 * 
 * @see <a href="../../gtk-doc/html/gobject/gobject-Generic-values.html">native GValue</a> 
 */
public class GValue extends com.sun.jna.Structure {
	/*< private >*/
	public volatile GType g_type;
	
	/**
	 * Native container for <tt>GValue</tt> data.
	 */
	// Public simply to pacify JNA Structure
	public static class GValueData extends com.sun.jna.Union {
		public volatile int v_int;
		public volatile long v_long;
		public volatile long v_int64;
		public volatile float v_float;
		public volatile double v_double;
		public volatile Pointer v_pointer;
	}

	public volatile GValueData data[] = new GValueData[2];	
	
	public GValue(GType type) {
		super();
		if (type == null)
			throw new NullPointerException();
		g_type = GType.INVALID;
        GValueAPI.gvalue.g_value_init(this, type);
        read();
	}
	
	public GValue(GType expected, Object object) {
		this(expected);
		set(object);
	}
	
	public GValue(Object object) {
		this(GType.fromInstance(object), object);
	}
	
	public void unset() {	
		GValueAPI.gvalue.g_value_unset(this.getPointer());
	}

    public Object unboxAndUnset() {
    	Object ret = unbox();
    	unset();
    	return ret;
    }
	
	@SuppressWarnings("unchecked")
	public Object unbox() {
		GType fundamental = g_type.getFundamental();		
		if (fundamental.equals(GType.INT)) {
			return GValueAPI.gvalue.g_value_get_int(this);
		} else if (fundamental.equals(GType.UINT)) {
			return GValueAPI.gvalue.g_value_get_uint(this);
		} else if (fundamental.equals(GType.CHAR)) {
			return Integer.valueOf(GValueAPI.gvalue.g_value_get_char(this));
		} else if (fundamental.equals(GType.UCHAR)) {
			return Integer.valueOf(GValueAPI.gvalue.g_value_get_uchar(this));
		} else if (fundamental.equals(GType.LONG)) {
			return GValueAPI.gvalue.g_value_get_long(this).longValue();
		} else if (fundamental.equals(GType.ULONG)) {
			return GValueAPI.gvalue.g_value_get_ulong(this).longValue();
		} else if (fundamental.equals(GType.INT64)) {
			return GValueAPI.gvalue.g_value_get_int64(this);
		} else if (fundamental.equals(GType.UINT64)) {
			return GValueAPI.gvalue.g_value_get_uint64(this);
		} else if (fundamental.equals(GType.BOOLEAN)) {
			return GValueAPI.gvalue.g_value_get_boolean(this);
		} else if (fundamental.equals(GType.FLOAT)) {
			return GValueAPI.gvalue.g_value_get_float(this);
		} else if (fundamental.equals(GType.DOUBLE)) {
			return GValueAPI.gvalue.g_value_get_double(this);
		} else if (fundamental.equals(GType.STRING)) {
			return GValueAPI.gvalue.g_value_get_string(this);
		} else if (fundamental.equals(GType.OBJECT)) {
			return GValueAPI.gvalue.g_value_dup_object(this);
		} else if (fundamental.equals(GType.BOXED)) {
			return GBoxed.boxedFor(g_type, GValueAPI.gvalue.g_value_dup_boxed(this));
		} else if (fundamental.equals(GType.ENUM)) {
			Class<? extends Enum> klass = (Class<? extends Enum>) GType.lookupProxyClass(g_type);
			if (!Enum.class.isAssignableFrom(klass))
				throw new RuntimeException();
			int value = GValueAPI.gvalue.g_value_get_enum(this);
			Enum<? extends Enum<?>> result = (Enum<? extends Enum<?>>) EnumMapper.getInstance().valueOf(value, klass);
			return result;
		} else {
			throw new IllegalArgumentException("Unsupported GType " + g_type);
		}
	}
	
	private void set(Object data) {
		GType fundamental = g_type.getFundamental();
	    if (fundamental.equals(GType.INT)) {
			GValueAPI.gvalue.g_value_set_int(this, (Integer) data);
		} else if (fundamental.equals(GType.UINT)) {
			GValueAPI.gvalue.g_value_set_uint(this, (Integer) data);
		} else if (fundamental.equals(GType.CHAR)) {
			GValueAPI.gvalue.g_value_set_char(this, (Byte) data);
		} else if (fundamental.equals(GType.UCHAR)) {
			GValueAPI.gvalue.g_value_set_uchar(this, (Byte) data);
		} else if (fundamental.equals(GType.LONG)) {
			GValueAPI.gvalue.g_value_set_long(this, new NativeLong((Long) data));
		} else if (fundamental.equals(GType.ULONG)) {
			GValueAPI.gvalue.g_value_set_ulong(this, new NativeLong((Long) data));
		} else if (fundamental.equals(GType.INT64)) {
			GValueAPI.gvalue.g_value_set_int64(this, (Long) data);
		} else if (fundamental.equals(GType.UINT64)) {
			GValueAPI.gvalue.g_value_set_uint64(this, (Long) data);
		} else if (fundamental.equals(GType.BOOLEAN)) {
			GValueAPI.gvalue.g_value_set_boolean(this, (Boolean) data);
		} else if (fundamental.equals(GType.FLOAT)) {
			GValueAPI.gvalue.g_value_set_float(this, (Float) data);
		} else if (fundamental.equals(GType.DOUBLE)) {
			GValueAPI.gvalue.g_value_set_double(this, (Double) data);
		} else if (fundamental.equals(GType.STRING)) {
			GValueAPI.gvalue.g_value_set_string(this, (String) data);
		} else if (fundamental.equals(GType.OBJECT)) {
			GValueAPI.gvalue.g_value_set_object(this, (GObject) data);
		} else if (fundamental.equals(GType.BOXED)) {
			Pointer ptr = GBoxed.getPointerFor(data);
			GValueAPI.gvalue.g_value_set_boxed(this, ptr);
		} else if (fundamental.equals(GType.ENUM)) {
			GValueAPI.gvalue.g_value_set_enum(this, EnumMapper.getInstance().intValue((Enum<?>) data));
		} else {
			throw new RuntimeException(String.format("Unsupported GType " + g_type));
		}
	}
	
	@Override
	public String toString() {
		return String.format("<GValue type=%s>", g_type);
	}
}
