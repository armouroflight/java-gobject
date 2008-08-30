/**
 * 
 */
package org.gnome.gir.repository;

public interface FunctionInfoFlags {
	public static final int IS_METHOD = (1 << 0);
	public static final int IS_CONSTRUCTOR = (1 << 1);
	public static final int IS_SETTER = (1 << 2);
	public static final int IS_GETTER = (1 << 3);
	public static final int WRAPS_VFUNC = (1 << 4);
}