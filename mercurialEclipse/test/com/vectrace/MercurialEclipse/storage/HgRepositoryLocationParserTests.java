/*******************************************************************************
 * Copyright (c) 2009 Intland.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Berkes (Intland) - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.storage;

import java.io.File;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;

/**
 * Unit test for parsing/creating hg repository representation
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class HgRepositoryLocationParserTests extends TestCase {

	public void testParseLine() throws Exception {
		parseLine(false);
	}

	public void testParseLineEncrypted() throws Exception {
		parseLine(true);
	}

	public void testParseLineWithProject() throws Exception {
		final String uri = "http://javaforge.com/hg/hgeclipse";
		final String user = "test";
		final String password = "test";
		final String alias = "default";
		IHgRepositoryLocation location = HgRepositoryLocationParser.parseLine(createTestLine(uri, user, password, alias, true));
		assertNotNull(location);
		assertEquals(uri, location.getLocation());
		assertEquals(user, location.getUser());
		assertEquals(password, location.getPassword());
		assertEquals(alias, location.getLogicalName());
	}

	public void testCreateLine() throws Exception {
		final String repo = "http://javaforge.com/hg/hgeclipse";
		final String user = "test";
		final String password = "test";
		final String alias = "default";
		IHgRepositoryLocation location = new HgRepositoryLocation(alias, repo, user, password);
		String repoLine = HgRepositoryLocationParser.createLine(location);
		assertNotNull(repoLine);
		assertTrue(repoLine.length() > 0);
		assertEquals(createTestLine(location.getLocation(), location.getUser(),
				location.getPassword(), location.getLogicalName(), true), repoLine);
	}

	public void testCreateLineWithProject() throws Exception {
		final String repo = "http://javaforge.com/hg/hgeclipse";
		final String user = "test";
		final String password = "test";
		final String alias = "default";
		IHgRepositoryLocation location = new HgRepositoryLocation(alias, repo, user, password);
		String repoLine = HgRepositoryLocationParser.createLine(location);
		assertNotNull(repoLine);
		assertTrue(repoLine.length() > 0);
		assertEquals(createTestLine(location.getLocation(), location.getUser(),
				location.getPassword(), location.getLogicalName(), true), repoLine);
	}

	public void testParseCreateLineLocalWinOld() throws Exception {
		final String uri = "C:\\Documents and settings\\workspace\\hgeclipse";
		final String alias = "default";
		final String user = "test";
		final String password = "test";
		String saveString = null;
		try {
			IHgRepositoryLocation location = HgRepositoryLocationParser.parseLine(alias, uri, user, password);
			assertNotNull(location);
			assertEquals(uri, location.getLocation());
			assertEquals(user, location.getUser());
			assertEquals(password, location.getPassword());
			assertEquals(alias, location.getLogicalName());
			saveString = HgRepositoryLocationParser.createSaveString(location);
		} catch(HgException ex) {
			if (File.pathSeparator.equals("\\")) {
				assertTrue(ex.getMessage(), false);
			}
			return;
		}
		assertNotNull(saveString);
		assertTrue(saveString.length() > 0);
		assertEquals(uri + HgRepositoryLocationParser.SPLIT_TOKEN + user
				+ HgRepositoryLocationParser.PASSWORD_TOKEN + password
				+ HgRepositoryLocationParser.ALIAS_TOKEN + alias, saveString);
	}

	public void testParseCreateLineLocalLinOld() throws Exception {
		final String uri = "/home/adam.berkes/workspace/hgeclipse";
		final String alias = "default";
		IHgRepositoryLocation location = HgRepositoryLocationParser.parseLine(alias, uri, null, null);
		assertNotNull(location);
		assertEquals(uri, location.getLocation());
		assertEquals(null, location.getUser());
		assertEquals(null, location.getPassword());
		assertEquals(alias, location.getLogicalName());
		String saveString = HgRepositoryLocationParser.createSaveString(location);
		assertNotNull(saveString);
		assertTrue(saveString.length() > 0);
		assertEquals(uri + HgRepositoryLocationParser.ALIAS_TOKEN + alias, saveString);
	}

	private void parseLine(boolean toEncryptAuth) throws Exception {
		final String uri = "http://javaforge.com/hg/hgeclipse";
		final String user = "test";
		final String password = "test";
		final String alias = "default";
		IHgRepositoryLocation location = HgRepositoryLocationParser.parseLine(createTestLine(uri, user, password, alias, toEncryptAuth));
		assertNotNull(location);
		assertEquals(uri, location.getLocation());
		assertEquals(user, location.getUser());
		assertEquals(password, location.getPassword());
		assertEquals(alias, location.getLogicalName());
	}

	private String createTestLine(String uri, String user, String password, String alias, boolean toEncryptAuth) {
		HgRepositoryAuthCrypter crypter = HgRepositoryAuthCrypterFactory.create();
		StringBuilder line = new StringBuilder("d");
		line.append(uri.length());
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		line.append(uri);
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		if (toEncryptAuth) {
			user = HgRepositoryLocationParser.ENCRYPTED_PREFIX + HgRepositoryLocationParser.PART_SEPARATOR + crypter.encrypt(user);
		}
		line.append(user.length());
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		line.append(user);
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		if (toEncryptAuth) {
			password = HgRepositoryLocationParser.ENCRYPTED_PREFIX + HgRepositoryLocationParser.PART_SEPARATOR + crypter.encrypt(password);
		}
		line.append(password.length());
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		line.append(password);
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		line.append(alias.length());
		line.append(HgRepositoryLocationParser.PART_SEPARATOR);
		line.append(alias);
		return line.toString();
	}
}
