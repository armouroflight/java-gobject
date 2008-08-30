/**
 * 
 */
package org.gnome.gir.repository;

import com.sun.jna.Pointer;
import com.sun.jna.Union;

public class Argument extends Union {
	  public Byte v_int8;
	  public Short v_int16;
	  public Integer v_int32;
	  public Long v_int64;
	  public Float v_float;
	  public Double v_double;
	  public String v_string;
	  public Pointer v_pointer;
}