
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

import java.util.EnumSet;

/**
 * Maps to and from native int and an Enum value.
 * @author wayne
 */
public class EnumMapper {
    private static final EnumMapper mapper = new EnumMapper();
    public static EnumMapper getInstance() {
        return mapper;
    }
    
    public int intValue(Enum<?> value) {
        return value instanceof NativeEnum ? ((NativeEnum) value).getNative() : value.ordinal();
    }
    public <E extends Enum<E>> E valueOf(int value, Class<E> enumClass) {
        //
        // Just loop around all the enum values and find one that matches.
        // Storing the values in a Map might be faster, but by the time you deal
        // with locking overhead, its hardly worth it for small enums.
        // 
        for (E e : EnumSet.allOf(enumClass)) {
            if (e.ordinal() == value) {
                return e;
            }
        }
        //
        // No value found - try to find the default value for unknown values.
        // This is useful for enums that aren't fixed in stone and/or where you
        // don't want to throw an Exception for an unknown value.
        //
        try {
            //for (Field f : enumClass.getDeclaredFields()) {
            //    if (f.getAnnotation(DefaultEnumValue.class) != null) {
            //        return Enum.valueOf(enumClass, f.getName());
            //    }
            //}
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {      
            //
            // No default, so just give up and throw an exception
            //
            throw new IllegalArgumentException("No known Enum mapping for "
                    + enumClass.getName() + " value=" + value);
        }
    }
}
