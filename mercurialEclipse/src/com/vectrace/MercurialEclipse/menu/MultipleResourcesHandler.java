/*******************************************************************************
 * Copyright (c) 2006-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Bastian Doetsch
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public abstract class MultipleResourcesHandler extends AbstractHandler {

	private final List<IResource> selection = new ArrayList<IResource>();
	private ExecutionEvent event;

	protected static Shell getShell() {
		return MercurialEclipsePlugin.getActiveShell();
	}

	/**
	 *
	 * @return never null, list with resources selected by user
	 */
	protected List<IResource> getSelectedResources() {
		return selection;
	}

	public Object execute(ExecutionEvent event1) throws ExecutionException {
		this.event = event1;
		ISelection selectionObject = HandlerUtil.getCurrentSelection(event);
		selection.clear();
		if (selectionObject instanceof IStructuredSelection) {
			selection.addAll(ResourceUtils.getResources((IStructuredSelection) selectionObject));
		}
		if (selection.isEmpty()) {
			selection.add(ResourceUtils.getActiveResourceFromEditor());
		}

		try {
			run(getSelectedResources());
		} catch (Exception e) {
			MessageDialog
					.openError(
							getShell(),
							Messages
									.getString("MultipleResourcesHandler.hgSays"), e.getMessage() + Messages.getString("MultipleResourcesHandler.seeErrorLog")); //$NON-NLS-1$ //$NON-NLS-2$
			throw new ExecutionException(e.getMessage(), e);
		} finally {
			selection.clear();
		}
		this.event = null;
		return null;
	}

	protected static HgRoot ensureSameRoot(List<IResource> resources) throws HgException {
		final HgRoot root = MercurialTeamProvider.getHgRoot(resources.get(0));
		if(root == null) {
			return null;
		}
		for (IResource res : resources) {
			if (!root.equals(MercurialTeamProvider.getHgRoot(res))) {
				throw new HgException(
						Messages
								.getString("MultipleResourcesHandler.allResourcesMustBeInSameProject")); //$NON-NLS-1$
			}
		}
		return root;
	}

	protected abstract void run(List<IResource> resources) throws Exception;

	public ExecutionEvent getEvent() {
		return event;
	}
}