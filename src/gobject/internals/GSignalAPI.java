
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
 * Copyright (c) 2008 Wayne Meissner
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

import gobject.internals.GObjectAPI.GClosureNotify;
import gobject.runtime.GObject;
import gobject.runtime.GType;

import java.util.HashMap;


import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 *
 * @author wayne
 */
public interface GSignalAPI extends Library {
    static GSignalAPI gsignal = GNative.loadLibrary("gobject-2.0", GSignalAPI.class, new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
		}
	});
    
    public interface GSignalFlags {
    	public static final int RUN_FIRST	= 1 << 0;
    	public static final int RUN_LAST	= 1 << 1;
    	public static final int RUN_CLEANUP	= 1 << 2;
    	public static final int NO_RECURSE	= 1 << 3;
    	public static final int DETAILED	= 1 << 4;
    	public static final int ACTION	    = 1 << 5;
    	public static final int NO_HOOKS	= 1 << 6;
    };
    
    public static int G_CONNECT_AFTER = 1 << 0;
    public static int G_CONNECT_SWAPPED = 1 << 1;
    
    NativeLong g_signal_connect_data(GObject obj, String signal, Callback callback, Pointer data,
            GClosureNotify destroy_data, int connect_flags);
    void g_signal_handler_disconnect(GObject obj, NativeLong id);
    
    int g_signal_lookup(String name, GType itype);
    String g_signal_name(int signal_id);
    void g_signal_query(int signal_id, GSignalQuery query);
    int g_signal_list_ids(GType itype, int[] n_ids);
    
    // Do nothing, but provide a base Callback class that gets automatic type conversion
    public static interface GSignalCallbackProxy extends com.sun.jna.CallbackProxy {}
    
    public static final class GSignalInvocationHint extends Structure
    {
      public int		signal_id;
      public GQuark	detail;
      public int	run_type;
    };
    
}
