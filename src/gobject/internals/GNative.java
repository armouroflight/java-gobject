
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;

/**
 *
 */
public final class GNative {
    static {
        // We never want to know about errors, and preserving them just slows things down.
        com.sun.jna.Native.setPreserveLastError(false);
    }
    private GNative() {
    }

    public static <T extends Library> T loadLibrary(String name, Class<T> interfaceClass, Map<String, ?> options) {
        if (Platform.isWindows()) {
            return loadWin32Library(name, interfaceClass, options);
        }
        return loadNativeLibrary(name, interfaceClass, options);
    }
    private static <T extends Library> T loadNativeLibrary(String name, Class<T> interfaceClass, Map<String, ?> options) {
        T library = interfaceClass.cast(Native.loadLibrary(name, interfaceClass, options));
        boolean needCustom = false;
        for (Method m : interfaceClass.getMethods()) {
            for (Class<?> cls : m.getParameterTypes()) {
                if (cls.isArray() && getConverter(cls.getComponentType()) != null) {
                    needCustom = true;
                }
            }
        }
        
        //
        // If no custom conversions are needed, just return the JNA proxy
        //
        if (!needCustom) {
            return library;
        } else {
//            System.out.println("Using custom library proxy for " + interfaceClass.getName());
            return interfaceClass.cast(Proxy.newProxyInstance(interfaceClass.getClassLoader(), 
                new Class[]{ interfaceClass }, new Handler<T>(library, options)));
        }
    }
    private static <T extends Library> T loadWin32Library(String name, Class<T> interfaceClass, Map<String, ?> options) {        
        //
        // gstreamer on win32 names the dll files one of foo.dll, libfoo.dll and libfoo-0.dll
        //
        String[] nameFormats = { 
            "%s", "lib%s", "lib%s-0",                   
        };
        for (int i = 0; i < nameFormats.length; ++i) {
            try {
                return interfaceClass.cast(loadNativeLibrary(String.format(nameFormats[i], name), interfaceClass, options));
            } catch (UnsatisfiedLinkError ex) {                
                continue;
            }
        }
        throw new UnsatisfiedLinkError("Could not load library " + name);
    }
    private static NativeLibrary getWin32NativeLibrary(String name) {
        //
        // gstreamer on win32 names the dll files one of foo.dll, libfoo.dll and libfoo-0.dll
        //
        String[] nameFormats = { 
            "%s", "lib%s", "lib%s-0",                   
        };
        for (int i = 0; i < nameFormats.length; ++i) {
            try {
                return NativeLibrary.getInstance(String.format(nameFormats[i], name));
            } catch (UnsatisfiedLinkError ex) {                
                continue;
            }
        }
        throw new UnsatisfiedLinkError("Could not load library " + name);
    }
    public static NativeLibrary getNativeLibrary(String name) {
        if (Platform.isWindows()) {
            return getWin32NativeLibrary(name);
        }
        return NativeLibrary.getInstance(name);
    }
    private static interface Converter {
        Class<?> nativeType();
        Object toNative(Object value);
        Object fromNative(Object value, Class<?> javaType);
    }
    private static final Converter enumConverter = new Converter() {

        public Class<?> nativeType() {
            return int.class;
        }

        public Object toNative(Object value) {
            return value != null ? EnumMapper.getInstance().intValue((Enum<?>) value) : 0;
        }
        @SuppressWarnings(value = "unchecked")
        public Object fromNative(Object value, Class javaType) {
            return EnumMapper.getInstance().valueOf((Integer) value, javaType);
        }

    };
    private static final Converter booleanConverter = new Converter() {

        public Class<?> nativeType() {
            return int.class;
        }

        public Object toNative(Object value) {
            return value != null ? Boolean.TRUE.equals(value) ? 1 : 0 : 0;
        }
        @SuppressWarnings(value = "unchecked")
        public Object fromNative(Object value, Class javaType) {
            return value != null ? ((Integer) value).intValue() != 0 : 0;
        }

    };
    private static Converter getConverter(Class<?> javaType) {
        if (Enum.class.isAssignableFrom(javaType)) {
            return enumConverter;
        } else if (boolean.class == javaType || Boolean.class == javaType) {
            return booleanConverter;
        }
        return null;
    }
    private static class Handler<T> implements InvocationHandler {
        private final InvocationHandler proxy;
        @SuppressWarnings("unused")
		private final T library;
        
        public Handler(T library, Map<String, ?> options) {
            this.library = library;
            this.proxy = Proxy.getInvocationHandler(library);
        }
        
        public Object invoke(Object self, Method method, Object[] args) throws Throwable {
            int lastArg = args != null ? args.length : 0;
            if (method.isVarArgs()) {
                --lastArg;
            }
            Runnable[] postInvoke = null;
            int postCount = 0;
            for (int i = 0; i < lastArg; ++i) {
                if (args[i] == null) {
                    continue;
                }
                final Class<?> cls = args[i].getClass();
                if (!cls.isArray() || cls.getComponentType().isPrimitive() || cls.getComponentType() == String.class) {
                    continue;
                }
                final Converter converter = getConverter(cls.getComponentType());
                if (converter != null) {
                    final Object[] src = (Object[]) args[i];
                    final Object dst = java.lang.reflect.Array.newInstance(converter.nativeType(), src.length);
                    final ArrayIO io = getArrayIO(converter.nativeType());
                    for (int a = 0; a < src.length; ++a) {
                        io.set(dst, a, converter.toNative(src[a]));
                    }                    
                    if (postInvoke == null) {
                        postInvoke = new Runnable[lastArg];
                    }
                    postInvoke[postCount++] = new Runnable() {

                        public void run() {
                            for (int a = 0; a < src.length; ++a) {
                                src[a] = converter.fromNative(io.get(dst, a), cls.getComponentType());
                            }
                        }
                    };
                    args[i] = dst;
                }
            }
            Object retval = proxy.invoke(self, method, args);
            //
            // Reload any native arrays into java arrays
            //
            for (int i = 0; i < postCount; ++i) {
                postInvoke[i].run();
            }
            return retval;
        }
        
        
        Class<?> getNativeClass(Class<?> cls) {
            if (cls == Integer.class) {
                return int.class;
            } else if (cls == Long.class) {
                return long.class;
            }
            return cls;
        }
        private static interface ArrayIO {
            public void set(Object array, int index, Object data);
            public Object get(Object array, int index);
        }
        private static final ArrayIO intArrayIO = new ArrayIO() {
            public void set(Object array, int index, Object data) {
                java.lang.reflect.Array.setInt(array, index, data != null ? (Integer) data : 0);
            }
            public Object get(Object array, int index) {
                return java.lang.reflect.Array.getInt(array, index);
            }
        };
        private static final ArrayIO longArrayIO = new ArrayIO() {
            public void set(Object array, int index, Object data) {
                java.lang.reflect.Array.setLong(array, index, data != null ? (Long) data : 0);
            }
            public Object get(Object array, int index) {
                return java.lang.reflect.Array.getLong(array, index);
            }
        };
        private static ArrayIO getArrayIO(final Class<?> cls) {
            if (cls == int.class || cls == Integer.class) {
                return intArrayIO;
            } else if (cls == long.class || cls == Long.class) {
                return longArrayIO;
            }
            throw new IllegalArgumentException("No such conversion");
        }
    }
}
