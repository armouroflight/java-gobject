
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
package org.gnome.gir.repository;

public interface FunctionInfoFlags {
	public static final int IS_METHOD = (1 << 0);
	public static final int IS_CONSTRUCTOR = (1 << 1);
	public static final int IS_SETTER = (1 << 2);
	public static final int IS_GETTER = (1 << 3);
	public static final int WRAPS_VFUNC = (1 << 4);
	public static final int THROWS = (1 << 5);	
}