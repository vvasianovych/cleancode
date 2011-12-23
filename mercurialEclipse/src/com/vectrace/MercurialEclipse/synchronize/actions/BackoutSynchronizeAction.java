/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ilya Ivanov (Intland) implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.wizards.BackoutWizard;

/**
 * @author Ilya Ivanov (Intland)
 */
public class BackoutSynchronizeAction extends SynchronizeModelAction {

	private final HgRoot hgRoot;
	private final ChangeSet changeSet;

	protected BackoutSynchronizeAction(String text, ISynchronizePageConfiguration configuration,
			HgRoot hgRoot, ChangeSet changeSet) {

		super(text, configuration);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/revert.gif"));

		this.hgRoot = hgRoot;
		this.changeSet = changeSet;
	}

	private class BackoutSynchronizeModelOperation extends SynchronizeModelOperation {

		protected BackoutSynchronizeModelOperation(ISynchronizePageConfiguration configuration,
				IDiffElement[] elements) {
			super(configuration, elements);
		}

		public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {

			getShell().getDisplay().asyncExec(new Runnable() {

				public void run() {
					BackoutWizard backoutWizard = new BackoutWizard(hgRoot, changeSet);
					WizardDialog dialog = new WizardDialog(getShell(), backoutWizard);
					dialog.setBlockOnOpen(true);
					int result = dialog.open();

					if(result == Window.OK){
						new RefreshWorkspaceStatusJob(hgRoot, RefreshRootJob.ALL).schedule();
					}
				}

			});
		}
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {

		return new BackoutSynchronizeModelOperation(configuration, elements);
	}
}
