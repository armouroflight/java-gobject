
package org.gnome.gir.repository;

public class FunctionInfo extends CallableInfo {
	protected FunctionInfo(Initializer init) {
		super(init);
	}

	public String getSymbol() {
		return Repository.getNativeLibrary().g_function_info_get_symbol(this);
	}
	
	public int getFlags() {
		return Repository.getNativeLibrary().g_function_info_get_flags(this);
	}
}