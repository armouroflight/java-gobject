package org.gnome.gir.gobject.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.gnome.gir.repository.Transfer;
import org.gnome.gir.repository.TypeTag;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReturnContainer {
	public Transfer transfer() default Transfer.NOTHING;
	public TypeTag containerType() default TypeTag.ARRAY;
	public TypeTag paramType();
	public boolean zeroTerminated() default false;
}
