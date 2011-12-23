/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * wleggette	implementation
 *     Andrei Loskutov - bug fixes
 *     Zsolt Koppany (Intland)
 *     Philip Graf               - Fixed bugs which FindBugs found
 *******************************************************************************/
/* ====================================================================
 *
 * @PROJECT.FULLNAME@ @VERSION@ License.
 *
 * Copyright (c) @YEAR@ L2FProd.com.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by L2FProd.com
 *        (http://www.L2FProd.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "@PROJECT.FULLNAME@", "SkinLF" and "L2FProd.com" must not
 *    be used to endorse or promote products derived from this software
 *    without prior written permission. For written permission, please
 *    contact info@L2FProd.com.
 *
 * 5. Products derived from this software may not be called "SkinLF"
 *    nor may "SkinLF" appear in their names without prior written
 *    permission of L2FProd.com.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL L2FPROD.COM OR ITS CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 */
package com.vectrace.MercurialEclipse.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * @author $Author: l2fprod $
 * @created 27 avril 2002
 * $Id$
 */
public class IniFile {

	private final Map<String, Map<String, String>> sections;

	/**
	 * Constructor for the IniFile object
	 */
	public IniFile() {
		sections = new HashMap<String, Map<String, String>>();
	}

	/**
	 * Constructor for the IniFile object
	 *
	 * @param filename
	 *            Description of Parameter
	 * @exception FileNotFoundException
	 *                Description of Exception
	 */
	public IniFile(String filename) throws FileNotFoundException {
		this();
		load(filename);
	}

	/**
	 * Sets the KeyValue attribute of the IniFile object
	 *
	 * @param section
	 *            The new KeyValue value
	 * @param key
	 *            The new KeyValue value
	 * @param value
	 *            The new KeyValue value
	 */
	public void setKeyValue(String section, String key, String value) {
		Map<String, String> section2 = getSection(section);
		if(section2 != null) {
			section2.put(key.toLowerCase(), value);
		}
	}

	/**
	 * Gets the Sections attribute of the IniFile object
	 *
	 * @return The Sections value
	 */
	public Map<String, Map<String, String>> getSections() {
		return sections;
	}

	/**
	 * Gets the Section attribute of the IniFile object
	 *
	 * @param section
	 *            Description of Parameter
	 * @return The Section value
	 */
	public Map<String, String> getSection(String section) {
		if(section == null){
			return sections.get(null);
		}
		return sections.get(section.toLowerCase());
	}

	/**
	 * Gets the NullOrEmpty attribute of the IniFile object
	 *
	 * @param section
	 *            Description of Parameter
	 * @param key
	 *            Description of Parameter
	 * @return The NullOrEmpty value
	 */
	public boolean isNullOrEmpty(String section, String key) {
		String value = getKeyValue(section, key);
		return value == null || value.length() == 0;
	}

	/**
	 * Gets the KeyValue attribute of the IniFile object. If the specified section is null, only values that
	 * are defined before the first section may be returned
	 *
	 * @param section
	 *            Description of Parameter
	 * @param key
	 *            Description of Parameter
	 * @return The KeyValue value
	 */
	public String getKeyValue(String section, String key) {
		Map<String, String> section2 = getSection(section);
		if(section2 != null) {
			return section2.get(key.toLowerCase());
		}
		return null;
	}

	/**
	 * Gets the KeyIntValue attribute of the IniFile object
	 *
	 * @param section
	 *            Description of Parameter
	 * @param key
	 *            Description of Parameter
	 * @return The KeyIntValue value
	 */
	public int getKeyIntValue(String section, String key) {
		return getKeyIntValue(section, key, 0);
	}

	/**
	 * Gets the KeyIntValue attribute of the IniFile object
	 *
	 * @param section
	 *            Description of Parameter
	 * @param key
	 *            Description of Parameter
	 * @param defaultValue
	 *            Description of Parameter
	 * @return The KeyIntValue value
	 */
	public int getKeyIntValue(String section, String key, int defaultValue) {
		String value = getKeyValue(section, key.toLowerCase());
		if (value == null) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return 0;
		}
	}


	/**
	 * Description of the Method
	 *
	 * @param filename
	 *            Description of Parameter
	 * @exception FileNotFoundException
	 *                Description of Exception
	 */
	public void load(String filename) throws FileNotFoundException {
		FileInputStream in = new FileInputStream(filename);
		try {
			load(in);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
	}

	/**
	 * Description of the Method
	 *
	 * @param filename
	 *            Description of Parameter
	 * @exception IOException
	 *                Description of Exception
	 */
	public void save(String filename) throws IOException {
		FileOutputStream out = new FileOutputStream(filename);
		try {
			save(out);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	/**
	 * Loads the whole init file. Values that are defined before the first section are stored in the null section.
	 *
	 * @param in
	 *            Description of Parameter
	 */
	private void load(InputStream in) {
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(in));
			try {
				Map<String, String> section = null;
				String sectionName;
				for (String read = null; (read = input.readLine()) != null;) {
					if (read.startsWith(";") || read.startsWith("#")) {
						continue;
					} else if (read.startsWith("[")) {
						// new section
						sectionName = read.substring(1, read.indexOf("]")).toLowerCase();
						section = sections.get(sectionName);
						if (section == null) {
							section = new HashMap<String, String>();
							sections.put(sectionName, section);
						}
					} else if (read.indexOf("=") != -1) {
						if(section == null){
							// create the null-section entry
							section = new HashMap<String, String>();
							sections.put(null, section);
						}
						// new key
						String key = read.substring(0, read.indexOf("=")).trim().toLowerCase();
						String value = read.substring(read.indexOf("=") + 1).trim();
						section.put(key, value);
					}
				}
			} finally {
				input.close();
			}
		} catch (IOException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	/**
	 * Description of the Method
	 *
	 * @param out
	 *            Description of Parameter
	 */
	private void save(OutputStream out) {
		try {
			PrintWriter output = new PrintWriter(out);

			try {
				for (String section : sections.keySet()) {
					output.println("[" + section + "]");
					for (Map.Entry<String, String> entry : getSection(section).entrySet()) {
						output.println(entry.getKey() + "=" + entry.getValue());
					}
				}
			} finally {
				output.close();
			}
		} finally {
			try {
				out.close();
			} catch (IOException ex) {
			}
		}
	}

	/**
	 * Adds a feature to the Section attribute of the IniFile object
	 *
	 * @param section
	 *            The feature to be added to the Section attribute
	 */
	public void addSection(String section) {
		sections.put(section.toLowerCase(), new HashMap<String, String>());
	}

	/**
	 * Description of the Method
	 *
	 * @param section
	 *            Description of Parameter
	 */
	public void removeSection(String section) {
	}

}
