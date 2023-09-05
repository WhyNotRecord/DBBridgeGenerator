package ru.rexchange.tools;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;

public class DateUtils {
	private static HashMap<String, SimpleDateFormat> dateFormats = new HashMap<>();

	private static SimpleDateFormat getDateFormat(String formatString) {
		SimpleDateFormat dateFormat = (SimpleDateFormat) dateFormats.get(formatString);
		if (dateFormat == null) {
			dateFormat = new SimpleDateFormat(formatString);
			dateFormats.put(formatString, dateFormat);
		}

		return dateFormat;
	}

	public static String formatDateTime(Date date, String formatString) {
		return date == null ? "" : getDateFormat(formatString).format(date);
	}

	/*
	 * public Date parseDateTime(String date, String formatString) throws
	 * ParseException { return getDateFormat(formatString).parse(date); }
	 */

}
