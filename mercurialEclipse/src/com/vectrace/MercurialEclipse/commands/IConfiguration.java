/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Stefan	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;


/**
 * @author Stefan
 *
 */
public interface IConfiguration {

	/**
	 * @return
	 */
	String getExecutable();

	/**
	 * @param commandId
	 * @return
	 */
	int getTimeOut(String commandId);

	/**
	 * @param preferenceConstant
	 * @param defaultIfNotSet
	 * @return
	 */
	String getPreference(String preferenceConstant, String defaultIfNotSet);
}
