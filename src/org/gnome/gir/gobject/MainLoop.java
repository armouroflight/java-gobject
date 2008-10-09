
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

package org.gnome.gir.gobject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.gnome.gir.gobject.GlibAPI.GSourceFunc;

import com.sun.jna.Pointer;

/**
 * The GLib main loop.
 */
public class MainLoop extends RefCountedObject {
    private static final List<Runnable> bgTasks = new LinkedList<Runnable>();
    
    private static MainLoop defaultLoop;
    private ThreadFactory asyncFactory = new ThreadFactory() {
    	private AtomicLong threadNum = new AtomicLong();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			long num = threadNum.getAndIncrement();
			t.setName(String.format("Async task %d for mainloop %s", num, MainLoop.this));
			return t;
		}
    };
    
    /**
     * Returns the default {@code MainLoop}
     * 
     * @return default {@code MainLoop}
     */
    public synchronized static MainLoop getDefault() {
    	if (defaultLoop == null)
    		defaultLoop = new MainLoop();
    	return defaultLoop;
    }
    
    /** 
     * Creates a new instance of {@code MainLoop}
     * 
     * <p> This will create a new main loop on the default main context.
     * 
     */
    public MainLoop() {
        super(initializer(GlibAPI.glib.g_main_loop_new(null, false)));
    }
    
    /**
     * Creates a new instance of {@code MainLoop}
     * 
     * <p> This variant is used internally.
     * 
     * @param init internal initialization data.
     */
    public MainLoop(Initializer init) { 
        super(init); 
    }
    
    /**
     * Instructs a main loop to stop processing and return from {@link #run}.
     */
    public void quit() {
        invokeLater(new Runnable() {
            public void run() {
                GlibAPI.glib.g_main_loop_quit(MainLoop.this);
            }
        });
    }
    
    /**
     * Enter a loop, processing all events.
     * <p> The loop will continue processing events until {@link #quit} is 
     * called.
     */
    public void run() {
        GlibAPI.glib.g_main_loop_run(this);
    }
    
    /**
     * Returns whether this main loop is currently processing or not.
     * 
     * @return <tt>true</tt> if the main loop is currently being run.
     */
    public boolean isRunning() {
        return GlibAPI.glib.g_main_loop_is_running(this);
    }
    
    /**
     * Gets the main context for this main loop.
     * 
     * @return a main context.
     */
    public GMainContext getMainContext() {
        return GlibAPI.glib.g_main_loop_get_context(this);
    }
    
    /**
     * Runs the main loop in a background thread.
     */
    public void startInBackground() {
        bgThread = new java.lang.Thread(new Runnable() {

            public void run() {
                MainLoop.this.run();
            }
        });
        bgThread.setDaemon(true);
        bgThread.setName("gmainloop");
        bgThread.start();
    }
    
    /**
     * Invokes a task on the main loop thread.
     * <p> This method will wait until the task has completed before returning.
     * 
     * @param r the task to invoke.
     */
    public void invokeAndWait(Runnable r) {
        FutureTask<Object> task = new FutureTask<Object>(r, null);
        invokeLater(task);
        try {
            task.get();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex.getCause());
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }
    private static final GlibAPI.GSourceFunc bgCallback = new GlibAPI.GSourceFunc() {
        public boolean callback(Pointer source) {
            List<Runnable> tasks = new ArrayList<Runnable>();
            synchronized (bgTasks) {
                tasks.addAll(bgTasks);
                bgTasks.clear();
            }
            for (Runnable r : tasks) {
                r.run();
            }
            GlibAPI.glib.g_source_unref(source);
            return false;
        }
    };
    
    /**
     * Invokes a task on the main loop thread.
     * <p> This method returns immediately, without waiting for the task to 
     * complete.
     * 
     * @param r the task to invoke.
     */
    public void invokeLater(final Runnable r) {
        synchronized (bgTasks) {
            boolean empty = bgTasks.isEmpty();
            bgTasks.add(r);
            // Only trigger the callback if there were no existing elements in the list
            // otherwise it is already triggered
            if (empty) {
                GSource source = GlibAPI.glib.g_idle_source_new();
                GlibAPI.glib.g_source_set_callback(source, bgCallback, source, null);
                source.attach(getMainContext());
                source.disown(); // gets destroyed in the callback
            }
        }
    }
    
    public void invokeLater(int timeout, TimeUnit units, final Runnable r) {
    	GSourceFunc func = new GSourceFunc() {
			@Override
			public boolean callback(Pointer data) {
				try {
					r.run();
				} catch (Exception e) {
					Thread.currentThread().getUncaughtExceptionHandler()
						.uncaughtException(Thread.currentThread(), e);
				}
				return false;
			}
    	};
    	if (units.equals(TimeUnit.SECONDS)) {
    		GlibAPI.glib.g_timeout_add_seconds(timeout, func, null);
    	} else {
    		GlibAPI.glib.g_timeout_add((int) units.toMillis(timeout), func, null);
    	}
    }
    
    /**
     * Stub interface for {@code queue}.
     * @author walters
     *
     * @param <T> Type of object passed to queue
     */
    public interface Handler<T> {
    	public void handle(Future<T> proxy);
    }    

    public <T> void threadInvoke(Callable<T> callable, Handler<T> handler) {
    	threadInvoke(asyncFactory, callable, handler);
    }
    
    public <T> void threadInvoke(ThreadFactory factory, final Callable<T> callable, final Handler<T> handler) {
    	Thread taskRunner = factory.newThread(new Runnable() {
			@Override
			public void run() {
				T result = null;
				Exception e = null;
				try {
					result = callable.call();
				} catch (Exception ex) {
					e = ex;
				}
				
				final T resultCapture = result;
				final Exception exceptionCapture = e;
				final FutureTask<T> proxy = new FutureTask<T>(new Callable<T>() {
					@Override
					public T call() throws Exception {
						if (exceptionCapture != null)
							throw exceptionCapture;
						return resultCapture;
					}
				});
				proxy.run();
				invokeLater(new Runnable() {
					@Override
					public void run() {					
						handler.handle(proxy);
					}
				});
			}
    	});
    	taskRunner.start();
    }
    
    //--------------------------------------------------------------------------
    // protected methods
    //
    /**
     * Increases the reference count on the native {@code GMainLoop}
     */
    protected void ref() {
        GlibAPI.glib.g_main_loop_ref(this);
    }
    
    /**
     * Decreases the reference count on the native {@code GMainLoop}
     */
    protected void unref() {
        GlibAPI.glib.g_main_loop_unref(this);
    }
    
    /**
     * Frees the native {@code GMainLoop}
     */
    protected void disposeNativeHandle(Pointer ptr) {
        GlibAPI.glib.g_main_loop_unref(ptr);
    }
    
    //--------------------------------------------------------------------------
    // Instance variables
    //
    private Thread bgThread;
}
