package org.gnome.gir.repository;

import org.gnome.gir.gobject.RefCountedObject;

import com.sun.jna.Pointer;

public class BaseInfo extends RefCountedObject {
	protected BaseInfo(Initializer init) {
		super(init);
	}
	
	public static BaseInfo newInstanceFor(Pointer ptr) {
		int itype = GIntrospectionAPI.gi.g_base_info_get_type(ptr);
		if (itype == InfoType.UNRESOLVED.ordinal()) {
			String namespace = GIntrospectionAPI.gi.g_base_info_get_namespace(ptr);
			String name = GIntrospectionAPI.gi.g_base_info_get_name(ptr);
			throw new UnresolvedException(namespace, name);
		}
		Initializer init = new Initializer(ptr, false, false);		
		if (itype == InfoType.ENUM.ordinal())
			return new EnumInfo(init);
		if (itype == InfoType.FLAGS.ordinal())
			return new FlagsInfo(init);
		if (itype == InfoType.FIELD.ordinal())
			return new FieldInfo(init);		
		if (itype == InfoType.OBJECT.ordinal())
			return new ObjectInfo(init);
		if (itype == InfoType.FUNCTION.ordinal())
			return new FunctionInfo(init);
		if (itype == InfoType.STRUCT.ordinal())
			return new StructInfo(init);
		if (itype == InfoType.UNION.ordinal())
			return new UnionInfo(init);		
		if (itype == InfoType.BOXED.ordinal())
			return new BoxedInfo(init);
		if (itype == InfoType.INTERFACE.ordinal())
			return new InterfaceInfo(init);
		if (itype == InfoType.CALLBACK.ordinal())
			return new CallbackInfo(init);
		if (itype == InfoType.PROPERTY.ordinal())
			return new PropertyInfo(init);		
		return new BaseInfo(new Initializer(ptr));
	}

	@Override
	public void ref() {
		Repository.getNativeLibrary().g_base_info_ref(this);
	}

	@Override
	public void unref() {
		//Repository.getNativeLibrary().g_base_info_unref(this);
	}
	
	public String getName() {
		return Repository.getNativeLibrary().g_base_info_get_name(this.handle());		
	}

	@Override
	protected void disposeNativeHandle(Pointer ptr) {
		unref();
	}

	public String getNamespace() {
		return GIntrospectionAPI.gi.g_base_info_get_namespace(this.handle());
	}
	
	public String toString() {
		return "<" + getClass().getSimpleName() + " ns=" + getNamespace() + " name=" + getName() + ">";
	}
}
