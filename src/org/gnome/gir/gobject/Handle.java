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

import com.sun.jna.Pointer;

abstract public class Handle extends NativeValue {
    // Use this to propagate low level pointer arguments up the constructor chain
    protected static class Initializer {
        public final Pointer ptr;
        public final boolean needRef, ownsHandle;
        public Initializer() {
            this.ptr = null;
            this.needRef = false;
            this.ownsHandle = false;
        }
        public Initializer(Pointer ptr) {
        	this(ptr, false, true);
        }
        public Initializer(Pointer ptr, boolean needRef, boolean ownsHandle) {
        	if (ptr == null)
        		throw new RuntimeException("Invalid NULL pointer for initializer");
            this.ptr = ptr;
            this.needRef = needRef;
            this.ownsHandle = ownsHandle;
        }
    }	
    
    protected static final Initializer defaultInit = new Initializer();    

    public Handle() {
    }
    abstract protected void invalidate();
}
