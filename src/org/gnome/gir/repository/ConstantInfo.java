package org.gnome.gir.repository;


public class ConstantInfo extends BaseInfo {

	protected ConstantInfo(Initializer init) {
		super(init);
	}

	public TypeInfo getType() {
		return Repository.getNativeLibrary().g_constant_info_get_type(this);
	}
	
	public Object getValue() {
		TypeTag tag = getType().getTag();
		Argument arg = new Argument();
		Repository.getNativeLibrary().g_constant_info_get_value(this, arg);
		return arg.toJava(tag);
	}
}
