package gobject.internals;

import java.util.LinkedList;
import java.util.List;

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
	
	@Override
	protected List getFieldOrder() {
		final List<String> fields = new LinkedList<>();
		fields.add("data");
		fields.add("next");
		return fields;
	}
}
