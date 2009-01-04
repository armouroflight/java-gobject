package gobject.runtime;

import gobject.internals.GioAPI;
import gobject.runtime.GObject.GObjectProxy;


/**
 * Provides a base class for implementing asynchronous function results. 
 */
public interface AsyncResult extends GObjectProxy {
	public GObject getSourceObject();
	
	public static final class AnonStub implements AsyncResult {
		public GObject getSourceObject() {
			return GioAPI.gio.g_async_result_get_source_object(this);			
		}
	}
}
