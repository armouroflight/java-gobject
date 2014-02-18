package gobject.runtime;

import gobject.internals.GTypeMapper;

import com.sun.jna.Callback;
import com.sun.jna.TypeMapper;

/**
 * A callback that will be invoked when an asynchronous operation
 * within Gio has been completed.
 * 
 * @see <a href="../../gtk-doc/gio/GAsyncResult.html#GAsyncReadyCallback">native GAsyncResult</a>
 */
public interface AsyncReadyCallback extends Callback {
	final TypeMapper TYPE_MAPPER = GTypeMapper.getInstance();
	
	public void callback(GObject object, AsyncResult result);
}
