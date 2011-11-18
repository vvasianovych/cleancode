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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * Repository location line format:
 * [u|d]<dateAsLong> <len> uri <len> [e] username <len> [e] password <len> alias/id
 */
public final class HgRepositoryLocationParser {

	protected static final String PART_SEPARATOR = " ";
	protected static final String SPLIT_TOKEN = "@@@";
	protected static final String ALIAS_TOKEN = "@alias@";
	protected static final String PASSWORD_TOKEN = ":";
	protected static final String PUSH_PREFIX = "u";
	protected static final String PULL_PREFIX = "d";
	protected static final String ENCRYPTED_PREFIX = "e";

	private HgRepositoryLocationParser() {
		// hide constructor of utility class.
	}

	public static String trimLocation(String url){
		if(url == null){
			return null;
		}
		url = url.trim();
		// this is NOT File.separator: we simply disallow to have different locations
		// which just ends with slash/backslash
		while(url.endsWith("/") || url.endsWith("\\") || url.endsWith(" ")){
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}

	protected static IHgRepositoryLocation parseLine(final String line) {
		if (line == null || line.length() < 1) {
			return null;
		}
		String repositoryLine = line;
		// get direction indicator: OBSOLETED. It does not make sense to save this attribute on the repo.
		// unfortunately if was released with 1.5.0, so simply read the first character
		// String direction = repositoryLine.substring(0,1);
		repositoryLine = repositoryLine.substring(1);
		try {
			List<String> parts = new ArrayList<String>(5);
			while (repositoryLine != null && repositoryLine.length() > 0) {
				int len = Integer.valueOf(repositoryLine.substring(0, repositoryLine.indexOf(PART_SEPARATOR))).intValue();
				repositoryLine = repositoryLine.substring(repositoryLine.indexOf(PART_SEPARATOR) + 1);
				String partValue = repositoryLine.substring(0, len);
				repositoryLine = repositoryLine.substring(repositoryLine.length() > len ? len + 1 : repositoryLine.length());
				parts.add(partValue);
			}
			HgRepositoryAuthCrypter crypter = HgRepositoryAuthCrypterFactory.create();
			String username = parts.get(1);
			if (username.startsWith(ENCRYPTED_PREFIX + PART_SEPARATOR)) {
				username = crypter.decrypt(username.substring(2));
			}
			String password = parts.get(2);
			if (password.startsWith(ENCRYPTED_PREFIX + PART_SEPARATOR)) {
				password = crypter.decrypt(password.substring(2));
			}
			String locationStr = trimLocation(parts.get(0));
			URI uri = parseLocationToURI(locationStr, username, password);
			IHgRepositoryLocation location;
			if(uri != null) {
				location = new HgRepositoryLocation(parts.get(3), uri);
			} else {
				location = new HgRepositoryLocation(parts.get(3), locationStr, "", "");
			}
			return location;
		} catch(Throwable th) {
			MercurialEclipsePlugin.logError(th);
			return null;
		}
	}

	protected static String createLine(final IHgRepositoryLocation location) {
		StringBuilder line = new StringBuilder(PULL_PREFIX);
		// remove authentication from location
		line.append(String.valueOf(location.getLocation().length()));
		line.append(PART_SEPARATOR);
		line.append(location.getLocation());
		line.append(PART_SEPARATOR);
		HgRepositoryAuthCrypter crypter = HgRepositoryAuthCrypterFactory.create();
		String user = location.getUser() != null ? location.getUser() : "";
		if (user.length() > 0) {
			user = ENCRYPTED_PREFIX + PART_SEPARATOR + crypter.encrypt(user);
		}
		line.append(String.valueOf(user.length()));
		line.append(PART_SEPARATOR);
		line.append(user);
		line.append(PART_SEPARATOR);
		String password = location.getPassword() != null ? location.getPassword() : "";
		if (password.length() > 0) {
			password = ENCRYPTED_PREFIX + PART_SEPARATOR + crypter.encrypt(password);
		}
		line.append(String.valueOf(password.length()));
		line.append(PART_SEPARATOR);
		line.append(password);
		line.append(PART_SEPARATOR);
		String logicalName = location.getLogicalName() != null ? location.getLogicalName() : "";
		line.append(String.valueOf(logicalName.length()));
		line.append(PART_SEPARATOR);
		line.append(logicalName);
		return line.toString();
	}

	protected static IHgRepositoryLocation parseLocation(String logicalName, String location, String user, String password) throws HgException {
		return parseLine(logicalName, location, user, password);
	}

	protected static IHgRepositoryLocation parseLocation(String location, String user, String password) throws HgException {
		return parseLocation(null, location, user, password);
	}

	protected static IHgRepositoryLocation parseLine(String logicalName, String location, String user, String password) throws HgException {
		if(StringUtils.isEmpty(location)){
			throw new HgException("Empty location for " + logicalName + " (user:" + user +")!");
		}

		String[] repoInfo = location.split(SPLIT_TOKEN);

		if ((user == null || user.length() == 0)
				&& repoInfo.length > 1) {
			String userInfo = repoInfo[1];
			if (userInfo.contains(ALIAS_TOKEN)) {
				userInfo = userInfo.substring(0, userInfo.indexOf(ALIAS_TOKEN));
			}
			String[] splitUserInfo = userInfo.split(PASSWORD_TOKEN, 2);
			user = splitUserInfo[0];
			if (splitUserInfo.length > 1) {
				password = splitUserInfo[1];
			} else {
				password = null;
			}
			location = repoInfo[0];
		}

		String[] alias = location.split(ALIAS_TOKEN);
		if (alias.length == 2
				&& (logicalName == null || logicalName.length() == 0)) {
			logicalName = alias[1];
			if (location.contains(ALIAS_TOKEN)) {
				location = location.substring(0, location.indexOf(ALIAS_TOKEN));
			}
		}

		location = trimLocation(location);
		URI uri = parseLocationToURI(location, user, password);
		if (uri != null) {
			return new HgRepositoryLocation(logicalName, uri);
		}
		return new HgRepositoryLocation(logicalName, location, user, password);
	}

	protected static URI parseLocationToURI(String location, String user, String password) throws HgException {
		URI uri = null;
		try {
			uri = new URI(location);
		} catch (URISyntaxException e) {
			// Bastian: we can't filter for directories only, as bundle-files are valid repositories
			// as well. So only check if file exists.

			// Most possibly windows path, return null
			File localPath = new File(location);
			if (localPath.exists()) {
				return null;
			}
			throw new HgException("Hg repository location invalid: <" + location + ">");
		}
		if (uri.getScheme() != null
				&& !uri.getScheme().equalsIgnoreCase("file")) { //$NON-NLS-1$
			String userInfo = null;
			if (uri.getUserInfo() != null) {
				// extract user and password from given URI
				String[] authorization = uri.getUserInfo().split(":", 2); //$NON-NLS-1$
				user = authorization[0];
				if (authorization.length > 1) {
					password = authorization[1];
				}
			}
			// This is a hack: ssh doesn't allow us to directly enter
			// in passwords in the URI (even though it says it does)
			if (uri.getScheme().equalsIgnoreCase("ssh")) {
				// Do not provide empty strings as user name: see issue #11828
				if (!StringUtils.isEmpty(user)) {
					userInfo = user;
				}
			} else {
				userInfo = createUserinfo(user, password);
			}

			try {
				return new URI(uri.getScheme(), userInfo,
						uri.getHost(), uri.getPort(), uri.getPath(),
						uri.getQuery(), uri.getFragment());
			} catch (URISyntaxException ex) {
				HgException hgex = new HgException("Failed to parse hg repository: <" + location + ">", ex);
				hgex.initCause(ex);
				throw hgex;
			}
		}
		return uri;
	}

	protected static String getUserNameFromURI(URI uri) {
		String userInfo = uri != null ? uri.getUserInfo() : null;
		if (userInfo != null) {
			if (userInfo.indexOf(PASSWORD_TOKEN) > 0) {
				return userInfo.substring(0, userInfo.indexOf(PASSWORD_TOKEN));
			}
			return userInfo;
		}
		return null;
	}

	protected static String getPasswordFromURI(URI uri) {
		String userInfo = uri != null ? uri.getUserInfo() : null;
		if (userInfo != null) {
			if (userInfo.indexOf(PASSWORD_TOKEN) > 0) {
				return userInfo.substring(userInfo.indexOf(PASSWORD_TOKEN) + 1);
			}
			// NEVER return the username as password
			return "";
		}
		return null;
	}

	private static String createUserinfo(String user1, String password1) {
		String userInfo = null;
		if (user1 != null && user1.length() > 0) {
			// pass gotta be separated by a colon
			if (password1 != null && password1.length() != 0) {
				userInfo = user1 + PASSWORD_TOKEN + password1;
			} else {
				userInfo = user1;
			}
		}
		return userInfo;
	}

	@Deprecated
	public static String createSaveString(IHgRepositoryLocation location) {
		StringBuilder line = new StringBuilder(location.getLocation());
		if (location.getUser() != null) {
			line.append(SPLIT_TOKEN);
			line.append(location.getUser());
			if (location.getPassword() != null) {
				line.append(PASSWORD_TOKEN);
				line.append(location.getPassword());
			}
		}
		if (location.getLogicalName() != null && location.getLogicalName().length() > 0) {
			line.append(ALIAS_TOKEN);
			line.append(location.getLogicalName());
		}
		return line.toString();
	}
}
