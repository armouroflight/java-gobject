

import java.util.HashMap;
import java.util.Map;

import org.gnome.gir.gobject.GErrorException;
import org.gnome.gir.gobject.GErrorStruct;
import org.gnome.gir.gobject.GObject;
import org.gnome.gir.gobject.GType;
import org.gnome.gir.gobject.GTypeMapper;
import org.gnome.gir.repository.Direction;
import org.gnome.gir.repository.Repository;

import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

public class Test extends GObject implements TestIface {

	public String getFoo() {
		return (String) get("foo");
	}
	
	public void setFoo(String arg) {
		set("foo", arg);
	}

	public void foo(String x, Double y, Integer z) throws GErrorException {
		PointerByReference error = new PointerByReference(null);
		Function target = Internals.library.getFunction("gtk_foo_bar");
		Object[] args = new Object[] { x, y, z };
		Boolean result = (Boolean) target.invoke(Boolean.class, args, Internals.invocationOptions);
		if (!result) {
			throw new GErrorException(new GErrorStruct(error.getValue()));
		}		
	}
	
	public Test baz(Double x, Integer z) throws GErrorException {
		PointerByReference error = new PointerByReference(null);
		Function target = Internals.library.getFunction("glib_baz");
		Object[] args = new Object[] { x, z };
		Pointer result = (Pointer) target.invoke(Pointer.class, args, Internals.invocationOptions); 
		if (error.getPointer() == null) {
			throw new GErrorException(new GErrorStruct(error.getValue()));
		}		
		return (Test) objectFor(result, Test.class);
	}	
	
	public static Test newWithFoo(String blah) {
		return new Test(initializer((Pointer) Internals.library.getFunction("gtk_test_new_with_foo").invoke(Pointer.class, new Object[] { blah }, Internals.invocationOptions)));
	}
	
	public static GType getGType() {
		return (GType) Internals.library.getFunction("gtk_test_get_type").invoke(GType.class, new Object[] {}, Internals.invocationOptions);
	}
	
	public Test(Direction dir, Integer foo, String blah) { 
		super(initializer((Pointer) Internals.library.getFunction("gtk_test2_new")
				.invoke(Pointer.class, new Object[] { dir, foo, blah }, Internals.invocationOptions)));
	}
	
	public interface Clicked extends Callback {
		public static final String METHOD_NAME = "onClicked";
		public void onClicked(Test t);
	}
	
	public long connect(Clicked c) {
		return connect("clicked", c);
	}
	
	public Test() {
		super(getGType(), (Object[])null);
	}
	
	public Test(Object[] args) {
		super(getGType(), args);
	}
	
	public Test(Map<String,Object> args) {
		super(getGType(), args);
	}

    protected Test(Initializer init) { 
    	super(init);
    }
    
    protected Test(GType gtype, Object[] args) {
    	super(gtype, args);
    }

	public static final class Internals {
		public static final NativeLibrary library = NativeLibrary.getInstance("gtk-2.0");
		public static final Repository repo = Repository.getDefault();
		public static final String namespace = "Gtk";
		public static final Map<Object,Object> invocationOptions = new HashMap<Object,Object>() {
			private static final long serialVersionUID = 1L;

			{	
				put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
			}
		};
		
		static {
			Repository.getDefault().requireNoFail(namespace);
		}
	};

	public static final void init() {
		Function f = Internals.library.getFunction("gtk_init");
		Object[] args = new Object[] {}; 
		f.invoke(Void.class, args, Internals.invocationOptions);
	}
	
	public static final Boolean eventsPending() {
		Function f = Internals.library.getFunction("gtk_events_pending");
		Object[] args = new Object[] {};
		return (Boolean) f.invoke(Boolean.class, args, Internals.invocationOptions);
	}
	
	public static final void propagateEvent(Pointer widget, Structure event) {
		Function f = Internals.library.getFunction("gtk_propagate_event");
		Object[] args = new Object[] { widget, event };
		f.invoke(Void.class, args, Internals.invocationOptions);
	}	
	
	public void foo(Pointer widget, Structure event) {
		Function f = Internals.library.getFunction("gtk_propagate_event");
		Object[] args = new Object[] { this, widget, event };
		f.invoke(Void.class, args, Internals.invocationOptions);		
	}
	
	public void ifaceFoo(String blah) {
		Function f = Internals.library.getFunction("gtk_propagate_event");
		Object[] args = new Object[] { this, blah };
		f.invoke(Void.class, args, Internals.invocationOptions);
	}
	
	private static Object[] afoo(boolean foo, Object...args) {
		return args;
	}
	
	private static int bar(String baz, Object...args) {
		return args.length;
	}
	
	public static final void main(String... args) {
		Object[] o = new Object[] { "hello", "world"};
		System.out.printf("%d", bar("Moo", afoo(true, o)));
	}
}