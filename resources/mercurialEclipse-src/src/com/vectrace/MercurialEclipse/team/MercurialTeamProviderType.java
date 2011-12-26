/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - new qualified name for project sets
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.RepositoryProviderType;

public class MercurialTeamProviderType extends RepositoryProviderType {

	public MercurialTeamProviderType() {
	}

	@Override
	public ProjectSetCapability getProjectSetCapability() {
		return MercurialProjectSetCapability.getInstance();
	}



}
