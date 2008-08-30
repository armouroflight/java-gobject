package org.gnome.gir.gobject;

import org.gnome.gir.repository.Transfer;

public interface ComplexReturn<T> {
	public void dispose(Transfer trans);
	public Object convert(Class<T> klass);	
}
