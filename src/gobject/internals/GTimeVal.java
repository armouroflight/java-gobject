package gobject.internals;

import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

public class GTimeVal extends Structure {
	public NativeLong tv_sec;
	public NativeLong tv_usec;
}
