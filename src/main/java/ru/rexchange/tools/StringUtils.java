package ru.rexchange.tools;

import java.util.Collection;
import java.util.Iterator;

public class StringUtils {
	public static String toUpperCamelCase(String s) {
		String[] parts = s.split("_");
		StringBuilder camelCaseString = new StringBuilder();
		for (String part : parts) {
			camelCaseString.append(toProperCase(part));
		}
		return camelCaseString.toString();
	}

	public static String toLowerCamelCase(String s) {
		String[] parts = s.split("_");
		StringBuilder camelCaseString = new StringBuilder(parts[0].toLowerCase());
		for (int i = 1; i < parts.length; i++) {
			camelCaseString.append(toProperCase(parts[i]));
		}
		return camelCaseString.toString();
	}

	static String toProperCase(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	public static String join(Collection<String> strings, String separator) {
		StringBuilder var2 = new StringBuilder();

		for (String s : strings) {
			if (var2.length() != 0) {
				var2.append(separator);
			}
			var2.append(s);
		}

		return var2.toString();
	}
}
