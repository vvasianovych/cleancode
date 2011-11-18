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

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;

/**
 * @author Andrei
 */
public interface IUncommittedChangesetManager {

	UncommittedChangesetGroup getUncommittedGroup();

	void storeChangesets();

	WorkingChangeSet getDefaultChangeset();

	void makeDefault(WorkingChangeSet set);

	public IProject[] getProjects();

}
