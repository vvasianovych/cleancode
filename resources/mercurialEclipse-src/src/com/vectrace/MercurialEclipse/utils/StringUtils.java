/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Berkes (Intland)     - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import java.util.regex.Pattern;

/**
 * General string manipulation functions set.
 *
 * @author adam.berkes <adam.berkes@intland.com>
 */
public final class StringUtils {
	public static final String NEW_LINE = System.getProperty("line.separator");

	private static final String OPTIONAL_BLANK_AND_TAB = "[ \t]*";

	private static final String REGEX = OPTIONAL_BLANK_AND_TAB + "(\r|\n)+" + OPTIONAL_BLANK_AND_TAB;
	private static final Pattern NEWLINE_PATTERN = Pattern.compile(REGEX);

	private StringUtils() {
		// hide constructor of utility class.
	}

	/**
	 * Removes all \r \n characters and optional leading and/or trailing blanks and TAB characters.
	 *
	 * @param text trimmed result
	 * @return
	 */
	public static String removeLineBreaks(String text) {
		if (text != null && text.length() != 0) {
			text = NEWLINE_PATTERN.matcher(text).replaceAll(" ");
			text = text.trim();
		}
		return text;
	}

	/**
	 * @param text any kind of text
	 * @return true if the text is either null or empty string
	 */
	public static boolean isEmpty(String text){
		return text == null || text.trim().length() == 0;
	}
}
