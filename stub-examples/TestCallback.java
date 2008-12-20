

import gobject.internals.GTypeMapper;

import com.sun.jna.Callback;
import com.sun.jna.TypeMapper;

public interface TestCallback extends Callback {
	public TypeMapper TYPE_MAPPER = GTypeMapper.getInstance();
	
	public boolean callback(Test foo, String bar);
}
