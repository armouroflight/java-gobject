

import gobject.internals.GErrorStruct;
import gobject.internals.GList;
import gobject.internals.GTypeMapper;
import gobject.internals.GlibRuntime;
import gobject.internals.NativeObject;
import gobject.runtime.GErrorException;
import gobject.runtime.GObject;
import gobject.runtime.GType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gnome.gir.repository.Direction;
import org.gnome.gir.repository.Repository;
import org.gnome.gir.repository.Transfer;

import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

public class Test extends GObject implements TestIface {

	
	public static final int Foo = 10;
	public static final int Bar = 20;
	
	public static void main(int x) {
		
		switch (x) {
		case Foo:
			break;
		case Bar:
			break;
		}
	}
	
	@Deprecated
	public String getFoo() {
		return (String) get("foo");
	}
	
	public void setFoo(String arg) {
		set("foo", arg);
	}
	
	public int bar(String foo, byte[] bytes) {
		Function target = Internals.library.getFunction("gtk_write_buf");
		
		Object[] args = new Object[] { foo, bytes.length, bytes };
		Integer result = (Integer) target.invoke(Integer.class, args, Internals.invocationOptions);
		return result;
	}
	
	public float moo(long q, String z) {
		Function target = Internals.library.getFunction("gtk_foo_bar");	
		Object[] args = new Object[] { q, z };
		Float result = (Float) target.invoke(Float.class, args, Internals.invocationOptions);
		return result;
	}

	public void foo(String x, double y, Integer z) throws GErrorException {
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
		if (error.getValue() != null) {
			throw new GErrorException(new GErrorStruct(error.getValue()));
		}
		return (Test) NativeObject.Internals.objectFor(result, Test.class, false);
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
	
	public interface FooNotify extends Callback {
		public void onFooNotify(boolean foo);
	}
	
	public long connectNotify(FooNotify notify) {
		return connect("notify::foo", notify);
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
		public static final String nsversion = "2.0";
		public static final Map<Object,Object> invocationOptions = new HashMap<Object,Object>() {
			private static final long serialVersionUID = 1L;

			{	
				put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
			}
		};
		
		static {
			Repository.getDefault().requireNoFail(namespace, nsversion);
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
	
	public static GObject getStage() {
		return null;
	}
	
	public void foo(Pointer widget, Structure event) {
		Function f = Internals.library.getFunction("gtk_propagate_event");
		Object[] args = new Object[] { this, widget, event };
		f.invoke(Void.class, args, Internals.invocationOptions);		
	}
	
	public List<GObject> cow() {
		Function f = Internals.library.getFunction("gtk_cow");
		Object[] args = new Object[] { this };
		Pointer p = (Pointer) f.invoke(Pointer.class, args, Internals.invocationOptions);
		GList list = GList.fromNative(p);
		return GlibRuntime.convertListGObject(list, Transfer.EVERYTHING, GObject.class);
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
