package gobject.internals;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class GList extends Structure implements GenericGList {
	public static class ByReference extends GList implements Structure.ByReference {
		public ByReference() {
			super();
		}
		protected ByReference(Pointer p) {
			super(p);
		}
	};
	
	public Pointer data;
	public GList.ByReference next;
	public GList.ByReference prev;	
	
	protected GList(Pointer p) {
		useMemory(p);
		read();
	}

	public GList() {
	}

	public static GList fromNative(Pointer p) {
		if (p == null)
			return null;
		return new GList(p);
	}

	public void free() {
		GlibAPI.glib.g_list_free(this);
	}

	public Pointer getData() {
		return data;
	}

	public GenericGList getNext() {
		return next;
	}
}
