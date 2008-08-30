
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
/**
 * 
 */
package org.gnome.gir.gobject;

import com.sun.jna.Pointer;

public final class GSignalQuery extends com.sun.jna.Structure {
    public int signal_id;
    public String signal_name;
    public GType itype;
    public int /* GSignalFlags */ signal_flags;
    public GType return_type; /* mangled with G_SIGNAL_TYPE_STATIC_SCOPE flag */
    public int n_params;
    public Pointer param_types; /* mangled with G_SIGNAL_TYPE_STATIC_SCOPE flag */
}