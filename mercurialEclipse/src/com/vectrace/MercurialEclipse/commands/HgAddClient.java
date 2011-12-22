/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
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
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgAddClient extends AbstractClient {

	public static void addResources(List<IResource> resources,
			IProgressMonitor monitor) throws HgException {
		Map<HgRoot, List<IResource>> resourcesByRoot = ResourceUtils.groupByRoot(resources);
		for (Map.Entry<HgRoot, List<IResource>> mapEntry : resourcesByRoot.entrySet()) {
			HgRoot hgRoot = mapEntry.getKey();
			if (monitor != null) {
				monitor.subTask(Messages.getString("HgAddClient.addingResourcesFrom") + hgRoot.getName()); //$NON-NLS-1$
			}
			// if there are too many resources, do several calls
			// From 1.8 hg can do it in one call
			if(!HgFeatures.LISTFILE.isEnabled()) {
			int size = mapEntry.getValue().size();
			int delta = AbstractShellCommand.MAX_PARAMS - 1;
			for (int i = 0; i < size; i += delta) {
				final int j = Math.min(i + delta, size);
				AbstractShellCommand command = new HgCommand("add", //$NON-NLS-1$
						"Adding " + j + " resources", hgRoot, true);
				command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
				command.setUsePreferenceTimeout(MercurialPreferenceConstants.ADD_TIMEOUT);
				command.addFiles(mapEntry.getValue().subList(i, j));
				command.executeToBytes();
			}
			} else {
				AbstractShellCommand command = new HgCommand("add", //$NON-NLS-1$
						"Adding resources", hgRoot, true);
				command.setExecutionRule(new AbstractShellCommand.ExclusiveExecutionRule(hgRoot));
				command.setUsePreferenceTimeout(MercurialPreferenceConstants.ADD_TIMEOUT);
				command.addFiles(mapEntry.getValue());
				command.executeToBytes();
			}
		}
	}
}
