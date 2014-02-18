package gobject.internals;

import java.util.LinkedList;
import java.util.List;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

public class GString extends Structure {
	public String str;
	public NativeLong len;    
	public NativeLong allocated_len;
	
	@Override
	protected List getFieldOrder() {
		final List<String> fields = new LinkedList<>();
		fields.add("str");
		fields.add("len");    
		fields.add("allocated_len");
		return fields;
	}
}
