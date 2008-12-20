package gobject.internals;
import java.util.HashMap;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 *
 */
public interface GThreadAPI extends Library {
    static GThreadAPI gthread = GNative.loadLibrary("gthread-2.0", GThreadAPI.class, new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
		}
	});
    void g_thread_init(Pointer funcs);
}
