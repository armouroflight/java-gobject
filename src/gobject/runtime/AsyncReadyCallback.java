package gobject.runtime;

import gobject.internals.GTypeMapper;

import com.sun.jna.Callback;
import com.sun.jna.TypeMapper;

/**
 * Type definition for a function that will be called back 
 * when an asynchronous operation within GIO has been completed.
 */
public interface AsyncReadyCallback extends Callback {
	final TypeMapper TYPE_MAPPER = GTypeMapper.getInstance();
	
	public void callback(GObject object, AsyncResult result);
}
