/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.RepositoryProvider;

import com.vectrace.MercurialEclipse.team.ResourceDecorator;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;

public class DisconnectHandler extends SingleResourceHandler {

	@Override
	protected void run(IResource resource) throws Exception {
		IProject project = resource.getProject();
		RepositoryProvider.unmap(project);
		MercurialStatusCache.getInstance().clear(project, false);
		LocalChangesetCache.getInstance().clear(project, false);
		ResourceDecorator.updateClientDecorations();
	}

}
