package gobject.runtime;

import gobject.internals.GioAPI;
import gobject.runtime.GObject.GObjectProxy;


/**
 * Holds the result of an asynchronous computation.
 * 
 * @see <a href="../../gtk-doc/gio/GAsyncResult.html">native GAsyncResult</a>
 */
public interface AsyncResult extends GObjectProxy {
	public GObject getSourceObject();
	
	static final class AnonStub implements AsyncResult {
		public GObject getSourceObject() {
			return GioAPI.gio.g_async_result_get_source_object(this);			
		}
	}
}
