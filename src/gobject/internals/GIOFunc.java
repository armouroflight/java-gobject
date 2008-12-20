package gobject.internals;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;


public interface GIOFunc extends Callback {
	public void callback(Pointer source, int condition, Pointer data);
}
