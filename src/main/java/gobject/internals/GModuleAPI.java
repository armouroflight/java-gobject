package gobject.internals;
import java.util.HashMap;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 *
 */
public interface GModuleAPI extends Library {
    static GModuleAPI gmodule = GNative.loadLibrary("gmodule-2.0", GModuleAPI.class, new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
		}
	});
    Pointer g_module_open(String path, int options);
    String g_module_name(Pointer p);
}
