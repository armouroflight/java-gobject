package org.gnome.gir.gobject;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class GList extends Structure {
	public static class ByReference extends GList implements Structure.ByReference {};
	
	public Pointer data;
	public GList.ByReference next;
	public GList.ByReference prev;	
	
	public List<Pointer> copy() {
		List<Pointer> ret = new ArrayList<Pointer>();
		GList list = this;
		while (list.next != null) {
			ret.add(list.data);
			list = list.next;
		}
		return ret;
	}
	
	public void free() {
		GlibAPI.glib.g_list_free(this);
	}
}
