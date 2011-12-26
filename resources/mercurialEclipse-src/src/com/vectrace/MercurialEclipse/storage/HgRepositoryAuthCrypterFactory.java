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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 * Create a new crypter using stored secret key
 * @author adam.berkes <adam.berkes@intland.com>
 */
public final class HgRepositoryAuthCrypterFactory {

	public static final String DEFAULT_KEY_FILENAME = ".key";

	private HgRepositoryAuthCrypterFactory() {
		// hide constructor of utility class.
	}

	public static HgRepositoryAuthCrypter create(File keyFile) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException {
		KeySpec keySpec = new DESedeKeySpec(getBytesFromFile(keyFile));
		SecretKey key = SecretKeyFactory.getInstance(HgRepositoryAuthCrypter.DEFAULT_ALGORITHM).generateSecret(keySpec);
		return new HgRepositoryAuthCrypter(key);
	}

	public static HgRepositoryAuthCrypter create() {
		try {
			File keyFile = MercurialEclipsePlugin.getDefault().getStateLocation().append(DEFAULT_KEY_FILENAME).toFile();
			if (keyFile.isFile()) {
				return create(keyFile);
			}
			SecretKey key = HgRepositoryAuthCrypter.generateKey();
			writeBytesToFile(key.getEncoded(), keyFile);
			return new HgRepositoryAuthCrypter(key);
		} catch (Exception ex) {
			MercurialEclipsePlugin.logError(ex);
		}
		return null;
	}

	private static byte[] getBytesFromFile(File file) throws IOException {
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
		try {
			long length = file.length();
			byte[] bytes = new byte[(int) length];
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length
				&& (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}
			if (offset < bytes.length) {
				throw new IOException("Could not completely read file " + file.getName());
			}
			return bytes;
		} finally {
			is.close();
		}
	}

	protected static void writeBytesToFile(byte[] content, File file) throws IOException {
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		try {
			os.write(content, 0, content.length);
		} finally {
			os.close();
			secureKeyFile(file);
		}
	}

	private static void secureKeyFile(File file) {
		// Ensure file availability only for user
		List<String> chmod = new ArrayList<String>(3);

		if (File.separatorChar == '\\') {
			chmod.add("cacls");
			chmod.add(file.getAbsolutePath());
			chmod.add("/T");
			chmod.add("/G");
			// TODO: find a better method as it is a possible security hole.
			// property can be overridden at eclipse startup.
			chmod.add(System.getProperty("user.name") + ":f");
			chmod.add("/R");
			chmod.add("Everyone");
		} else {
			chmod.add("chmod");
			chmod.add("600");
			chmod.add(file.getAbsolutePath());
		}
		try {
			Runtime.getRuntime().exec(chmod.toArray(new String[] {}));
		} catch (IOException ex) {
			MercurialEclipsePlugin.logError("Unable to set permission on file", ex);
		}
	}
}
