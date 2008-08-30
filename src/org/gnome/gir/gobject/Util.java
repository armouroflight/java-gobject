package org.gnome.gir.gobject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Util {
	public static final <T> List<T> toList(Iterable<T> iterable) {
		List<T> ret = new ArrayList<T>();
		for (T it : iterable)
			ret.add(it);
		return ret;
	}
	
	public static final <T> Set<T> toSet(Iterable<T> iterable) {
		Set<T> ret = new HashSet<T>();
		for (T it : iterable)
			ret.add(it);
		return ret;
	}	
}
