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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.wizards.MercurialParticipantSynchronizeWizard;

public class SyncHandler extends MultipleResourcesHandler {

	@Override
	protected void run(List<IResource> resources) throws Exception {
		if(resources.isEmpty()) {
			return;
		}
		boolean showLocalChanges = getId().equals("com.vectrace.MercurialEclipse.menu.SyncHandler3");
		MercurialParticipantSynchronizeWizard wizard = new MercurialParticipantSynchronizeWizard();
		wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(resources));
		if(showLocalChanges || !shouldShowWizard(wizard)) {
			if(showLocalChanges) {
				HgRoot root = MercurialTeamProvider.getHgRoot(resources.get(0));
				if(root == null) {
					return;
				}
				IProject[] projects;
				if(syncAllProjectsInRepo()) {
					projects = new IProject[] { root.getResource() };
				} else {
					projects = MercurialTeamProvider.getKnownHgProjects(root).toArray(new IProject[0]);
				}
				MercurialSynchronizeParticipant participant = MercurialParticipantSynchronizeWizard.createParticipant(root, projects);
				MercurialParticipantSynchronizeWizard.openSyncView(participant);
			} else {
				wizard.performFinish();
			}
		} else {
			WizardDialog wizardDialog = new WizardDialog(getShell(), wizard);
			wizardDialog.open();
		}
	}

	private boolean shouldShowWizard(MercurialParticipantSynchronizeWizard wizard){
		if(wizard.prepareSettings() == null){
			return true;
		}
		String id = getId();
		return !"com.vectrace.MercurialEclipse.menu.SyncHandler".equals(id);
	}

	private String getId() {
		ExecutionEvent executionEvent = getEvent();
		return executionEvent.getCommand().getId();
	}

	@Override
	protected List<IResource> getSelectedResources() {
		List<IResource> resources = super.getSelectedResources();
		if(syncAllProjectsInRepo()) {
			Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(resources);
			resources.clear();
			Set<HgRoot> roots = byRoot.keySet();
			for (HgRoot root : roots) {
				IProject rootProj = root.getResource();
				resources.add(rootProj);
			}
		}
		return resources;
	}

	private static boolean syncAllProjectsInRepo() {
		return MercurialEclipsePlugin.getDefault().getPreferenceStore()
				.getBoolean(MercurialPreferenceConstants.PREF_SYNC_ALL_PROJECTS_IN_REPO);
	}
}
