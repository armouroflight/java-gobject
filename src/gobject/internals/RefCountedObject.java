
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
 * Copyright (c) 2007, 2008 Wayne Meissner
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

/**
 * A {@link NativeObject} that has an associated reference count
 * @author wayne
 */
abstract public class RefCountedObject extends NativeObject {
    /** Creates a new instance of RefCountedObject */
    protected RefCountedObject(Initializer init) {
        super(init);
        if (!init.ownsRef) {
            ref();
        }
    }
    // overridden in subclasses
    abstract protected void ref();
    abstract protected void unref();
    
    @Override
    protected void finalize() throws Throwable {
    	NativeObject.Internals.debugMemory("UNREF %s%n", this);
    	unref();
    	super.finalize();
    }
}
