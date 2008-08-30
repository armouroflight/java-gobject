
package org.gnome.gir.repository;

public class CallableInfo extends BaseInfo {
	protected CallableInfo(Initializer init) {
		super(init);
	}	
	public TypeInfo getReturnType() {
		return Repository.getNativeLibrary().g_callable_info_get_return_type(this);
	}
	public Transfer getCallerOwns() {
		return Repository.getNativeLibrary().g_callable_info_get_caller_owns(this);
	}
	public boolean getMayReturnNull() {
		return Repository.getNativeLibrary().g_callable_info_get_may_return_null(this);
	}
	public int getNArgs() {
		return Repository.getNativeLibrary().g_callable_info_get_n_args(this);
	}
	public ArgInfo getArg(int n) {
		return Repository.getNativeLibrary().g_callable_info_get_arg(this, n);
	}
	
	public ArgInfo[] getArgs() {
		int n = getNArgs();
		ArgInfo[] ret = new ArgInfo[n];
		for (int i = 0; i < n; i++)
			ret[i] = getArg(i);
		return ret;
	}
}