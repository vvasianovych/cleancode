/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;

import com.vectrace.MercurialEclipse.team.CompareAction;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class CompareWithParentHandler extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		IFile file = ResourceUtils.getActiveResourceFromEditor();

		if (file != null) {
			new CompareAction(file).run(null);
		}

		return null;
	}
}