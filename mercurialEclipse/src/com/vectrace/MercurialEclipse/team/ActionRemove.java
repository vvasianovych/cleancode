/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     Stefan Groschupf          - logError
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRemoveClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author zingo
 */
public class ActionRemove implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	private IStructuredSelection selection;

	public ActionRemove() {
		super();
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 *
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
		selection = null;
		window = null;
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 *
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow w) {
		this.window = w;
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 *
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		Shell shell = window != null ? window.getShell() : MercurialEclipsePlugin.getActiveShell();
		if (!confirmRemove(shell)) {
			return;
		}
		List<IResource> resources = ResourceUtils.getResources(selection);
		Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(resources);
		try {
			HgRemoveClient.removeResourcesLater(byRoot);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		} finally {
			for (Map.Entry<HgRoot, List<IResource>> mapEntry : byRoot.entrySet()) {
				HgRoot hgRoot = mapEntry.getKey();
				new RefreshStatusJob("Refreshing " + hgRoot.getName(), hgRoot).schedule();
			}
		}
	}

	private boolean confirmRemove(Shell shell) {
		return MessageDialog.openConfirm(shell,
				Messages.getString("ActionRemove.removeFileTitle"),
				Messages.getString("ActionRemove.removeFileConfirmation"));
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 *
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection newSelection) {
		if (newSelection instanceof IStructuredSelection) {
			selection = (IStructuredSelection) newSelection;
		} else {
			selection = null;
		}
	}

}
