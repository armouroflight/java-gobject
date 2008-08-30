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

import java.util.concurrent.Callable;

import com.sun.jna.Pointer;

/**
 *
 */
public class GSource extends RefCountedObject {
    
    public GSource(Initializer init) {
        super(init);
    }
    public int attach(GMainContext context) {
        return GlibAPI.glib.g_source_attach(this, context);
    }
    public void setCallback(final Callable<Boolean> call) {
        this.callback = new GlibAPI.GSourceFunc() {
            public boolean callback(Pointer data) {
                if (GlibAPI.glib.g_source_is_destroyed(handle())) {
                    return false;
                }
                try {
                    return call.call().booleanValue();
                } catch (Exception ex) {
                    return false;
                }
            }
        };
        GlibAPI.glib.g_source_set_callback(this, callback, null, null);
    }
    private GlibAPI.GSourceFunc callback;
    
    protected void ref() {
        GlibAPI.glib.g_source_ref(handle());
    }
    protected void unref() {
        GlibAPI.glib.g_source_unref(handle());
    }

    @Override
    protected void disposeNativeHandle(Pointer ptr) {
        GlibAPI.glib.g_source_destroy(ptr);
        GlibAPI.glib.g_source_unref(ptr);
    }
}
