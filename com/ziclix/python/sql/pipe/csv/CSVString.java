
/*
 * Jython Database Specification API 2.0
 *
 * $Id$
 *
 * Copyright (c) 2001 brian zimmer <bzimmer@ziclix.com>
 *
 */
package com.ziclix.python.sql.pipe.csv;

/**
 * A utility class to aide in quoting CSV strings.
 *
 * @author brian zimmer
 * @version $Revision$
 */
public class CSVString {

	/**
	 * The default delimiter.
	 */
	public static final String DELIMITER = ",";

	private CSVString() {}

	/**
	 * Escape the string as needed using the default delimiter.
	 */
	public static String toCSV(String string) {
		return toCSV(string, CSVString.DELIMITER);
	}

	/**
	 * Escape the string as needed using the given delimiter.
	 */
	public static String toCSV(String string, String delimiter) {

		String res = replace(string, "\"", "\"\"");

		if ((res.indexOf("\"") >= 0) || (string.indexOf(delimiter) >= 0)) {
			res = "\"" + res + "\"";
		}

		return res;
	}

	/**
	 * Returns a new string resulting from replacing the first occurrence, or all occurrences,
	 * of search string in this string with replace string.
	 * If the string search does not occur in the character sequence represented by this object,
	 * then this string is returned.
	 * @param search the old string
	 * @param replace the new string
	 * @param all=true all occurrences of the search string are replaced
	 * @param all=false only the first occurrence of the search string is replaced
	 * @return a string derived from this string by replacing the first occurrence,
	 * or every occurrence of search with replace.
	 */
	public static String replace(String original, String search, String replace, boolean all) {

		String valReturn = new String("");
		int l = original.length();
		int lo = search.length();
		int ln = replace.length();
		int i = 0;
		int j;

		while (i <= l) {
			j = original.indexOf(search, i);

			if (j == -1) {
				valReturn = valReturn.concat(original.substring(i, l));
				i = l + 1;		// Stop, no more occurrence.
			} else {
				valReturn = valReturn.concat(original.substring(i, j));
				valReturn = valReturn.concat(replace);
				i = j + lo;

				if (!all) {		// Stop, replace the first occurrence only.
					valReturn = valReturn.concat(original.substring(i, l));
					i = l + 1;	// Stop, replace the first occurrence only.
				}
			}
		}

		return valReturn;
	}

	/**
	 * Returns a new string resulting from replacing all occurrences,
	 * of search string in this string with replace string.
	 * If the string search does not occur in the character sequence represented by this object,
	 * then this string is returned.
	 * @param search the old string
	 * @param replace the new string
	 * @return a string derived from this string by replacing every occurrence of search with replace.
	 */
	public static String replace(String original, String search, String replace) {
		return replace(original, search, replace, true);
	}

	/**
	 * Returns a new string resulting from replacing the end of this String
	 * from oldSuffix to newSuffix.
	 * The original string is returned if it does not end with oldSuffix.
	 * @param oldSuffix the old suffix
	 * @param newSuffix the new suffix
	 * @return a string derived from this string by replacing the end oldSuffix by newSuffix
	 */
	public static String replaceEndWith(String original, String oldSuffix, String newSuffix) {

		if (original.endsWith(oldSuffix)) {
			String st = original.substring(0, original.length() - oldSuffix.length());

			return st.concat(newSuffix);
		} else {
			return original;
		}
	}
}
