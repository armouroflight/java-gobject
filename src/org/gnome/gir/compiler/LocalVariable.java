/**
 * 
 */
package org.gnome.gir.compiler;

import org.objectweb.asm.Type;

final class LocalVariable {
	String name;
	int offset;
	Type type;
	public LocalVariable(String name, int offset, Type type) {
		super();
		this.name = name;
		this.offset = offset;
		this.type = type;
	}
}