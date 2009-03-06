
/* 
 * Copyright (c) 2008 Colin Walters <walters@verbum.org>
 * 
 * This file is part of java-gobject-introspection.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the 
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, 
 * Boston, MA  02111-1307  USA.
 *
 */
/**
 * 
 */
package org.gnome.gir.repository;

import gobject.internals.GTypeMapper;
import gobject.runtime.GType;

import java.util.HashMap;


import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

interface GIntrospectionAPI extends Library {
	public static final GIntrospectionAPI gi = (GIntrospectionAPI) Native.loadLibrary("girepository-1.0", GIntrospectionAPI.class, new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put(Library.OPTION_TYPE_MAPPER, new GTypeMapper());
		}
	});	
	
	GType g_irepository_get_type();
	Pointer g_irepository_get_default();
	void g_irepository_prepend_search_path(String path);
	int g_irepository_get_n_infos(Repository repo, String namespace);
	boolean g_irepository_is_registered (Repository repository, String namespace);
	Pointer g_irepository_require(Repository repo, String namespace, String version, int flags, PointerByReference error);
	BaseInfo g_irepository_find_by_name(Repository repo, String namespace, String name);
	BaseInfo g_irepository_find_by_gtype(Repository repository, NativeLong g_type);	
	String[] g_irepository_get_namespaces(Repository repo);
	String[] g_irepository_get_dependencies(Repository repo, String namespace);
	String g_irepository_get_version(Repository repo, String namespace);
	BaseInfo g_irepository_get_info(Repository repo, String namespace, int idx);
	String g_irepository_get_shared_library(Repository repository, String namespace);
	String g_irepository_get_typelib_path(Repository repository, String namespace);
	
	void g_base_info_ref(BaseInfo info);
	void g_base_info_unref(BaseInfo info);		
	int g_base_info_get_type(Pointer info);
	boolean g_base_info_is_deprecated(BaseInfo info);	
	String g_base_info_get_name(Pointer info);	
	String g_base_info_get_namespace(Pointer info);	
	String g_base_info_get_annotation(BaseInfo info, String anno);
	BaseInfo g_base_info_get_container(BaseInfo info);
	
	String g_function_info_get_symbol(FunctionInfo info);
	int g_function_info_get_flags(FunctionInfo info);
	PropertyInfo g_function_info_get_property_info(FunctionInfo info);
	VFuncInfo g_function_info_get_vfunc(FunctionInfo info);
	
	boolean g_function_info_invoke(FunctionInfo info, Argument[] in, int n_in_args,
			Argument[] out, int n_out_args, Argument retval,
			PointerByReference error);
	
	TypeInfo g_callable_info_get_return_type(CallableInfo info);
	Transfer g_callable_info_get_caller_owns(CallableInfo info);
	boolean g_callable_info_get_may_return_null(CallableInfo info);
	int g_callable_info_get_n_args(CallableInfo info);
	ArgInfo g_callable_info_get_arg(CallableInfo info, int n);
	
	Direction           g_arg_info_get_direction          (ArgInfo info);
	boolean              g_arg_info_is_dipper              (ArgInfo info);
	boolean              g_arg_info_is_return_value        (ArgInfo info);
	boolean              g_arg_info_is_optional            (ArgInfo info);
	boolean              g_arg_info_may_be_null            (ArgInfo info);
	Transfer            g_arg_info_get_ownership_transfer (ArgInfo info);
	TypeInfo            g_arg_info_get_type               (ArgInfo info);
	
	boolean              g_type_info_is_pointer          (TypeInfo info);
    TypeTag             g_type_info_get_tag             (TypeInfo info);
	TypeInfo            g_type_info_get_param_type      (TypeInfo info,
						                                  int       n);
	BaseInfo            g_type_info_get_interface       (TypeInfo info);
	int                  g_type_info_get_array_length    (TypeInfo info);
	int                  g_type_info_get_array_fixed_size    (TypeInfo info);	
	boolean              g_type_info_is_zero_terminated  (TypeInfo info);

	int                  g_type_info_get_n_error_domains (TypeInfo info);
	ErrorDomainInfo     g_type_info_get_error_domain    (TypeInfo info,
							                              int       n);
	
	String               g_error_domain_info_get_quark   (ErrorDomainInfo info);
	InterfaceInfo       g_error_domain_info_get_codes   (ErrorDomainInfo info);
	
	NativeLong           g_value_info_get_value      (ValueInfo info);
	
	FieldInfoFlags      g_field_info_get_flags      (FieldInfo info);
	int                  g_field_info_get_size       (FieldInfo info);
	int                  g_field_info_get_offset     (FieldInfo info);
	TypeInfo            g_field_info_get_type       (FieldInfo info);
	
	int                   g_union_info_get_n_fields  (UnionInfo info);
	FieldInfo           g_union_info_get_field     (UnionInfo info,
						           int         n);
	int                   g_union_info_get_n_methods (UnionInfo info);
	FunctionInfo        g_union_info_get_method    (UnionInfo info,
							   int         n);
	boolean               g_union_info_is_discriminated (UnionInfo info);
	int                   g_union_info_get_discriminator_offset (UnionInfo info);
	TypeInfo             g_union_info_get_discriminator_type (UnionInfo info);
	ConstantInfo         g_union_info_get_discriminator      (UnionInfo info,
					    	                                   int         n);

	int                   g_struct_info_get_n_fields  (StructInfo info);
	FieldInfo            g_struct_info_get_field     (StructInfo info,
							    int         n);
	int                   g_struct_info_get_n_methods (StructInfo info);
	FunctionInfo       g_struct_info_get_method    (StructInfo info,
						    int         n);
	FunctionInfo       g_struct_info_find_method   (StructInfo info,
							    String name);
	boolean            g_struct_info_is_gtype_struct (StructInfo info);
	
	String          g_registered_type_info_get_type_name (RegisteredTypeInfo info);
	String          g_registered_type_info_get_type_init (RegisteredTypeInfo info);
	GType           g_registered_type_info_get_g_type    (RegisteredTypeInfo info);
	
	int             g_enum_info_get_n_values             (EnumInfo      info);
	ValueInfo      g_enum_info_get_value                (EnumInfo      info,
								     int            n);
	int             g_enum_info_get_n_values             (FlagsInfo      info);
	ValueInfo      g_enum_info_get_value                (FlagsInfo      info,
								     int            n);	
	
	String          g_object_info_get_type_name 	    (ObjectInfo    info);
	String          g_object_info_get_type_init 	    (ObjectInfo    info);
	boolean         g_object_info_get_abstract             (ObjectInfo    info);	
	ObjectInfo      g_object_info_get_parent             (ObjectInfo    info);
	int                   g_object_info_get_n_interfaces      (ObjectInfo    info);
	InterfaceInfo       g_object_info_get_interface          (ObjectInfo    info,
								     int            n);
	int                   g_object_info_get_n_fields          (ObjectInfo    info);
	FieldInfo           g_object_info_get_field              (ObjectInfo    info,
								     int            n);
	int                   g_object_info_get_n_properties      (ObjectInfo    info);
	PropertyInfo       g_object_info_get_property           (ObjectInfo    info,
								     int            n);
	int                   g_object_info_get_n_methods         (ObjectInfo    info);
	FunctionInfo       g_object_info_get_method             (ObjectInfo    info,
								     int            n);
	FunctionInfo       g_object_info_find_method            (ObjectInfo info,
								     String name);
	int                   g_object_info_get_n_signals          (ObjectInfo    info);
	SignalInfo         g_object_info_get_signal             (ObjectInfo    info,
								     int            n);
	int                   g_object_info_get_n_vfuncs           (ObjectInfo    info);
	VFuncInfo          g_object_info_get_vfunc              (ObjectInfo    info,
								     int            n);
	int                   g_object_info_get_n_constants        (ObjectInfo    info);
	ConstantInfo       g_object_info_get_constant           (ObjectInfo    info,
								     int            n);
	
	int                   g_interface_info_get_n_prerequisites (InterfaceInfo info);
	BaseInfo           g_interface_info_get_prerequisite    (InterfaceInfo info,
								     int        n);
	int                   g_interface_info_get_n_properties    (InterfaceInfo info);
	PropertyInfo       g_interface_info_get_property        (InterfaceInfo info,
								     int        n);
	int                   g_interface_info_get_n_methods       (InterfaceInfo info);
	FunctionInfo       g_interface_info_get_method          (InterfaceInfo info,
								     int        n);
	FunctionInfo       g_interface_info_find_method         (InterfaceInfo info,
			       				             String name);
	int                   g_interface_info_get_n_signals       (InterfaceInfo info);
	SignalInfo         g_interface_info_get_signal          (InterfaceInfo info,
								     int        n);
	int                   g_interface_info_get_n_vfuncs        (InterfaceInfo info);
	VFuncInfo          g_interface_info_get_vfunc           (InterfaceInfo info,
								     int        n);
	int                   g_interface_info_get_n_constants     (InterfaceInfo info);
	ConstantInfo       g_interface_info_get_constant        (InterfaceInfo info,
								     int        n);
	
	int          g_property_info_get_flags                (PropertyInfo         info);
	TypeInfo            g_property_info_get_type                 (PropertyInfo         info);
	
	int                  g_signal_info_get_flags                  (SignalInfo           info);
    VFuncInfo             g_signal_info_get_class_closure          (SignalInfo          info);
	boolean                g_signal_info_true_stops_emit            (SignalInfo          info);
	
	VFuncInfoFlags        g_vfunc_info_get_flags                   (VFuncInfo            info);
	int                    g_vfunc_info_get_offset                  (VFuncInfo            info);
	SignalInfo            g_vfunc_info_get_signal                  (VFuncInfo            info);
	
	TypeInfo              g_constant_info_get_type                 (ConstantInfo         info);
	int                    g_constant_info_get_value                (ConstantInfo         info,
									                                 Argument              value);
}