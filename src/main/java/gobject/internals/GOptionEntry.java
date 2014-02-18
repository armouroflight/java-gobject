package gobject.internals;

import java.util.LinkedList;
import java.util.List;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class GOptionEntry extends Structure {
	public String long_name;
	public byte short_name;
	public int flags;

	public int   arg;
	public Pointer     arg_data;
	  
	public String description;
	public String arg_description;
	
	@Override
	protected List getFieldOrder() {
		final List<String> fields = new LinkedList<>();
		fields.add("long_name");
		fields.add("short_name");
		fields.add("flags");
		fields.add("arg");
		fields.add("arg_data");
		fields.add("description");
		fields.add("arg_description");
		return fields;
	}
}
