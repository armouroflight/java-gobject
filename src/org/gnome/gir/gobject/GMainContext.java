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

/**
 *
 */
public class GMainContext extends RefCountedObject {
    private static GlibAPI glib = GlibAPI.glib;
    
    public GMainContext() {
        this(initializer(glib.g_main_context_new()));
    }
    private GMainContext(Initializer init) {
        super(init);
    }
    public int attach(GSource source) {
        return glib.g_source_attach(source, this);
    }
    public static GMainContext getDefaultContext() {
        return new GMainContext(initializer(glib.g_main_context_default(), false, false));
    }
    
    protected void ref() {
        glib.g_main_context_ref(handle());
    }
    protected void unref() {
        glib.g_main_context_unref(handle());
    }

    @Override
    protected void disposeNativeHandle(Pointer ptr) {
        glib.g_main_context_unref(ptr);
    }
}
