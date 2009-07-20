package gobject.internals;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class GSList extends Structure implements GenericGList {
	public static class ByReference extends GSList implements Structure.ByReference {
		public ByReference() {
			super();
		}
		protected ByReference(Pointer p) {
			super(p);
		}		
	};
	
	public Pointer data;
	public GSList.ByReference next;
	
	protected GSList(Pointer p) {
		useMemory(p);
		read();
	}

	public GSList() {
		super();
	}

	public static GSList fromNative(Pointer p) {
		if (p == null)
			return null;
		return new GSList(p);
	}	
	
	public void free() {
		GlibAPI.glib.g_slist_free(this);
	}

	public Pointer getData() {
		return data;
	}

	public GenericGList getNext() {
		return next;
	}
}
