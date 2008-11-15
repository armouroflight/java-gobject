package org.gnome.gir.repository;

import org.gnome.gir.gobject.GErrorException;
import org.gnome.gir.gobject.GErrorStruct;
import org.gnome.gir.gobject.GObjectGlobals;

import com.sun.jna.NativeLong;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.PointerByReference;

public class Repository extends PointerType {

	private boolean disableRequires = false;
	
	/* Needed for the compiler to be able to verify classes without loading typelibs 
	 * which could potentially conflict. */
	public void disableRequires() {
		disableRequires = true;
	}
	
	public void prependSearchPath(String path) {
		GIntrospectionAPI.gi.g_irepository_prepend_search_path(path);
	}
	
	public BaseInfo findByName(String namespace, String name) {
		return GIntrospectionAPI.gi.g_irepository_find_by_name(this, namespace, name);
	}
	
	public BaseInfo findByGType(NativeLong g_type) {
		return GIntrospectionAPI.gi.g_irepository_find_by_gtype(this, g_type);
	}
		
	public void require(String namespace, String version) throws GErrorException {
		if (disableRequires)
			return;
		PointerByReference error = new PointerByReference(null);
		if (GIntrospectionAPI.gi.g_irepository_require(this, namespace, version, 0, error) == null) {
			throw new GErrorException(new GErrorStruct(error.getValue()));
		}
	}
	
	public void requireNoFail(String namespace, String version) {
		try {
			require(namespace, version);
		} catch (GErrorException e) {
			throw new RuntimeException(e);
		}
	}
	
	public BaseInfo[] getInfos(String namespace) {
		int nInfos = GIntrospectionAPI.gi.g_irepository_get_n_infos(this, namespace);
		BaseInfo[] ret = new BaseInfo[nInfos];
		for (int i = 0; i < nInfos; i++) {
			ret[i] = GIntrospectionAPI.gi.g_irepository_get_info(this, namespace, i);
		}
		return ret;
	}
	
	public String getSharedLibrary(String namespace) {
		return GIntrospectionAPI.gi.g_irepository_get_shared_library(this, namespace);
	}
	
	public String[] getDependencies(String namespace) {
		return GIntrospectionAPI.gi.g_irepository_get_dependencies(this, namespace);
	}
	
	public String getTypelibPath(String namespace) {
		return GIntrospectionAPI.gi.g_irepository_get_typelib_path(this, namespace);
	}
	
	public boolean isRegistered(String targetNamespace) {
		return GIntrospectionAPI.gi.g_irepository_is_registered(this, targetNamespace);
	}
	
	public String getNamespaceVersion(String namespace) {
		return GIntrospectionAPI.gi.g_irepository_get_version(this, namespace);
	}

	static GIntrospectionAPI getNativeLibrary() {
		return GIntrospectionAPI.gi;
	}
	
	public static synchronized Repository getDefault() {
		GObjectGlobals.init();
		return getNativeLibrary().g_irepository_get_default();
	}
	
	public void unloadAll() {
		GIntrospectionAPI.gi.g_irepository_unload_all(this);
	}
}
