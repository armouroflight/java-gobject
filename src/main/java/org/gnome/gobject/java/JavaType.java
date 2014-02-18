package org.gnome.gobject.java;

import org.gnome.gir.repository.BaseInfo;
import org.gnome.gir.repository.TypeInfo;
import org.gnome.gir.repository.TypeTag;

public class JavaType {
	private final String typeDecl;
	private final boolean typeIsVoid;
	
	public JavaType(final String typeDecl, final boolean isVoid)
	{
		this.typeDecl = typeDecl;
		this.typeIsVoid = isVoid;
	}
	public JavaType(final TypeInfo typeInfo) throws UnknownTypeException
	{
		final BaseInfo typeInterface = typeInfo.getInterface();
		if (typeInterface != null)
		{
			// TODO needs package name
			// TODO refactor so same code for creating package/class name is used
			// here as to decide the class packages
			typeDecl = typeInterface.getName();
			typeIsVoid = false;
		}
		else
		{
			final JavaBaseType baseType = JavaBaseType.fromTypeTag(typeInfo.getTag());
			if (typeInfo.isPointer())
			{
				typeDecl = baseType.getRefType();
			}
			else
			{
				typeDecl = baseType.getType();
			}
			
			typeIsVoid = (baseType == JavaBaseType.Void);
		}
		
//		if (typeDecl == null)
//		{
//			throw new IllegalArgumentException("Return type null " + typeInfo);
//		}
	}
	
	public boolean isVoid()
	{
		return typeIsVoid;
	}
	
	public String getTypeDecl()
	{
		return typeDecl;
	}
}
