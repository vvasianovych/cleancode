/**
 * /*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - Implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.actions.AddToWorkspaceAction;
import com.vectrace.MercurialEclipse.commands.HgPathsClient;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * Defines ProjectSetCapabilities for MercurialEclipse
 *
 * @author Bastian Doetsch
 */
public class MercurialProjectSetCapability extends ProjectSetCapability {

	public static final String SEPARATOR = "|";

	private static MercurialProjectSetCapability instance;

	@Override
	public String[] asReference(IProject[] providerProjects,
			ProjectSetSerializationContext context, IProgressMonitor monitor)
			throws TeamException {

		try {
			String[] references = new String[providerProjects.length];

			monitor
					.beginTask(
							Messages
									.getString("MercurialProjectSetCapability.determiningProjectReferences"), //$NON-NLS-1$
							providerProjects.length);

			for (int i = 0; i < providerProjects.length; i++) {
				String reference = asReference(null, providerProjects[i]
						.getName());
				if (!(monitor.isCanceled() || reference == null)) {
					references[i] = reference;
				} else {
					String msg;
					if (monitor.isCanceled()) {
						msg = Messages
								.getString("MercurialProjectSetCapability.cancelled"); //$NON-NLS-1$
					} else {
						msg = Messages
								.getString("MercurialProjectSetCapability.notDeterminable") //$NON-NLS-1$
								+ providerProjects[i];
					}
					throw new TeamException(msg);
				}
			}
			return references;
		} finally {
			monitor.done();
		}
	}

	@Override
	public IProject[] addToWorkspace(String[] referenceStrings,
			ProjectSetSerializationContext context, IProgressMonitor monitor)
			throws TeamException {

		// use AddToWorkspaceAction to decouple adding from other workspace
		// tasks.
		AddToWorkspaceAction action = new AddToWorkspaceAction();

		// our beloved projects from the import file
		action.setReferenceStrings(referenceStrings);
		try {
			action.run(monitor);
		} catch (Exception e) {
			MessageDialog
					.openError(
							Display.getCurrent().getActiveShell(),
							Messages
									.getString("MercurialProjectSetCapability.errorWhileImporting"), e.getMessage()); //$NON-NLS-1$
		}
		return action.getProjectsCreated();
	}

	@Override
	public String asReference(URI uri, String projectName) {
		String reference = null;
		try {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IProject project = workspace.getRoot().getProject(projectName);
			HgRoot hgRoot = MercurialTeamProvider.getHgRoot(project);

			String srcRepository = ""; //$NON-NLS-1$
			Map<String, String> locs = HgPathsClient.getPaths(hgRoot);
			if (locs.containsKey(HgPathsClient.DEFAULT_PULL)) {
				srcRepository = locs.get(HgPathsClient.DEFAULT_PULL);
			} else if (locs.containsKey(HgPathsClient.DEFAULT)) {
				srcRepository = locs.get(HgPathsClient.DEFAULT);
			} else {
				srcRepository = hgRoot.getAbsolutePath();
			}

			if (srcRepository != null && srcRepository.length() > 0) {
				reference = "MercurialEclipseProjectSet" + SEPARATOR + project.getName()
						+ SEPARATOR + srcRepository;
				IPath path = ResourceUtils.getPath(project);
				if(!hgRoot.getIPath().equals(path)){
					reference += SEPARATOR + hgRoot.toRelative(path.toFile());
				}
			}
		} catch (CoreException e) {
			// reference is null -> error condition
		}
		return reference;
	}

	@Override
	public String getProject(String referenceString) {
		String[] split = referenceString.split("\\" + SEPARATOR);
		if(split.length > 1){
			return split[1];
		}
		return null;
	}

	public String getPullRepo(String referenceString) {
		String[] split = referenceString.split("\\" + SEPARATOR);
		if(split.length > 2){
			return split[2];
		}
		return null;
	}

	public String getRootRelativePath(String referenceString) {
		String[] split = referenceString.split("\\" + SEPARATOR);
		if(split.length > 3){
			return split[3];
		}
		return null;
	}

	/**
	 * Singleton accessor method.
	 */
	public static MercurialProjectSetCapability getInstance() {
		if (instance == null) {
			instance = new MercurialProjectSetCapability();
		}
		return instance;
	}

}
