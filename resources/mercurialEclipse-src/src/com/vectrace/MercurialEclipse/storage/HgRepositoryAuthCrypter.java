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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.utils.Base64Coder;

/**
 * En/Crypt authentication data for a persisted repository
 * @author adam.berkes <adam.berkes@intland.com>
 */
public class HgRepositoryAuthCrypter {
	protected static final String DEFAULT_ALGORITHM = "DESede";

	private Cipher ecipher;
	private Cipher dcipher;
	private String algorithm;

	HgRepositoryAuthCrypter(SecretKey key) {
		this(key, DEFAULT_ALGORITHM);
	}

	HgRepositoryAuthCrypter(SecretKey key, String algorithm) {
		try {
			ecipher = Cipher.getInstance(DEFAULT_ALGORITHM);
			dcipher = Cipher.getInstance(DEFAULT_ALGORITHM);
			ecipher.init(Cipher.ENCRYPT_MODE, key);
			dcipher.init(Cipher.DECRYPT_MODE, key);
			this.algorithm = algorithm;

		} catch (javax.crypto.NoSuchPaddingException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (java.security.NoSuchAlgorithmException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (java.security.InvalidKeyException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public static SecretKey generateKey() {
		return generateKey(DEFAULT_ALGORITHM);
	}

	public static SecretKey generateKey(String algorithm) {
		try {
			return KeyGenerator.getInstance(algorithm).generateKey();
		} catch (NoSuchAlgorithmException ex) {
			MercurialEclipsePlugin.logError(ex);
		}
		return null;
	}

	public String encrypt(String str) {
		try {
			// Encode the string into bytes using utf-8
			byte[] utf8 = str.getBytes("UTF8");
			// Encrypt
			byte[] enc = ecipher.doFinal(utf8);
			return new String(Base64Coder.encode(enc));
		} catch (javax.crypto.BadPaddingException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (IllegalBlockSizeException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (UnsupportedEncodingException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return null;
	}

	public String decrypt(String str) {
		try {
			// Decode base64 to get bytes
			byte[] dec = Base64Coder.decode(str);
			// Decrypt
			byte[] utf8 = dcipher.doFinal(dec);
			// Decode using utf-8
			return new String(utf8, "UTF8");
		} catch (javax.crypto.BadPaddingException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (IllegalBlockSizeException e) {
			MercurialEclipsePlugin.logError(e);
		} catch (UnsupportedEncodingException e) {
			MercurialEclipsePlugin.logError(e);
		}
		return null;
	}

	public String getAlgorithm() {
		return algorithm;
	}
}
