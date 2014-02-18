package org.gnome.gobject.java;

import org.gnome.gir.repository.TypeTag;

public class UnknownTypeException extends Exception{
	private final TypeTag type;
	public UnknownTypeException(TypeTag type)
	{
		super("Unknown type " + type.name());
		this.type = type;
	}
	public TypeTag getType() {
		return type;
	}
}
