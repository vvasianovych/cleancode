/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.eclipse.ui.part.ResourceTransfer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.synchronize.cs.UncommittedChangesetGroup;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class HgDropAdapterAssistant extends CommonDropAdapterAssistant {


	public HgDropAdapterAssistant() {
		super();
	}

	@Override
	public IStatus validateDrop(Object target, int operation, TransferData transferType) {
		if (!LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
			return Status.CANCEL_STATUS;
		}
		if(!(target instanceof WorkingChangeSet) && !(target instanceof UncommittedChangesetGroup)) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter dropAdapter, DropTargetEvent dropTargetEvent,
			Object target) {
		TransferData currentTransfer = dropAdapter.getCurrentTransfer();
		IFile[] files;
		if (LocalSelectionTransfer.getTransfer().isSupportedType(
				currentTransfer)) {
			files = getSelectedResources().toArray(new IFile[0]);
		} else if(ResourceTransfer.getInstance().isSupportedType(currentTransfer)) {
			files = (IFile[]) dropTargetEvent.data;
		} else {
			return Status.CANCEL_STATUS;
		}
		if (files == null || files.length == 0) {
			return Status.CANCEL_STATUS;
		}
		if(dropAdapter.getCurrentTarget() instanceof UncommittedChangesetGroup) {
			createNewChangeset((UncommittedChangesetGroup)dropAdapter.getCurrentTarget(), files);
		} else if(dropAdapter.getCurrentTarget() instanceof WorkingChangeSet) {
			moveToAnotherChangeset((WorkingChangeSet)dropAdapter.getCurrentTarget(), files);
		} else {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	private static void moveToAnotherChangeset(WorkingChangeSet changeSet, IFile[] files) {
		changeSet.getGroup().move(files, changeSet);
	}

	private static void createNewChangeset(UncommittedChangesetGroup group, IFile[] files) {
		group.create(files);
	}

	/**
	 * Returns the resource selection from the LocalSelectionTransfer.
	 *
	 * @return the resource selection from the LocalSelectionTransfer
	 */
	private static Set<IFile> getSelectedResources() {

		ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return Collections.emptySet();
		}
		List<IResource> resources = ResourceUtils.getResources((IStructuredSelection) selection);
		final MercurialStatusCache cache = MercurialStatusCache.getInstance();
		final Set<IFile> files = new HashSet<IFile>();
		for (IResource resource : resources) {
			if(resource instanceof IFile && !cache.isClean(resource)) {
				files.add((IFile) resource);
			} else if(resource instanceof IContainer && !cache.isClean(resource)) {
				IContainer folder = (IContainer) resource;
				try {
					folder.accept(new IResourceVisitor() {
						public boolean visit(IResource child) throws CoreException {
							if(child instanceof IFile && !cache.isClean(child)) {
								files.add((IFile) child);
							}
							return true;
						}
					});
				} catch (CoreException e) {
					MercurialEclipsePlugin.logError(e);
				}
			}
		}
		return files;
	}

}
