package org.gnome.gir.repository;

import java.util.List;

import org.gnome.gir.gobject.GErrorException;
import org.gnome.gir.gobject.GErrorStruct;
import org.gnome.gir.gobject.GObjectGlobals;
import org.gnome.gir.gobject.ZeroTerminatedArray;

import com.sun.jna.NativeLong;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.PointerByReference;

public class Repository extends PointerType {

	public BaseInfo findByName(String namespace, String name) {
		return GIntrospectionAPI.gi.g_irepository_find_by_name(this, namespace, name);
	}
	
	public BaseInfo findByGType(NativeLong g_type) {
		return GIntrospectionAPI.gi.g_irepository_find_by_gtype(this, g_type);
	}
		
	public void require(String namespace) throws GErrorException {
		PointerByReference error = new PointerByReference(null);
		if (!GIntrospectionAPI.gi.g_irepository_require(this, namespace, 0, error)) {
			throw new GErrorException(new GErrorStruct(error.getValue()));
		}
	}
	
	public void requireNoFail(String namespace) {
		try {
			require(namespace);
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
	
	public String getTypelibPath(String namespace) {
		return GIntrospectionAPI.gi.g_irepository_get_typelib_path(this, namespace);
	}	

	public List<String> getNamespaces() {
		ZeroTerminatedArray<String> z = GIntrospectionAPI.gi.g_irepository_get_namespaces(this);
		List<String> ret = z.convert(String.class);
		z.dispose(Transfer.EVERYTHING);
		return ret;
	}
	
	public boolean isRegistered(String targetNamespace) {
		return GIntrospectionAPI.gi.g_irepository_is_registered(this, targetNamespace);
	}	

	static GIntrospectionAPI getNativeLibrary() {
		return GIntrospectionAPI.gi;
	}
	
	public static synchronized Repository getDefault() {
		GObjectGlobals.init();
		return getNativeLibrary().g_irepository_get_default();
	}
}
