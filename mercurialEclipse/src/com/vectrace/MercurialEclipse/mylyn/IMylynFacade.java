/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * zluspai	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.mylyn;

import org.eclipse.core.resources.IResource;

/**
 * Facade to the mylyn accessor methods.
 *
 * @author zluspai
 */
public interface IMylynFacade {

	/**
	 * Get the comment for the current mylyn task.
	 * @param resources
	 * @return The comment text, or null if no task is active, or mylyn is not used
	 */
	String getCurrentTaskComment(IResource[] resources);

	/**
	 * Get the resources for the current task (this is the current the mylyn context).
	 * @return The resources, or null if no task active or mylyn is not used
	 */
	IResource[] getCurrentTaskResources();

}
