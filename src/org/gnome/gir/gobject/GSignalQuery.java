/**
 * 
 */
package org.gnome.gir.gobject;

import com.sun.jna.Pointer;

public final class GSignalQuery extends com.sun.jna.Structure {
    public int signal_id;
    public String signal_name;
    public GType itype;
    public int /* GSignalFlags */ signal_flags;
    public GType return_type; /* mangled with G_SIGNAL_TYPE_STATIC_SCOPE flag */
    public int n_params;
    public Pointer param_types; /* mangled with G_SIGNAL_TYPE_STATIC_SCOPE flag */
}