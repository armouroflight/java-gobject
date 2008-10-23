/**
 * 
 */
package org.gnome.gir.compiler;

import java.util.HashSet;
import java.util.Set;

import org.gnome.gir.gobject.GType;

class StubClassCompilation extends ClassCompilation {
	Set<InnerClassCompilation> innerClasses;
	String publicName;

	
	public StubClassCompilation(String namespace,
			String name) {
		super(namespace, name);
		this.innerClasses = new HashSet<InnerClassCompilation>();
		this.baseName = name.substring(0, 1).toUpperCase() + name.substring(1);
		this.publicName = GType.getPublicNameMapped(namespace, name);
	}
	
	public ClassCompilation newInner() {
		int size = innerClasses.size();
		InnerClassCompilation cw = new InnerClassCompilation(namespace, baseName + "$" + size+1);
		innerClasses.add(cw);
		return cw;
	}
	
	public InnerClassCompilation newInner(String name) {
		InnerClassCompilation cw = new InnerClassCompilation(namespace, baseName + "$" + name);
		innerClasses.add(cw);
		return cw;
	}
	
	public void close() {
		if (closed)
			return;
		for (InnerClassCompilation inner : innerClasses)
			inner.close();
		super.close();
		writer.visitEnd();
		closed = true;
	}
}