/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - removeResources()
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.HgFeatures;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgRemoveClient extends AbstractClient {

	public static void removeResource(IResource resource,
			IProgressMonitor monitor) throws HgException {
		if (monitor != null) {
			monitor.subTask(Messages.getString("HgRemoveClient.removeResource.1") + resource.getName() //$NON-NLS-1$
					+ Messages.getString("HgRemoveClient.removeResource.2")); //$NON-NLS-1$
		}
		HgRoot root = MercurialRootCache.getInstance().getHgRoot(resource);
		HgCommand command = new HgCommand("remove", "Removing resource", root, true); //$NON-NLS-1$
		command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(command.getHgRoot()));
		command.addOptions("--force"); //$NON-NLS-1$
		command.addFiles(resource);
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.REMOVE_TIMEOUT);
		command.executeToBytes();
		MercurialStatusCache.getInstance().refreshStatus(resource, monitor);
	}

	public static void removeResources(List<IResource> resources) throws HgException {
		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(resources);

		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot hgRoot = mapEntry.getKey();
			// if there are too many resources, do several calls
			// From 1.8 hg can do it in one call
			if(!HgFeatures.LISTFILE.isEnabled()) {
			int size = mapEntry.getValue().size();
			int delta = AbstractShellCommand.MAX_PARAMS - 1;
			for (int i = 0; i < size; i += delta) {
				AbstractShellCommand command = new HgCommand("remove", "Removing resource", hgRoot, true); //$NON-NLS-1$
				command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
				command.setUsePreferenceTimeout(MercurialPreferenceConstants.REMOVE_TIMEOUT);
				command.addFiles(mapEntry.getValue().subList(i, Math.min(i + delta, size)));
				command.executeToBytes();
			}
			} else {
				AbstractShellCommand command = new HgCommand("remove", "Removing resource", hgRoot, true); //$NON-NLS-1$
				command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
				command.setUsePreferenceTimeout(MercurialPreferenceConstants.REMOVE_TIMEOUT);
				command.addFiles(mapEntry.getValue());
				command.executeToBytes();
			}
		}
	}

	public static void removeResourcesLater(Map<HgRoot, List<IResource>> resourcesByRoot) throws HgException {
		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot hgRoot = mapEntry.getKey();
			// if there are too many resources, do several calls
			// From 1.8 hg can do it in one call
			int size = mapEntry.getValue().size();
			if(!HgFeatures.LISTFILE.isEnabled()) {
			int delta = AbstractShellCommand.MAX_PARAMS - 1;
			for (int i = 0; i < size; i += delta) {
				final int j = Math.min(i + delta, size);
				AbstractShellCommand command = new HgCommand("remove", //$NON-NLS-1$
						"Removing " + (j - i) + " resources", hgRoot, true);
				command.addOptions("-Af");
				command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
				command.setUsePreferenceTimeout(MercurialPreferenceConstants.REMOVE_TIMEOUT);
				command.addFiles(mapEntry.getValue().subList(i, j));
				command.executeToBytes();
			}
			} else {
				AbstractShellCommand command = new HgCommand("remove", //$NON-NLS-1$
						"Removing " + size + " resources", hgRoot, true);
				command.addOptions("-Af");
				command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
				command.setUsePreferenceTimeout(MercurialPreferenceConstants.REMOVE_TIMEOUT);
				command.addFiles(mapEntry.getValue());
				command.executeToBytes();
			}
		}
	}
}
