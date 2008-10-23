package org.gnome.gir.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gnome.gir.gobject.GList;
import org.gnome.gir.gobject.GSList;
import org.gnome.gir.gobject.GType;
import org.gnome.gir.gobject.UnmappedPointer;
import org.gnome.gir.repository.ArgInfo;
import org.gnome.gir.repository.BaseInfo;
import org.gnome.gir.repository.BoxedInfo;
import org.gnome.gir.repository.CallableInfo;
import org.gnome.gir.repository.Direction;
import org.gnome.gir.repository.FieldInfo;
import org.gnome.gir.repository.FlagsInfo;
import org.gnome.gir.repository.InterfaceInfo;
import org.gnome.gir.repository.ObjectInfo;
import org.gnome.gir.repository.StructInfo;
import org.gnome.gir.repository.TypeInfo;
import org.gnome.gir.repository.TypeTag;
import org.gnome.gir.repository.UnionInfo;
import org.objectweb.asm.Type;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

public class TypeMap {

	public static Type toJava(TypeTag tag) {
		if (tag == TypeTag.LONG || tag == TypeTag.ULONG ||
				tag == TypeTag.SSIZE || tag == TypeTag.SIZE)
			return Type.getType(Long.class);
		if (tag == TypeTag.ARRAY)
			return Type.getType(List.class);
		if (tag == TypeTag.GLIST)
			return Type.getType(GList.class);
		if (tag == TypeTag.GSLIST)
			return Type.getType(GSList.class);
		if (tag == TypeTag.GHASH)
			return Type.getType(Map.class);
		return toTypeBase(tag);		
	}

	public static Type toJavaRef(TypeTag tag) {
		Type t = toJava(tag);
		if (t == null)
			return null;
		if (t.equals(Type.INT_TYPE))
			return Type.getType(IntByReference.class);
		if (t.equals(Type.LONG_TYPE))
			return Type.getType(LongByReference.class);
		if (t.equals(Type.BOOLEAN_TYPE))
			return Type.getType(IntByReference.class);
		if (t.equals(Type.BYTE_TYPE))
			return Type.getType(ByteByReference.class);
		if (t.equals(Type.SHORT_TYPE))
			return Type.getType(ShortByReference.class);
		if (t.equals(Type.FLOAT_TYPE))
			return Type.getType(FloatByReference.class);
		if (t.equals(Type.DOUBLE_TYPE))
			return Type.getType(DoubleByReference.class);
		if (t.equals(Type.getType(String.class)) || t.equals(Type.getType(File.class)))
			return Type.getType(PointerByReference.class);
		return Type.getType(UnmappedPointer.class);
	}

	static Type toTypeBase(TypeTag tag) {
		if (tag == TypeTag.VOID)
			return Type.VOID_TYPE;
		if (tag == TypeTag.BOOLEAN)
			return Type.BOOLEAN_TYPE;
		if (tag == TypeTag.INT8 || tag == TypeTag.UINT8)
			return Type.BYTE_TYPE;
		if (tag == TypeTag.INT16 || tag == TypeTag.UINT16)
			return Type.SHORT_TYPE;
		if (tag == TypeTag.INT32 || tag == TypeTag.UINT32 ||
				tag == TypeTag.INT || tag == TypeTag.UINT)
			return Type.INT_TYPE;
		if (tag == TypeTag.INT64 || tag == TypeTag.UINT64
				|| tag == TypeTag.SIZE || tag == TypeTag.SSIZE)
			return Type.LONG_TYPE;
		if (tag == TypeTag.LONG) {
			if (Native.LONG_SIZE == 8)
				return Type.LONG_TYPE;
			else
				return Type.INT_TYPE;
		}
		if (tag == TypeTag.FLOAT)
			return Type.FLOAT_TYPE;
		if (tag == TypeTag.DOUBLE)
			return Type.DOUBLE_TYPE;
		if (tag == TypeTag.TIMET)
			return Type.getType(Date.class);
		if (tag == TypeTag.GTYPE)
			return Type.getType(GType.class);		
		if (tag == TypeTag.UTF8)
			return Type.getType(String.class);
		if (tag == TypeTag.FILENAME)
			return Type.getType(File.class);		
		return null;
	}

	static Class<?> getPrimitiveBox(Type type) {
		if (type.equals(Type.BOOLEAN_TYPE))
			return Boolean.class;
		if (type.equals(Type.BYTE_TYPE))
			return Byte.class;
		if (type.equals(Type.CHAR_TYPE))
			return Character.class;
		if (type.equals(Type.SHORT_TYPE))
			return Short.class;
		if (type.equals(Type.INT_TYPE))
			return Integer.class;
		if (type.equals(Type.LONG_TYPE))
			return Long.class;
		if (type.equals(Type.FLOAT_TYPE))
			return Float.class;
		if (type.equals(Type.DOUBLE_TYPE))
			return Double.class;
		return null;
	}

	static Type getCallableReturn(CallableInfo callable) {
		TypeInfo info = callable.getReturnType();
		if (info.getTag().equals(TypeTag.INTERFACE)) {
			return TypeMap.typeFromInfo(info);
		}
		return toJava(info.getTag());
	}

	public static Type toJava(ArgInfo arg) {
		if (arg.getDirection() == Direction.IN) {
			return TypeMap.toJava(arg.getType());
		} else {
			return toJavaRef(arg.getType().getTag());
		}
	}

	public static Type toJava(FieldInfo arg) {
		TypeInfo type = arg.getType();
		if (type.getTag().equals(TypeTag.INTERFACE)) {
			BaseInfo iface = arg.getType().getInterface();
			/* Special case structure/union members; we need to use the
			 * $ByReference tag if the member is actually a pointer.
			 */
			if (iface instanceof StructInfo || iface instanceof UnionInfo) {
				boolean hasFields;
				if (iface instanceof StructInfo)
					hasFields = ((StructInfo) iface).getFields().length > 0;
				else
					hasFields = ((UnionInfo) iface).getFields().length > 0;
				if (!hasFields)
					return Type.getType(Pointer.class);
				String internalName = CodeFactory.getInternalNameMapped(iface);
				if (type.isPointer() && internalName.startsWith(GType.dynamicNamespace))
					internalName += "$ByReference";
				return Type.getObjectType(internalName);
			} else if (iface instanceof InterfaceInfo || iface instanceof ObjectInfo ||
					iface instanceof BoxedInfo) {
				/* Interfaces/Objects/Boxed are always Pointer for now
				 */
				return Type.getType(Pointer.class);
			}
		}
		Type result = TypeMap.toJava(type);
		if (result == null) {
			CodeFactory.logger.warning(String.format("Unhandled field type %s (type %s)", result, type));
		}
		return result;
	}

	public static Type toJava(TypeInfo type) {	
		//Transfer transfer = arg.getOwnershipTransfer();
		TypeTag tag = type.getTag();
		
		if (tag.equals(TypeTag.VOID)) {
			// FIXME - for now we change random Voids into Pointer, but this seems to
			// be a G-I bug
			return Type.getType(Pointer.class);		
		} else if (tag.equals(TypeTag.INTERFACE)) {
			return TypeMap.typeFromInfo(type.getInterface());
		} else if (tag.equals(TypeTag.ARRAY)) {
			return TypeMap.toJavaArray(type.getParamType(0));
		} else if (!type.isPointer() || (tag.equals(TypeTag.UTF8) || tag.equals(TypeTag.FILENAME))) {
			return toTypeBase(tag);
		} else if (type.isPointer()) {
			return toJavaRef(tag);
		} else {
			return toTypeBase(tag);
		}	
	}

	public static Type toJavaArray(TypeInfo containedType) {
		Type result = toJava(containedType);
		if (result == null)
			return null;
		String descriptor = result.getDescriptor();
		return Type.getType("[" + descriptor);
	}

	static Type typeFromInfo(BaseInfo info) {
		/* Unfortunately, flags are best mapped as plain Integer  for now */
		if (info instanceof FlagsInfo)
			return Type.getObjectType("java/lang/Integer");
		String internalName = GType.getInternalNameMapped(info.getNamespace(), info.getName());
		if (internalName != null)
			return Type.getObjectType(internalName);
		return null;
	}

	static Type typeFromInfo(TypeInfo info) {
		BaseInfo base = info.getInterface();
		return typeFromInfo(base);
	}

	public static List<InterfaceInfo> getUniqueInterfaces(List<InterfaceInfo> ifaces) {
		boolean hit = true;
		ifaces = new ArrayList<InterfaceInfo>(ifaces);
		while (hit) {
			hit = false;
			for (Iterator<InterfaceInfo> it = ifaces.iterator(); it.hasNext();) {
				InterfaceInfo possible = it.next();
				for (InterfaceInfo iface : ifaces) {
					if (iface == possible)
						continue;
					if (TypeMap.isAssignableFrom(possible, iface)) {
						it.remove();
						hit = true;
						break;
					}
				}
			}
		}
		return ifaces;
	}

	public static List<InterfaceInfo> getUniqueInterfaces(ObjectInfo obj) {
		List<InterfaceInfo> ifaces = Arrays.asList(obj.getInterfaces());
		return getUniqueInterfaces(ifaces);
	}

	public static boolean introspectionImplements(ObjectInfo obj, InterfaceInfo iface) {
		while (!(obj.getNamespace().equals("GObject") && obj.getName().equals("Object"))) {
			List<InterfaceInfo> ifaces = Arrays.asList(obj.getInterfaces());
			for (InterfaceInfo possible : ifaces) {
				if (TypeMap.isAssignableFrom(iface, possible))
					return true;
			}
			obj = obj.getParent();
		}
		return false;
	}

	static boolean isAssignableFrom(InterfaceInfo lhs, InterfaceInfo rhs) {
		if (lhs.equals(rhs))
			return true;
		List<InterfaceInfo> prereqs = Arrays.asList(lhs.getPrerequisites());
		for (InterfaceInfo iface : prereqs) {
			if (isAssignableFrom(iface, rhs))
				return true;
		}
		return false;
	}

}
