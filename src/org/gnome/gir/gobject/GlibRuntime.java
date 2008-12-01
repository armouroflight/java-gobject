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

package org.gnome.gir.gobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gnome.gir.repository.Transfer;
import org.gnome.gir.runtime.GObject;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public class GlibRuntime {
	private static final class CallbackData {
		Callback callback;
		GlibAPI.GDestroyNotify destroy;
	}
	/*
	 * We want to hold a strong reference to both the callback and the destroy
	 * notify, until the notify is called.
	 */
	private static final Set<CallbackData> outstandingCallbacks 
		= Collections.synchronizedSet(new HashSet<CallbackData>());

	public static final String toStringAndGFree(Pointer ptr) {
		String result = ptr.getString(0);
		GlibAPI.glib.g_free(ptr);
		return result;
	}
	
	public static final GlibAPI.GDestroyNotify createDestroyNotify(Callback callback) {
		if (callback == null)
			return null;
		
		final CallbackData data = new CallbackData();
		GlibAPI.GDestroyNotify destroy = new GlibAPI.GDestroyNotify() {
			@Override
			public void callback(Pointer ignored) {
				outstandingCallbacks.remove(data);
			}
		};
		data.callback = callback;
		data.destroy = destroy;
		outstandingCallbacks.add(data);
		return destroy;
	}
	
	public static List<String> convertListUtf8(GenericGList glist, Transfer transfer) {
		List<String> ret = new ArrayList<String>();
		GenericGList origList = glist;
		boolean stringFree = transfer.equals(Transfer.EVERYTHING);
		while (glist != null) {
			Pointer data = glist.getData();
			String p = data.getString(0);
			if (stringFree)
				GlibAPI.glib.g_free(data);
			ret.add(p);
			glist = glist.getNext();
		}
		if (!transfer.equals(Transfer.NOTHING))
			origList.free();
		return ret;
	}
	
	public static List<GObject> convertListGObject(GenericGList glist, Transfer transfer, Class<? extends NativeObject> klass) {
		List<GObject> ret = new ArrayList<GObject>();
		GenericGList origList = glist;		
		boolean objTransfer = transfer.equals(Transfer.EVERYTHING);
		while (glist != null) {
			GObject obj = (GObject) NativeObject.Internals.objectFor(glist.getData(), klass, objTransfer);
			ret.add(obj);
			glist = glist.getNext();
		}
		if (!transfer.equals(Transfer.NOTHING))
			origList.free();		
		return ret;
	}		
}
