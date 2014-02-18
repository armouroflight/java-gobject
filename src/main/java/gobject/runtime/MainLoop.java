
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

package gobject.runtime;

import gobject.internals.GMainContext;
import gobject.internals.GSourceFunc;
import gobject.internals.GlibAPI;
import gobject.internals.GlibRuntime;
import gobject.internals.RefCountedObject;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;


import com.sun.jna.Pointer;

/**
 * Managed peer for a native {@code GMainLoop} structure.  There is typically one
 * main loop associated with a program, which reacts to events.
 * <p>
 * This class includes custom methods and interfaces for event scheduling, making use of
 * {@link java.util.concurrent} classes.  This methods are crucial for bridging between
 * asynchronous worker threads and the main event (UI) thread.
 * <p>
 * Scheduling code to run in the future:
 * <pre>
 * MainLoop loop = MainLoop.getDefault();
 * 
 * loop.invokeLater(5, TimeUnit.SECONDS, new Runnable() {
 *     public void run() {
 *         System.err.println("Hello 5 seconds later");    
 *     }
 * });
 * </pre>
 * <p>
 * Creating a worker thread for an expensive task, and then passing the result
 * to the main thread to update the GUI:
 * <pre>
 * MainLoop loop = MainLoop.getDefault();
 * 
 * loop.threadInvoke(new Callable<String>() {
 *     public String call() throws Exception {
 *         HttpURLConnection conn = (HttpURLConnection) new URL("http://www.example.com")).openConnection();
 *         return conn.getContentType();
 *     },
 *     new MainLoop.Handler() {
 *         public void handle(Future<String> result) {
 *             try {
 *                 contentTextArea.setText(result.get());
 *             } catch(Exception e) {
 *                 showErrorDialog(e);
 *             }
 *         }
 *     });
 * </pre> 
 * 
 * @see <a href="../../gtk-doc/html/glib/glib-The-Main-Event-Loop.html">native GMainLoop</a> 
 */
public class MainLoop extends RefCountedObject {
    private static MainLoop defaultLoop;
    private ThreadFactory asyncFactory = new ThreadFactory() {
    	private AtomicLong threadNum = new AtomicLong();
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			long num = threadNum.getAndIncrement();
			t.setName(String.format("Async task %d", num));
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
     * Handle representing pending work on this loop.  This class implements
     * {@link Future} in order to provide means of determining when the work is
     * complete and to cancel it.  The {@code get} method will always return {@code null}.
     * @author walters
     */
    private static final class AsyncFuture implements Future<Object> {
    	int srcId = 0;
    	Thread thread = null;    	
    	private boolean cancelled;
    	
    	public synchronized void setComplete() {
    		srcId = 0;
    	}
    	
    	public synchronized void threadToIdle(int srcId) {
    		thread = null;
    		this.srcId = srcId;
    	}
    	
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			if (thread != null && mayInterruptIfRunning) {
				thread.interrupt();
				return true;
			}
			if (srcId == 0)
				return false;
			cancelled = true;
			GlibAPI.glib.g_source_remove(srcId);
			srcId = 0;
			return true;
		}

		public Object get() throws InterruptedException, ExecutionException {
			return null;
		}

		public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
				TimeoutException {
			return null;
		}

		public synchronized boolean isCancelled() {
			return isDone() && cancelled;
		}

		public synchronized boolean isDone() {
			return thread == null && srcId == 0;
		}
    }
    
    /**
	 * Queues a task for later invocation on this loop.
	 * <p> This method is safe to call from any thread - the code will be executed
	 * on the thread processing this loop.   
     * 
     * @param r the task to invoke.
     * @return a handle for the task
     * @see Future
     */
    public Future<?> invokeLater(Runnable r) {
    	return invokeLater(0, TimeUnit.MILLISECONDS, r);
    }
    
    private GSourceFunc sourceFuncForRunnable(final Runnable r, final Runnable finallyHandler) {
    	GSourceFunc func = new GSourceFunc() {
			public boolean callback(Pointer data) {
				try {
					r.run();
				} catch (Exception e) {
					Thread.currentThread().getUncaughtExceptionHandler()
						.uncaughtException(Thread.currentThread(), e);
				} finally {
					if (finallyHandler != null)
						finallyHandler.run();
				}
				return false;
			}
    	};
    	return func;
    }
    
    private int rawInvokeLater(final Runnable r) {
    	GSourceFunc func = sourceFuncForRunnable(r, null);
    	GlibAPI.GDestroyNotify destroy = GlibRuntime.createDestroyNotify(func);
    	return GlibAPI.glib.g_timeout_add_full(0, 0, func, null, destroy);   	
    }
    
    /**
	 * Queues a task for later invocation on this loop after a specified time
	 * period has elapsed.
	 * <p> This method is safe to call from any thread - the code will be executed
	 * on the thread processing this loop.   
     * 
     * @param r the task to invoke.
     * @return a handle for the task
     * @see Future
     */
    public Future<?> invokeLater(int timeout, TimeUnit units, final Runnable r) {
    	final AsyncFuture handle = new AsyncFuture();
    	GSourceFunc func = sourceFuncForRunnable(r, new Runnable() {
			public void run() {
				handle.setComplete();
			}
    	});
    	GlibAPI.GDestroyNotify destroy = GlibRuntime.createDestroyNotify(func);    	
    	int id;
    	if (units.equals(TimeUnit.SECONDS)) {
    		id = GlibAPI.glib.g_timeout_add_seconds_full(0, timeout, func, null, destroy);
    	} else {
    		id = GlibAPI.glib.g_timeout_add_full(0, (int) units.toMillis(timeout), func, null, destroy);
    	}
    	handle.srcId = id;
    	return handle;
    }
    
    /**
     * A callback which is invoked when the result of a threaded invocation
     * is complete.
     *
     * @param <T> Type of object passed to queue
     */
    public interface Handler<T> {
    	public void handle(Future<T> proxy);
    }

    /**
     * Run code {@code callable} in a new {@link Thread}.  When the code completes, the
     * result will be passed to {@code handler} which is processed in the thread context of this
     * loop.  
     * <p>
     * For example, this function could be used to retrieve data over HTTP via a blocking API,
     * and then display the retrieved data inside a graphical interface.
     * 
     * @param <T> Result type of callable
     * @param callable Code to call in a new thread
     * @param handler Code to process result of thread computation
     * @return handle for this task
     */
    public <T> Future<?> threadInvoke(Callable<T> callable, Handler<T> handler) {
    	return threadInvoke(asyncFactory, callable, handler);
    }
    
    public <T> Future<?> threadInvoke(ThreadFactory factory, final Callable<T> callable, final Handler<T> handler) {
    	final AsyncFuture handle = new AsyncFuture();    	
    	Thread taskRunner = factory.newThread(new Runnable() {
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
					public T call() throws Exception {
						if (exceptionCapture != null)
							throw exceptionCapture;
						return resultCapture;
					}
				});
				proxy.run();
				/* Need to ensure that we hold a lock on handle here - conceivably
				 * the idle handler could fire in the main thread before we transition
				 * the handle, and that would result in an inconsistent state.
				 */
				synchronized (handle) {
					int idleId = rawInvokeLater(new Runnable() {
						public void run() {
							handle.srcId = 0;
							handler.handle(proxy);
						}
					});
					handle.threadToIdle(idleId);
				}
			}
    	});
    	handle.thread = taskRunner;
    	taskRunner.start();
    	return handle;
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
}
