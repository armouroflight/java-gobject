package gobject.internals;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

public class GString extends Structure {
	public String str;
	public NativeLong len;    
	public NativeLong allocated_len;
}
