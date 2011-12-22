/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

import com.vectrace.MercurialEclipse.commands.HgAddClient;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.team.cache.RefreshStatusJob;
import com.vectrace.MercurialEclipse.ui.ResourcesTreeContentProvider;
import com.vectrace.MercurialEclipse.ui.UntrackedResourcesFilter;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class AddHandler extends MultipleResourcesHandler {

	@Override
	public void run(final List<IResource> resources) throws HgException {

		MercurialStatusCache cache = MercurialStatusCache.getInstance();
		if(resources.size() == 1 && resources.get(0) instanceof IFile){
			// shortcut for the single file "add..." operation.
			// we do not need any dialogs here.
			IResource resource = resources.get(0);
			if(cache.isUnknown(resource)) {
				HgAddClient.addResources(resources, null);
				new RefreshStatusJob(Messages.getString("AddHandler.refreshStatus"), resource
						.getProject()).schedule();
			}
			return;
		}

		Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(resources);

		Map<HgRoot, Set<IPath>> untrackedFiles = new HashMap<HgRoot, Set<IPath>>();
		Map<HgRoot, Set<IPath>> untrackedFolders = new HashMap<HgRoot, Set<IPath>>();
		for (HgRoot hgRoot : byRoot.keySet()) {
			String[] rawFiles = HgStatusClient.getUntrackedFiles(hgRoot);
			Set<IPath> files = new HashSet<IPath>();
			Set<IPath> folders = new HashSet<IPath>();

			for (String raw : rawFiles) {
				IPath path = new Path(raw);
				if(cache.isIgnored(hgRoot.toAbsolute(path))) {
					continue;
				}
				files.add(path);
				int count = path.segmentCount();
				for (int i = 1; i < count; i++) {
					folders.add(path.removeLastSegments(i));
				}
			}

			untrackedFiles.put(hgRoot, files);
			untrackedFolders.put(hgRoot, folders);
		}

		ViewerFilter untrackedFilter = new UntrackedResourcesFilter(untrackedFiles,
				untrackedFolders);

		CheckedTreeSelectionDialog dialog = new CheckedTreeSelectionDialog(getShell(),
				WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(),
				new ResourcesTreeContentProvider(ResourceUtils.groupByProject(resources).keySet()));

		dialog.setInput(ResourcesTreeContentProvider.ROOT);
		dialog.setTitle(Messages.getString("AddHandler.addToVersionControl")); //$NON-NLS-1$
		dialog.setMessage(Messages.getString("AddHandler.selectFiles")); //$NON-NLS-1$
		dialog.setContainerMode(true);
		dialog.setInitialElementSelections(resources);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.addFilter(untrackedFilter);
		Set<IContainer> expanded = new HashSet<IContainer>();
		for (IResource resource : resources) {
			IContainer parent = resource.getParent();
			while(parent != null && !expanded.contains(parent)){
				if(parent.getType() == IResource.ROOT){
					break;
				}
				expanded.add(parent);
				parent = parent.getParent();
			}
		}
		dialog.setExpandedElements(expanded.toArray(new IContainer[0]));
		if (dialog.open() == IDialogConstants.OK_ID) {
			HgAddClient.addResources(keepFiles(dialog.getResult()), null);
			for (HgRoot root : byRoot.keySet()) {
				new RefreshStatusJob(Messages.getString("AddHandler.refreshStatus"), root).schedule();     //$NON-NLS-1$
			}
		}
	}

	/**
	 * Only keep IFiles
	 */
	private List<IResource> keepFiles(Object[] objects) {
		List<IResource> files = new ArrayList<IResource>();
		for (Object object : objects) {
			if (object instanceof IFile) {
				files.add((IFile) object);
			}
		}
		return files;
	}


}
