package org.gnome.gir.gobject;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;


public final class GObjectGlobals {
	private GObjectGlobals() {}
	
	private static Function backtrace = null;
	private static Function backtraceSymbolsFd = null;	
	
	private static final void glibcBacktrace() {
		if (backtrace == null)
			return;
		Pointer[] backtraceData = new Pointer[100];
		long size = backtrace.invokeLong(new Object[] { backtraceData, backtraceData.length });
		backtraceSymbolsFd.invoke(new Object[] { backtraceData, size, Integer.valueOf(2) });
	}
	
	private static boolean initialized = false;
	
	public static synchronized void init() {
		if (initialized)
			return;
		final GlibAPI.GLogFunc handler = new GlibAPI.GLogFunc() {
			@Override
			public void callback(String log_domain, int log_level,
					String message, Pointer data) {
				if (((log_level & GlibAPI.GLogLevelFlags.CRITICAL) > 0) || 
						((log_level & GlibAPI.GLogLevelFlags.ERROR) > 0) ||
						((log_level & GlibAPI.GLogLevelFlags.WARNING) > 0)) {
					String msg = "GLib Failure: " + log_domain + " " + message;
					System.err.println(msg);
					System.err.println("--- glibc backtrace follows ---");
					glibcBacktrace();
					System.err.println("--- glibc backtrace ends ---");					
					throw new RuntimeException(msg);
				}
			}
		};
		try {
			NativeLibrary libc =  NativeLibrary.getInstance("c");
			backtrace = libc.getFunction("backtrace");
			backtraceSymbolsFd = libc.getFunction("backtrace_symbols_fd");			
		} catch (Throwable t) {
		}
		
		GlibAPI.glib.g_log_set_default_handler(handler, null);
		GThreadAPI.gthread.g_thread_init(null);
		GObjectAPI.gobj.g_type_init();
		initialized = true;
	}
}
