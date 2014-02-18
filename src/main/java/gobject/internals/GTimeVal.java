package gobject.internals;

import java.util.LinkedList;
import java.util.List;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

public class GTimeVal extends Structure {
	public NativeLong tv_sec;
	public NativeLong tv_usec;
	
	@Override
	protected List getFieldOrder() {
		final List<String> fields = new LinkedList<>();
		fields.add("tv_sec");
		fields.add("tv_usec");
		return fields;
	}
}
