

import org.gnome.gir.gobject.GTypeMapper;

import com.sun.jna.Structure;

public class TestStructure extends Structure {
	public static class ByValue extends TestStructure implements Structure.ByValue {
		
	};
	public static class ByRereference extends TestStructure implements Structure.ByReference {};
	
	public TestStructure() {
		super(GTypeMapper.getInstance());
	}
	
	public String foo;
	public TestStructure.ByRereference refed;
}
