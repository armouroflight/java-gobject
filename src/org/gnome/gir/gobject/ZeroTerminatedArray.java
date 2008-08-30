package org.gnome.gir.gobject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.gnome.gir.repository.Transfer;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class ZeroTerminatedArray<T> extends PointerType implements ComplexReturn<T> {
	public Iterable<Pointer> iterComponents() {
		return new Iterable<Pointer>() {
			@Override
			public Iterator<Pointer> iterator() {
				return new Iterator<Pointer>() {
					private int idx = 0;					

					private Pointer getCurrent() {
						return ZeroTerminatedArray.this.getPointer().getPointer(idx * Pointer.SIZE);
					}
					
					@Override					
					public boolean hasNext() {
						return getCurrent() != null; 
					}

					@Override
					public Pointer next() {
						Pointer ret = getCurrent();
						idx++;
						return ret;
					}

					@Override
					public void remove() {
						throw new RuntimeException();
					}							
				};
			}
		};
	};
	
	public void dispose(Transfer trans) {
		if (trans == Transfer.EVERYTHING) {
			for (Pointer p : iterComponents()){
				GlibAPI.glib.g_free(p);
			}	
		}
		if (trans == Transfer.EVERYTHING || trans == Transfer.CONTAINER)
			GlibAPI.glib.g_free(this.getPointer());
	}
	
	@SuppressWarnings("unchecked")
	public List<T> convert(Class<T> klass) {
		List<T> ret = new ArrayList<T>();
		for (Pointer p : iterComponents()) {
			if (klass.equals(String.class))
				ret.add((T) p.getString(0));
			else
				throw new UnsupportedOperationException();
		}
		return ret;
	}
}
