package org.gnome.gobject.java;

import org.gnome.gir.repository.ArgInfo;

public class JavaArg {
	private final String typeDecl;
	private final String name;
	public JavaArg(ArgInfo argInfo) throws UnknownTypeException {
		this(new JavaType(argInfo.getType()).getTypeDecl(), argInfo.getName());
		
	}
	
	public JavaArg(final String type, final String name) {
		this.typeDecl = type;
		this.name = name;
	}
	
	public String getType()
	{
		return typeDecl;
	}
	
	public String getName()
	{
		return name;
	}
}
