/**
 * 
 */
package gobject.internals;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

public interface GSourceFunc extends Callback {
    boolean callback(Pointer data);
}