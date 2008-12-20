package org.gnome.gir.repository;

import gobject.internals.RefCountedObject;

import com.sun.jna.Pointer;

public class BaseInfo extends RefCountedObject {
	private String cachedNamespace = null;
	private String cachedName = null;
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
		Initializer init = new Initializer(ptr, true);		
		if (itype == InfoType.ENUM.ordinal())
			return new EnumInfo(init);
		if (itype == InfoType.FLAGS.ordinal())
			return new FlagsInfo(init);
		if (itype == InfoType.FIELD.ordinal())
			return new FieldInfo(init);		
		if (itype == InfoType.OBJECT.ordinal())
			return new ObjectInfo(init);
		if (itype == InfoType.ARG.ordinal())
			return new ArgInfo(init);
		if (itype == InfoType.VALUE.ordinal())
			return new ValueInfo(init);
		if (itype == InfoType.SIGNAL.ordinal())
			return new SignalInfo(init);			
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
		if (itype == InfoType.CONSTANT.ordinal())
			return new ConstantInfo(init);				
		return new BaseInfo(init);
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
		if (cachedName == null)
			cachedName = Repository.getNativeLibrary().g_base_info_get_name(getNativeAddress());
		return cachedName;
	}

	public String getNamespace() {
		if (cachedNamespace == null)
			cachedNamespace = GIntrospectionAPI.gi.g_base_info_get_namespace(getNativeAddress());
		return cachedNamespace;
	}
	
	public String toString() {
		return "<" + getClass().getSimpleName() + " ns=" + getNamespace() + " name=" + getName() + ">";
	}
	
	public boolean isDeprecated() {
		return GIntrospectionAPI.gi.g_base_info_is_deprecated(this);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BaseInfo))
			return super.equals(o);
		BaseInfo bo = (BaseInfo) o;
		return getNamespace().equals(bo.getNamespace())
			&& getName().equals(bo.getName());
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash += 31 * getNamespace().hashCode();
		hash += 31 * getName().hashCode();
		return hash;
	}
}
