package gobject.internals;

import gobject.runtime.GBoxed;

import com.sun.jna.Pointer;


public class GHashTable extends GBoxed {
	public GHashTable(Pointer ptr) {
		super(GBoxedAPI.gboxed.g_hash_table_get_type(), ptr);
	}
}
