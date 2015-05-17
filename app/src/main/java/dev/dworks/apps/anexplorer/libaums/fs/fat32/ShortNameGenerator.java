/*
 * (C) Copyright 2014 mjahnen <jahnen@in.tum.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package dev.dworks.apps.anexplorer.libaums.fs.fat32;

import java.util.Collection;
import java.util.Locale;

/**
 * This class is responsible for generating valid 8.3 short names for any given
 * long file name.
 * 
 * @author mjahnen
 * @see FatLfnDirectoryEntry
 * @see FatDirectoryEntry
 */
/* package */class ShortNameGenerator {

	/**
	 * See fatgen103.pdf from Microsoft for allowed characters.
	 * 
	 * @param c
	 *            The character to test.
	 * @return True if the character is allowed in an 8.3 short name.
	 */
	private static boolean isValidChar(char c) {
		if (c >= '0' && c <= '9')
			return true;
		if (c >= 'A' && c <= 'Z')
			return true;

		return (c == '$' || c == '%' || c == '\'' || c == '-' || c == '_' || c == '@' || c == '~'
				|| c == '`' || c == '!' || c == '(' || c == ')' || c == '{' || c == '}' || c == '^'
				|| c == '#' || c == '&');
	}

	/**
	 * 
	 * @param str
	 *            The String to test.
	 * @return True if the String contains any invalid chars which are not
	 *         allowed on 8.3 short names.
	 */
	private static boolean containsInvalidChars(String str) {
		int length = str.length();
		for (int i = 0; i < length; i++) {
			final char c = str.charAt(i);
			if (!isValidChar(c))
				return true;
		}
		return false;
	}

	/**
	 * Replaces all invalid characters in an string with an underscore (_).
	 * 
	 * @param str
	 *            The string where invalid chars shall be replaced.
	 * @return The new string only containing valid chars.
	 */
	private static String replaceInvalidChars(String str) {
		int length = str.length();
		StringBuilder builder = new StringBuilder(length);

		for (int i = 0; i < length; i++) {
			final char c = str.charAt(i);
			if (isValidChar(c)) {
				builder.append(c);
			} else {
				builder.append("_");
			}
		}

		return builder.toString();
	}

	/**
	 * Generates an 8.3 short name for a given long file name. It creates a
	 * suffix at the end of the short name if there is already a existing entry
	 * in the directory with an equal short name.
	 * 
	 * @param lfnName
	 *            Long file name.
	 * @param existingShortNames
	 *            The short names already existing in the directory.
	 * @return The generated short name.
	 */
	/* package */static ShortName generateShortName(String lfnName,
			Collection<ShortName> existingShortNames) {
		lfnName = lfnName.toUpperCase(Locale.ROOT).trim();

		// remove leading periods
		int i;
		for (i = 0; i < lfnName.length(); i++) {
			if (lfnName.charAt(i) != '.')
				break;
		}

		lfnName = lfnName.substring(i);

		final int periodIndex = lfnName.lastIndexOf('.');
		String name;
		String extension;
		// suffix is needed if invalid chars have been replaced
		boolean losslyConversion = false;

		if (periodIndex == -1) {
			// no extension given
			if (containsInvalidChars(lfnName)) {
				// suffix is needed
				losslyConversion = true;
				name = replaceInvalidChars(lfnName);
			} else {
				name = lfnName;
			}
			extension = "";
		} else {
			// extension given
			String tmp = lfnName.substring(0, periodIndex);
			if (containsInvalidChars(tmp)) {
				// suffix is needed
				losslyConversion = true;
				name = replaceInvalidChars(lfnName);
			} else {
				name = tmp;
			}

			extension = replaceInvalidChars(lfnName.substring(periodIndex + 1));
			// extension maximum is 3
			if (extension.length() > 3) {
				extension = extension.substring(0, 3);
			}
		}

		name = name.replace(" ", "");
		extension = extension.replace(" ", "");

		ShortName result = new ShortName(name, extension);

		// also create the suffix if the length is bigger than 8 or the short
		// name already exists
		if (losslyConversion || name.length() > 8 || existingShortNames.contains(result)) {
			int maxLen = Math.min(name.length(), 8);
			// 999999 is highest value specified by Microsoft
			for (i = 1; i < 999999; i++) {
				final String suffix = "~" + i;
				final int suffixLen = suffix.length();
				final String newName = name.substring(0, Math.min(maxLen, 8 - suffixLen)) + suffix;
				result = new ShortName(newName, extension);

				if (!existingShortNames.contains(result))
					break;
			}
		}

		return result;
	}

}
