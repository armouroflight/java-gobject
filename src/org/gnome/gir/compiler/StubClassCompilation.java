/**
 * 
 */
package org.gnome.gir.compiler;

import gobject.runtime.GType;

import java.util.HashSet;
import java.util.Set;


class StubClassCompilation extends ClassCompilation {
	Set<InnerClassCompilation> innerClasses;
	String publicName;

	
	public StubClassCompilation(String namespace, String version,
			String name) {
		super(namespace, version, name);
		this.innerClasses = new HashSet<InnerClassCompilation>();
		this.baseName = name.substring(0, 1).toUpperCase() + name.substring(1);
		this.publicName = GType.getPublicNameMapped(namespace, name);
	}
	
	public ClassCompilation newInner() {
		int size = innerClasses.size();
		InnerClassCompilation cw = new InnerClassCompilation(namespace, nsversion, baseName + "$" + size+1);
		innerClasses.add(cw);
		return cw;
	}
	
	public InnerClassCompilation newInner(String name) {
		InnerClassCompilation cw = new InnerClassCompilation(namespace, nsversion, baseName + "$" + name);
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