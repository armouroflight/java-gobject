package org.gnome.gobject.java;

import java.io.File;

import java.util.Date;

import org.gnome.gir.repository.TypeTag;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

public enum JavaBaseType {
	Void("void", Pointer.class.getName()), 
	Long("long", LongByReference.class.getName()), 
	List("List"), 
	Map("Map"),
	Boolean("boolean"),
	Byte("byte", ByteByReference.class.getName()),
	Short("short", ShortByReference.class.getName()),
	Integer("int", IntByReference.class.getName()),
	Float("float", FloatByReference.class.getName()),
	Double("double", DoubleByReference.class.getName()),
	String("String", PointerByReference.class.getName()),
	File(File.class.getName(), PointerByReference.class.getName()),
	Date(Date.class.getName());
	
	final String type;
	final String refType;
	/**
	 * Type with no reference type
	 * @param type
	 */
	JavaBaseType(String type)
	{
		this(type, null);
	}
	JavaBaseType(String type, String refType)
	{
		this.type = type;
		this.refType = refType;
	}
	
	/**
	 * Get the declaration for the type
	 * @return The declaration of type
	 */
    public String getType() {
		return type;
	}
    /**
     * Get the declaration for a reference type
     * @return The declaration of the type
     */
	public String getRefType() {
		return refType;
	}
	public static JavaBaseType fromTypeTag(TypeTag tag) throws UnknownTypeException {
		if (tag == TypeTag.VOID)
            return Void;
        if (tag == TypeTag.BOOLEAN)
            return Boolean;
        if (tag == TypeTag.INT8 || tag == TypeTag.UINT8)
            return Byte;
        if (tag == TypeTag.INT16 || tag == TypeTag.UINT16
        		|| tag == TypeTag.USHORT)
            return Short;
        if (tag == TypeTag.INT32 || tag == TypeTag.UINT32 ||
                tag == TypeTag.INT || tag == TypeTag.UINT)
            return Integer;
        if (tag == TypeTag.INT64 || tag == TypeTag.UINT64
        		|| tag == TypeTag.SSIZE || tag == TypeTag.SIZE)
            return Long;
        if (tag == TypeTag.FLOAT)
            return Float;
        if (tag == TypeTag.DOUBLE)
            return Double;
        if (tag == TypeTag.UTF8)
            return String;
        if (tag == TypeTag.FILENAME)
            return File;
        if (tag == TypeTag.LONG || tag == TypeTag.ULONG)
        {
        	if (Native.LONG_SIZE == 8)
        		return Long;
        	else
        		return Integer;
        }
        if (tag == TypeTag.TIMET)
        {
        	return Date;
        }
        throw new UnknownTypeException(tag);
    }
}
