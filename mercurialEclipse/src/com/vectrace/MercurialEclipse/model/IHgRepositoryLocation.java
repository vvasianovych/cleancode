/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov	- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Interface for hg repository, which might be either local (file) or remote (url)
 * @author Andrei
 */
public interface IHgRepositoryLocation extends IWorkbenchAdapter, IAdaptable {

	int compareTo(IHgRepositoryLocation loc);

	int compareTo(File loc);

	/**
	 * @return might return null
	 */
	String getUser();

	/**
	 * @return might return null
	 */
	String getPassword();

	/**
	 * Return unsafe (with password) URI for repository location if possible
	 * @return a valid URI of the repository or null if repository is local directory
	 * @throws HgException unable to parse to URI or location is invalid.
	 */
	URI getUri() throws HgException;

	/**
	 * @return might return null
	 */
	String getLocation();

	/**
	 * @return might return null
	 */
	String getLogicalName();

	/**
	 * @return true if the repository is on a local file system
	 */
	boolean isLocal();

	/**
	 *
	 * @return null if the repository is NOT local, otherwise the corresponding hg root on the file
	 *         system (representing same absolute (but <b>resolved, canonical</b>) file path)
	 */
	HgRoot toHgRoot();
}