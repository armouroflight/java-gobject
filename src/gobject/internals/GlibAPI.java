
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
import gobject.runtime.MainLoop;

import java.util.HashMap;


import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.ptr.PointerByReference;

/**
 *
 */
public interface GlibAPI extends Library {
    static GlibAPI glib = GNative.loadLibrary("glib-2.0", GlibAPI.class, new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
		}
	});
    Pointer g_main_loop_new(GMainContext context, boolean running);
    void g_main_loop_run(MainLoop loop);
    boolean g_main_loop_is_running(MainLoop loop);
    GMainContext g_main_loop_get_context(MainLoop loop);
    void g_main_loop_quit(MainLoop loop);
    void g_main_loop_ref(MainLoop ptr);
    void g_main_loop_unref(MainLoop ptr);
    void g_main_loop_unref(Pointer ptr);
    
    /*
     * GMainContext functions
     */
    
    Pointer g_main_context_new();
    void g_main_context_ref(Pointer context);
    void g_main_context_unref(Pointer context);
    Pointer g_main_context_default();
    boolean g_main_context_pending(GMainContext ctx);
    boolean g_main_context_acquire(GMainContext ctx);
    void g_main_context_release(GMainContext ctx);
    boolean g_main_context_is_owner(GMainContext ctx);
    boolean g_main_context_wait(GMainContext ctx);
    
    GSource g_idle_source_new();
    GSource g_timeout_source_new(int interval);
    GSource g_timeout_source_new_seconds(int interval);
    int g_source_attach(GSource source, GMainContext context);
    void g_source_destroy(Pointer source);
    void g_source_destroy(GSource source);
    Pointer g_source_ref(Pointer source);
    void g_source_unref(Pointer source);
    void g_source_set_callback(GSource source, GSourceFunc callback, Object data, GDestroyNotify destroy);
    boolean g_source_is_destroyed(Pointer source);
    boolean g_source_is_destroyed(GSource source);
    /*
     * GThread functions
     */
    interface GThreadFunc extends Callback {
        Pointer callback(Pointer data);
    }
    void g_thread_init(Pointer funcs);
    Pointer g_thread_create(GThreadFunc func, Pointer data, boolean joinable, PointerByReference error);
    Pointer g_thread_self();
    Pointer g_thread_join(Pointer thread);
    void g_thread_yield();
    void g_thread_set_priority(Pointer thread, int priority);
    void g_thread_exit(Pointer retval);
    

    NativeLong g_idle_add(GSourceFunc function, Pointer data);
    interface GDestroyNotify extends Callback {
        void callback(Pointer data);
    }
    
    int g_timeout_add(int interval, GSourceFunc function, Pointer data);
    int g_timeout_add_full(int priority, int interval, GSourceFunc function,
            Pointer data, GDestroyNotify notify);
    int g_timeout_add_seconds(int interval, GSourceFunc function, Pointer data);
    int g_timeout_add_seconds_full(int priority, int interval, GSourceFunc function, Pointer data, GDestroyNotify destroy);    
    void g_error_free(Pointer error);
    void g_error_free(GErrorStruct error);
    
    void g_source_remove(int id);
    void g_free(Pointer ptr);
    
    Pointer g_date_new();
    Pointer g_date_new_dmy(int day, int month, int year);
    Pointer g_date_new_julian(int julian_day);
    void g_date_free(Pointer date);
    
    public interface GLogLevelFlags {
    	public static final int RECURSION          = 1 << 0;
    	public static final int FATAL              = 1 << 1;
    	  /* GLib log levels */
    	public static final int ERROR             = 1 << 2;       /* always fatal */
    	public static final int CRITICAL          = 1 << 3;
    	public static final int WARNING           = 1 << 4;
    	public static final int MESSAGE           = 1 << 5;
    	public static final int INFO              = 1 << 6;
    	public static final int DEBUG            = 1 << 7;
    	public static final int MASK              = ~(RECURSION | FATAL);
    }
    
    public interface GLogFunc extends Callback {
    	void callback (String log_domain, int log_level, String message, Pointer data);
    }
    
    int g_log_set_handler(String log_domain, int levels, GLogFunc handler, Pointer user_data);
    void g_log_default_handler (String log_domain, int log_level, String message, Pointer unused_data);    
    Pointer g_log_set_default_handler(GLogFunc log_func, Pointer user_data);    

    public static final class GMutex extends Structure {
    	
    }
    
    /* FIXME - this only works on POSIX */
    public static final class GStaticMutex extends Structure
    {
      GMutex runtime_mutex;
      public static final class StaticMutex extends Union {
        public byte pad[] = new byte[40];
        public double dummy_double;
        Pointer dummy_pointer;
        long dummy_long;
      };
      StaticMutex static_mutex;
    };
 
    public static final class GSystemThread extends Union
    {
      public char   data[] = new char[8];
      public double dummy_double;
      public Pointer dummy_pointer;
      public long   dummy_long;
    };
    
    public static final class GStaticRecMutex extends Structure
    {
      /*< private >*/
      public GStaticMutex mutex;
      public int depth;
      public GSystemThread owner;
    }
    
	void g_list_free(GList list);
	void g_slist_free(GSList list);	
}
