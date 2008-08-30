
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;

import org.gnome.gir.gobject.annotation.Return;
import org.gnome.gir.gobject.annotation.ConstField;
import org.gnome.gir.gobject.annotation.IncRef;
import org.gnome.gir.gobject.annotation.Invalidate;

import com.sun.jna.CallbackParameterContext;
import com.sun.jna.FromNativeContext;
import com.sun.jna.FromNativeConverter;
import com.sun.jna.FunctionResultContext;
import com.sun.jna.MethodParameterContext;
import com.sun.jna.MethodResultContext;
import com.sun.jna.Pointer;
import com.sun.jna.StructureReadContext;
import com.sun.jna.ToNativeContext;
import com.sun.jna.ToNativeConverter;
import com.sun.jna.TypeConverter;

/**
 *
 * @author wayne
 */
public class GTypeMapper extends com.sun.jna.DefaultTypeMapper {
    public GTypeMapper() {
        addToNativeConverter(URI.class, uriConverter);    	
    }
    
    private static ToNativeConverter nativeValueArgumentConverter = new ToNativeConverter() {

        public Object toNative(Object arg, ToNativeContext context) {
            return arg != null ? ((NativeValue) arg).nativeValue() : null;
        }

        public Class<?> nativeType() {
            return Void.class; // not really correct, but not used in this instance
        }        
    };    
 
    private static TypeConverter nativeObjectConverter = new TypeConverter() {
        public Object toNative(Object arg, ToNativeContext context) {
            if (arg == null) {
                return null;
            }
            Pointer ptr = ((NativeObject) arg).handle();
            
            //
            // Deal with any adjustments to the proxy neccessitated by gstreamer
            // breaking their reference-counting idiom with special cases
            //
            if (context instanceof MethodParameterContext) {
                MethodParameterContext mcontext = (MethodParameterContext) context;
                Method method = mcontext.getMethod();
                int index = mcontext.getParameterIndex();
                Annotation[][] parameterAnnotations = method.getParameterAnnotations();
                if (index < parameterAnnotations.length) {
                    Annotation[] annotations = parameterAnnotations[index];
                    for (int i = 0; i < annotations.length; ++i) {
                        if (annotations[i] instanceof Invalidate) {
                            ((Handle) arg).invalidate();
                            break;
                        } else if (annotations[i] instanceof IncRef) {
                            ((RefCountedObject) arg).ref();
                            break;
                        }
                    }
                }
            }
            return ptr;
        }
 
        @SuppressWarnings(value = "unchecked")
        public Object fromNative(Object result, FromNativeContext context) {
            if (result == null) {
                return null;
            }
            if (context instanceof MethodResultContext) {
                //
                // By default, gstreamer increments the refcount on objects 
                // returned from functions, so drop a ref here
                //
                boolean ownsHandle = ((MethodResultContext) context).getMethod().isAnnotationPresent(Return.class);
                int refadj = ownsHandle ? -1 : 0;
                return NativeObject.objectFor((Pointer) result, context.getTargetType(), refadj, ownsHandle);
            }
            if (context instanceof CallbackParameterContext) {
                return NativeObject.objectFor((Pointer) result, context.getTargetType(), 1, true);
            }
            if (context instanceof StructureReadContext) {
                StructureReadContext sctx = (StructureReadContext) context;
                boolean ownsHandle = sctx.getField().getAnnotation(ConstField.class) == null;
                return NativeObject.objectFor((Pointer) result, context.getTargetType(), 1, ownsHandle);
            }
            if (context instanceof FunctionResultContext) {
            	return NativeObject.objectFor((Pointer) result, context.getTargetType(), 0, true);
            }
            throw new IllegalStateException("Cannot convert to NativeObject from " + context);
        }
        
        public Class<?> nativeType() {
            return Pointer.class;
        }
    };
    private static TypeConverter enumConverter = new TypeConverter() {

        @SuppressWarnings(value = "unchecked")
        public Object fromNative(Object value, FromNativeContext context) {
            return EnumMapper.getInstance().valueOf((Integer) value, context.getTargetType());
        }

        public Class<?> nativeType() {
            return Integer.class;
        }

        @SuppressWarnings("unchecked")
        public Object toNative(Object arg, ToNativeContext context) {
            if (arg == null) {
                return null;
            }
            return EnumMapper.getInstance().intValue((Enum) arg);
        }
    };

    private TypeConverter stringConverter = new TypeConverter() {

        public Object fromNative(Object result, FromNativeContext context) {
            if (result == null) {
                return null;
            }
            if (context instanceof MethodResultContext) {
                MethodResultContext functionContext = (MethodResultContext) context;
                Method method = functionContext.getMethod();
                Pointer ptr = (Pointer) result;
                String s = ptr.getString(0);
                if (method.isAnnotationPresent(Return.class)) {
                    GlibAPI.glib.g_free(ptr);
                }
                return s;
            } else {
                return ((Pointer) result).getString(0);
            }           
        }

        public Class<?> nativeType() {
            return Pointer.class;
        }

        public Object toNative(Object arg, ToNativeContext context) {
            // Let the default String -> native conversion handle it
            return arg;            
        }
    };

    private TypeConverter booleanConverter = new TypeConverter() {
        public Object toNative(Object arg, ToNativeContext context) {
            return Integer.valueOf(Boolean.TRUE.equals(arg) ? 1 : 0);
        }

        public Object fromNative(Object arg0, FromNativeContext arg1) {
            return Boolean.valueOf(((Integer)arg0).intValue() != 0);
        }

        public Class<?> nativeType() {
            return Integer.class;
        }
    };
    private TypeConverter gquarkConverter = new TypeConverter() {

        public Object fromNative(Object arg0, FromNativeContext arg1) {
            return new GQuark((Integer) arg0);
        }

        public Class<?> nativeType() {
            return Integer.class;
        }

        public Object toNative(Object arg0, ToNativeContext arg1) {
            return ((GQuark) arg0).intValue();
        }
    };
    
    private TypeConverter intptrConverter = new TypeConverter() {
        
        public Object toNative(Object arg, ToNativeContext context) {
            return ((IntPtr)arg).value;            
        }

        public Object fromNative(Object arg0, FromNativeContext arg1) {
            return new IntPtr(((Number) arg0).intValue());            
        }

        public Class<?> nativeType() {
            return Pointer.SIZE == 8 ? Long.class : Integer.class;
        }
    };
    private static ToNativeConverter uriConverter = new ToNativeConverter() {

        public Object toNative(Object arg0, ToNativeContext arg1) {
            URI uri = (URI) arg0;
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
            return uriString;
        }

        public Class<?> nativeType() {
            return String.class;
        }
    };
    @SuppressWarnings("unchecked")
	public FromNativeConverter getFromNativeConverter(Class type) {
        if (Enum.class.isAssignableFrom(type)) {
            return enumConverter;              
        } else if (NativeObject.class.isAssignableFrom(type)) {
            return nativeObjectConverter;
        } else if (Boolean.class == type || boolean.class == type) {
            return booleanConverter;
        } else if (String.class == type) {
            return stringConverter;
        } else if (IntPtr.class == type) {
            return intptrConverter;
        } else if (GQuark.class == type) {
            return gquarkConverter;
        }
        return super.getFromNativeConverter(type);
    }

    @SuppressWarnings("unchecked")
	public ToNativeConverter getToNativeConverter(Class type) {
        if (NativeObject.class.isAssignableFrom(type)) {
            return nativeObjectConverter;
        } else if (NativeValue.class.isAssignableFrom(type)) {
            return nativeValueArgumentConverter;
        } else if (Enum.class.isAssignableFrom(type)) {
            return enumConverter;
        } else if (Boolean.class == type || boolean.class == type) {
            return booleanConverter;
        } else if (String.class == type) {
            return stringConverter;        
        } else if (IntPtr.class == type) {
            return intptrConverter;
        } else if (GQuark.class == type) {
            return gquarkConverter;
        }
        return super.getToNativeConverter(type);
    }
}
