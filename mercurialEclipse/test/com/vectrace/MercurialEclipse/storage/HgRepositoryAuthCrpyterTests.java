/*******************************************************************************
 * Copyright (c) 2009 Intland.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Adam Berkes (Intland) - implementation
 *******************************************************************************/

package com.vectrace.MercurialEclipse.storage;

import java.io.File;
import java.security.InvalidKeyException;

import javax.crypto.SecretKey;

import junit.framework.TestCase;

/**
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class HgRepositoryAuthCrpyterTests extends TestCase {

	private File keyFile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		keyFile = File.createTempFile(".key", "");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if(keyFile != null && !keyFile.delete()) {
			keyFile.deleteOnExit();
		}
	}

	public void testCrypt() throws Exception {
		SecretKey key = HgRepositoryAuthCrypter.generateKey();
		HgRepositoryAuthCrypterFactory.writeBytesToFile(key.getEncoded(), keyFile);
		HgRepositoryAuthCrypter crypter = HgRepositoryAuthCrypterFactory.create(keyFile);
		String data = "test";
		assertEquals(data, crypter.decrypt(crypter.encrypt(data)));
	}

	public void testWrongCrypt() throws Exception {
		try {
			HgRepositoryAuthCrypter.generateKey();
			HgRepositoryAuthCrypterFactory.create(keyFile);
		} catch (InvalidKeyException ex) {
			return;
		}
		assertTrue("Crypter cannot created from empty key data", false);
	}
}
