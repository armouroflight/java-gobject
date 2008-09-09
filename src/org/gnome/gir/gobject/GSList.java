package org.gnome.gir.gobject;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class GSList extends Structure {
	public static class ByReference extends GSList implements Structure.ByReference {};
	
	public Pointer data;
	public GSList.ByReference next;
	
	public List<Pointer> copy() {
		List<Pointer> ret = new ArrayList<Pointer>();
		GSList list = this;
		while (list.next != null) {
			ret.add(list.data);
			list = list.next;
		}
		return ret;
	}
	
	public void free() {
		GlibAPI.glib.g_slist_free(this);
	}	
}
