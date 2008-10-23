package org.gnome.gir.compiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameMap {

	private static final Pattern allNumeric = Pattern.compile("[0-9]+");
	private static final Pattern firstNumericUscore = Pattern.compile("([0-9]+)_"); 	
	private static final Pattern replaceFirstNumeric = Pattern.compile("([0-9]+)([A-Za-z]+)"); 
	public static String fixIdentifier(String base, String ident) {
		Matcher match = firstNumericUscore.matcher(ident);
		if (match.lookingAt()) {
			return base + ident;
		}
		match = replaceFirstNumeric.matcher(ident);
		if (!match.lookingAt()) {
			if (allNumeric.matcher(ident).matches()) {
				return base + ident;
			}
			return ident;
		}
		return match.replaceFirst("$2$1");
	}
	
	public static String enumNameToUpper(String base, String nick) {
		return fixIdentifier(base, nick.replace("-", "_")).toUpperCase();
	}

	static String ucaseToPascal(String ucase) {
		String camel = NameMap.ucaseToCamel(ucase);
		return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
	}

	static String ucaseToCamel(String ucase) {
		// So this function works on signal/property names too
		ucase = ucase.replace('-', '_');
		String[] components = ucase.split("_");
		for (int i = 1; i < components.length; i++) {
			if (components[i].length() > 0)
				components[i] = "" + Character.toUpperCase(components[i].charAt(0)) + components[i].substring(1);
		}
		StringBuilder builder = new StringBuilder();
		for (String component : components)
			builder.append(component);
		return builder.toString();
	}
}
