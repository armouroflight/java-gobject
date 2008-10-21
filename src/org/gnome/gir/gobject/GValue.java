
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

import java.net.URI;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public class GValue extends com.sun.jna.Structure {
	/*< private >*/
	public volatile GType g_type;
	
	private boolean ownsHandle;
	
	protected GValue() {
	}
	
	public GValue(GType type) {
		super();
		ownsHandle = true;
		if (type == null)
			throw new NullPointerException();
		g_type = GType.INVALID;
        GValueAPI.gvalue.g_value_init(this, type);
	}
	
	@Override
	public void finalize() throws Throwable {
		if (ownsHandle) {
			GValueAPI.gvalue.g_value_unset(this.getPointer());
			ownsHandle = false;
		}
		super.finalize();
	}
	
	public static GValue box(Object obj) {
		GType type = GType.valueOf(obj.getClass());
		GValue val = new GValue(type);
		val.set(obj);
		return val;
	}
	
	/* public for GTypeValueTable methods */
	public static class GValueData extends com.sun.jna.Union {
		public volatile int v_int;
		public volatile long v_long;
		public volatile long v_int64;
		public volatile float v_float;
		public volatile double v_double;
		public volatile Pointer v_pointer;
	}

	public volatile GValueData data[] = new GValueData[2];
	
    private static GValue transform(GValue src, GType dstType) {
        GValue dst = new GValue(dstType);
        GValueAPI.gvalue.g_value_transform(src, dst);
        return dst;
    }
    private static void transform(Object data, GType type, GValue dst) {
        GValue src = new GValue(type);
        setGValue(src, type, data);
        GValueAPI.gvalue.g_value_transform(src, dst);
    }	
    
    private static boolean setGValue(GValue value, GType type, Object data) {
        if (type.equals(GType.INT)) {
            GValueAPI.gvalue.g_value_set_int(value, intValue(data));
        } else if (type.equals(GType.UINT)) {
            GValueAPI.gvalue.g_value_set_uint(value, intValue(data));
        } else if (type.equals(GType.CHAR)) {
            GValueAPI.gvalue.g_value_set_char(value, (byte) intValue(data));
        } else if (type.equals(GType.UCHAR)) {
            GValueAPI.gvalue.g_value_set_uchar(value, (byte) intValue(data));
        } else if (type.equals(GType.LONG)) {
            GValueAPI.gvalue.g_value_set_long(value, new NativeLong(longValue(data)));
        } else if (type.equals(GType.ULONG)) {
            GValueAPI.gvalue.g_value_set_ulong(value, new NativeLong(longValue(data)));
        } else if (type.equals(GType.INT64)) {
            GValueAPI.gvalue.g_value_set_int64(value, longValue(data));
        } else if (type.equals(GType.UINT64)) {
            GValueAPI.gvalue.g_value_set_uint64(value, longValue(data));
        } else if (type.equals(GType.BOOLEAN)) {
            GValueAPI.gvalue.g_value_set_boolean(value, booleanValue(data));
        } else if (type.equals(GType.FLOAT)) {
            GValueAPI.gvalue.g_value_set_float(value, floatValue(data));
        } else if (type.equals(GType.DOUBLE)) {
            GValueAPI.gvalue.g_value_set_double(value, doubleValue(data));
        } else {
            return false;
        }
        return true;
    }
    
    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        throw new IllegalArgumentException("Expected boolean value, not " + value.getClass());
    }
    private static int intValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else if (value instanceof Enum) {
        	return EnumMapper.getInstance().intValue((Enum<?>) value);
        }
        throw new IllegalArgumentException("Expected integer value, not " + value.getClass());
    }
    private static long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        } else if (value instanceof Enum) {
        	return EnumMapper.getInstance().intValue((Enum<?>) value);
        }
        throw new IllegalArgumentException("Expected long value, not " + value.getClass());
    }
    private static float floatValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            return Float.parseFloat((String) value);
        }
        throw new IllegalArgumentException("Expected float value, not " + value.getClass());
    }
    private static double doubleValue(Object value) {
        if (value instanceof Number) {
            return  ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        throw new IllegalArgumentException("Expected double value, not " + value.getClass());
    }    
	
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
		} else if (GValueAPI.gvalue.g_value_type_transformable(g_type, GType.OBJECT)) {
			return GValueAPI.gvalue.g_value_dup_object(transform(this, GType.OBJECT));
		} else if (GValueAPI.gvalue.g_value_type_transformable(g_type, GType.INT)) {
			return GValueAPI.gvalue.g_value_get_int(transform(this, GType.INT));
		} else if (GValueAPI.gvalue.g_value_type_transformable(g_type, GType.INT64)) {
			return GValueAPI.gvalue.g_value_get_int64(transform(this, GType.INT64));
		} else {
			throw new IllegalArgumentException("Unknown conversion from GType=" + g_type);
		}
	}
	
	public void set(Object data) {
		GType fundamental = g_type.getFundamental();
	    if (fundamental.equals(GType.INT)) {
			GValueAPI.gvalue.g_value_set_int(this, intValue(data));
		} else if (fundamental.equals(GType.UINT)) {
			GValueAPI.gvalue.g_value_set_uint(this, intValue(data));
		} else if (fundamental.equals(GType.CHAR)) {
			GValueAPI.gvalue.g_value_set_char(this, (byte) intValue(data));
		} else if (fundamental.equals(GType.UCHAR)) {
			GValueAPI.gvalue.g_value_set_uchar(this, (byte) intValue(data));
		} else if (fundamental.equals(GType.LONG)) {
			GValueAPI.gvalue.g_value_set_long(this, new NativeLong(longValue(data)));
		} else if (fundamental.equals(GType.ULONG)) {
			GValueAPI.gvalue.g_value_set_ulong(this, new NativeLong(longValue(data)));
		} else if (fundamental.equals(GType.INT64)) {
			GValueAPI.gvalue.g_value_set_int64(this, longValue(data));
		} else if (fundamental.equals(GType.UINT64)) {
			GValueAPI.gvalue.g_value_set_uint64(this, longValue(data));
		} else if (fundamental.equals(GType.BOOLEAN)) {
			GValueAPI.gvalue.g_value_set_boolean(this, booleanValue(data));
		} else if (fundamental.equals(GType.FLOAT)) {
			GValueAPI.gvalue.g_value_set_float(this, floatValue(data));
		} else if (fundamental.equals(GType.DOUBLE)) {
			GValueAPI.gvalue.g_value_set_double(this, doubleValue(data));
		} else if (fundamental.equals(GType.STRING)) {
			//
			// Special conversion of java URI to gstreamer compatible uri
			//
			if (data instanceof URI) {
				URI uri = (URI) data;
				String uriString = uri.toString();
				// Need to fixup file:/ to be file:/// for gstreamer
				if ("file".equals(uri.getScheme()) && uri.getHost() == null) {
					final String path = uri.getRawPath();
					if (com.sun.jna.Platform.isWindows()) {
						uriString = "file:/" + path;
					} else {
						uriString = "file://" + path;
					}
				}
				GValueAPI.gvalue.g_value_set_string(this, uriString);
			} else {
				GValueAPI.gvalue.g_value_set_string(this, data.toString());
			}
		} else if (fundamental.equals(GType.OBJECT)) {
			GValueAPI.gvalue.g_value_set_object(this, (GObject) data);
		} else if (fundamental.equals(GType.BOXED)) {
			Pointer ptr = GBoxed.getPointerFor(data);
			GValueAPI.gvalue.g_value_set_boxed(this, ptr);
		} else if (GValueAPI.gvalue.g_value_type_transformable(GType.INT64, g_type)) {
			transform(data, GType.INT64, this);
		} else if (GValueAPI.gvalue.g_value_type_transformable(GType.LONG, g_type)) {
			transform(data, GType.LONG, this);
		} else if (GValueAPI.gvalue.g_value_type_transformable(GType.INT, g_type)) {
			transform(data, GType.INT, this);
		} else if (GValueAPI.gvalue.g_value_type_transformable(GType.DOUBLE, g_type)) {
			transform(data, GType.DOUBLE, this);
		} else if (GValueAPI.gvalue.g_value_type_transformable(GType.FLOAT, g_type)) {
			transform(data, GType.FLOAT, this);
		} else {
			throw new RuntimeException(String.format("Unsupported transformation of GType %s",  g_type));
		}
	}
}
