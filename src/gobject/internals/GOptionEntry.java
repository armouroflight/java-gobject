package gobject.internals;

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
}
