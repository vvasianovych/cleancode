/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     VecTrace (Zingo Andersen) - some updates
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.actions;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkbenchPart;

public abstract class HgOperation extends TeamOperation {

	protected String result;

	// constructors

	public HgOperation(IWorkbenchPart part) {
		super(part);
	}

	public HgOperation(IRunnableContext context) {
		super(context);
	}

	public HgOperation(IWorkbenchPart part, IRunnableContext context) {
		super(part, context);
	}

	// operations

	public String getResult() {
		return result;
	}

	// TODO: No background for now.
	@Override
	protected boolean canRunAsJob() {
		return false;
	}

	@Override
	protected String getJobName() {
		return getActionDescription();
	}

	protected abstract String getActionDescription();
}
