/*******************************************************************************
 * Copyright (c) 2009 Intland.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Zsolt Koppany - zsolt.koppany@intland.com
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class ConfigurationWizardMainPageTests extends TestCase {
	public void testGetLocalDirectory() throws Exception {
		ConfigurationWizardMainPage cp = new ConfigurationWizardMainPage("", "", null);

		assertNull(cp.getLocalDirectory(null));
		assertNull(cp.getLocalDirectory("http://intland.com"));
		assertNull(cp.getLocalDirectory("https://codebeamer.com/cb/issue/29229?orgDitchnetTabPaneId=task-details-comments"));
		assertNull(cp.getLocalDirectory("ftp://ftp.codebeamer.com"));
		assertNull(cp.getLocalDirectory("ssh://www.intland.com"));
		assertNull(cp.getLocalDirectory(""));
		assertNull(cp.getLocalDirectory("  "));
		assertNull(cp.getLocalDirectory(null));

		File tempDir = File.createTempFile("hgplg", "");
		tempDir.delete();
		tempDir.mkdir();

		try {
			assertTrue(tempDir.isDirectory());

			File dir = cp.getLocalDirectory(tempDir.getAbsolutePath());

			assertTrue(dir.isDirectory());

			URL url = new URL("file", "", tempDir.getAbsolutePath());
			dir = cp.getLocalDirectory(url.toString());

			assertTrue(dir.isDirectory());

			dir = cp.getLocalDirectory("file://" + tempDir.getAbsolutePath());

			assertTrue(dir.isDirectory());
		} finally {
			tempDir.delete();
		}
	}
}
