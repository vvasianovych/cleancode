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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.navigator.CommonDragAdapterAssistant;
import org.eclipse.ui.part.ResourceTransfer;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author andrei
 */
public class HgDragAdapterAssistant extends CommonDragAdapterAssistant {

	private static final Transfer[] SUPPORTED_TRANSFERS = new Transfer[] { ResourceTransfer
			.getInstance() };

	public HgDragAdapterAssistant() {
		super();
	}

	@Override
	public Transfer[] getSupportedTransferTypes() {
		return SUPPORTED_TRANSFERS;
	}

	@Override
	public boolean setDragData(DragSourceEvent event, IStructuredSelection selection) {
		if (!ResourceTransfer.getInstance().isSupportedType(event.dataType)) {
			return false;
		}
		IFile[] resources = getSelectedResources(selection);
		if (resources.length == 0) {
			return false;
		}
		event.data = resources;
		return true;
	}

	private static IFile[] getSelectedResources(IStructuredSelection selection) {
		List<IResource> resources = ResourceUtils.getResources(selection);
		final Set<IFile> files = new HashSet<IFile>();
		for (IResource resource : resources) {
			if(resource instanceof IFile) {
				// TODO how to deal with folders in a sync tree? We have to
				// add only those files from the folder which are shown in particular changeset
				files.add((IFile) resource);
			} else if(resource instanceof IContainer) {
				IContainer folder = (IContainer) resource;
				try {
					folder.accept(new IResourceVisitor() {
						public boolean visit(IResource child) throws CoreException {
							if(child instanceof IFile) {
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
		return files.toArray(new IFile[0]);
	}

}

