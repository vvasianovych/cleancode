/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ilya Ivanov (Intland)	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.wizards.StripWizard;

/**
 * @author Ilya Ivanov (Intland)
 */
public class StripSynchronizeAction extends SynchronizeModelAction {

	private final HgRoot hgRoot;
	private final ChangeSet changeSet;

	protected StripSynchronizeAction(String text, ISynchronizePageConfiguration configuration,
			HgRoot hgRoot, ChangeSet changeSet) {

		super(text, configuration);
		setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/revert.gif"));

		this.hgRoot = hgRoot;
		this.changeSet = changeSet;
	}

	private class StripSynchronizeModelOperation extends SynchronizeModelOperation {

		protected StripSynchronizeModelOperation(ISynchronizePageConfiguration configuration,
				IDiffElement[] elements) {
			super(configuration, elements);
		}

		public void run(IProgressMonitor monitor) throws InvocationTargetException,
				InterruptedException {

			getShell().getDisplay().asyncExec(new Runnable() {

				public void run() {
					StripWizard stripWizard = new StripWizard(hgRoot, changeSet);
					WizardDialog dialog = new WizardDialog(getShell(), stripWizard);
					dialog.setBlockOnOpen(true);
					dialog.open();
				}

			});
		}
	}

	@Override
	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {

		return new StripSynchronizeModelOperation(configuration, elements);
	}
}
