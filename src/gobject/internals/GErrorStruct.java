package gobject.internals;

import com.sun.jna.Pointer;

public class GErrorStruct extends com.sun.jna.Structure {
    public int domain; /* GQuark */
    public int code;
    public String message;
    
    /** Creates a new instance of GError */
    public GErrorStruct() { clear(); }
    public GErrorStruct(Pointer ptr) {
        useMemory(ptr);
    }
    public int getCode() {
        return (Integer) readField("code");
    }
    public String getMessage() {
        return (String) readField("message");
    }
	public int getDomain() {
		return (Integer) readField("domain");
	}
}
