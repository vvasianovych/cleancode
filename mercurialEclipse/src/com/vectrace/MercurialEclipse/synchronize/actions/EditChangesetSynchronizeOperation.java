/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Subclipse                    - original impl.o
 *     Andrei Loskutov - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.window.Window;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

import com.vectrace.MercurialEclipse.dialogs.EditChangesetDialog;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class EditChangesetSynchronizeOperation extends SynchronizeModelOperation {
	private final WorkingChangeSet changeset;
//	private final ISynchronizePageConfiguration configuration;
//	private MercurialSynchronizeParticipant participant;

	public EditChangesetSynchronizeOperation(
			ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, WorkingChangeSet input) {
		super(configuration, elements);
//		this.configuration = configuration;
//		ISynchronizeParticipant participant1 = configuration.getParticipant();
//		if(participant1 instanceof MercurialSynchronizeParticipant) {
//			this.participant = (MercurialSynchronizeParticipant) participant1;
//		}
		this.changeset = input;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		getPart();
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				// TODO get hg root and get the changeset type
				boolean isDefaultChangeset = changeset.isDefault();
				HgRoot hgRoot;
				Set<IFile> files = changeset.getFiles();
				if(files.isEmpty()){
					hgRoot = HgRoot.NO_ROOT;
				} else {
					// TODO not elegant. Should we restrict changesets to one root only?
					hgRoot = ResourceUtils.groupByRoot(files).keySet().iterator().next();
				}
				EditChangesetDialog dialog = new EditChangesetDialog(getShell(), hgRoot, changeset, isDefaultChangeset);
				int ok = dialog.open();
				if(Window.OK != ok){
					return;
				}
				changeset.getGroup().changesetChanged(changeset);
			}
		});
		monitor.done();
	}
}
