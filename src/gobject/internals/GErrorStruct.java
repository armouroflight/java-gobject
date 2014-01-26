package gobject.internals;

import java.util.LinkedList;
import java.util.List;

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
	@Override
	protected List getFieldOrder() {
		LinkedList<Object> linkedList = new LinkedList<>();
		linkedList.add("domain");
		linkedList.add("code");
		linkedList.add("message");
		return linkedList;
	}
}
