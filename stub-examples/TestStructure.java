

import org.gnome.gir.gobject.BoxedStructure;
import org.gnome.gir.gobject.GType;
import org.gnome.gir.gobject.GTypeMapper;

import com.sun.jna.Structure;

public class TestStructure extends BoxedStructure {
	public static class ByValue extends TestStructure implements Structure.ByValue {
		
	};
	public static class ByRereference extends TestStructure implements Structure.ByReference {};
	
	public TestStructure() {
		super(GTypeMapper.getInstance());
	}
	
	public String foo;
	public int bar;
	public TestStructure.ByRereference refed;
	
	public static GType getGType() {
		return GType.INVALID;
	}
	
	public TestStructure(String foo, int bar) {
		super(GTypeMapper.getInstance());		
		this.foo = foo;
		this.bar = bar;
	}
}
