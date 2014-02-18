package org.gnome.gir.repository;


public class ArgInfo extends BaseInfo {
	protected ArgInfo(Initializer init) {
		super(init);
	}	
	public Direction getDirection() {
		return Repository.getNativeLibrary().g_arg_info_get_direction(this);
	}
	public boolean isDipper() {
		return Repository.getNativeLibrary().g_arg_info_is_dipper(this);
	}	
	public boolean isReturnValue() {
		return Repository.getNativeLibrary().g_arg_info_is_return_value(this);
	}	
	public boolean isOptional() {
		return Repository.getNativeLibrary().g_arg_info_is_optional(this);
	}		
	public boolean mayBeNull() {
		return Repository.getNativeLibrary().g_arg_info_may_be_null(this);
	}	
	public Transfer getOwnershipTransfer() {
		return Repository.getNativeLibrary().g_arg_info_get_ownership_transfer(this);
	}
	
	public TypeInfo getType() {
		return Repository.getNativeLibrary().g_arg_info_get_type(this);
	}
	
	@Override
	public String toString() {
		return "<" + getClass().getSimpleName() + " ns=" + getNamespace() + " name=" + getName() + " tag=" + getType().getTag() + ">";
	}	
}
