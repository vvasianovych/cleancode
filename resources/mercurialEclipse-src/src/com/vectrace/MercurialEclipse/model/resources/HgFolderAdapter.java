/*******************************************************************************
 * Copyright (c) 2011 Andrei Loskutov
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model.resources;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import com.vectrace.MercurialEclipse.model.HgRoot;

public class HgFolderAdapter extends HgContainerAdapter implements IFolder {

	public HgFolderAdapter(File file, HgRoot root, HgContainerAdapter parent) {
		super(file, root, parent);
	}

	public void create(boolean force, boolean local, IProgressMonitor monitor) throws CoreException {
		throwEx();
	}

	public void create(int updateFlags, boolean local, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void createLink(URI location, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		toFile().delete();
	}

	public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	@Override
	public Object getAdapter(Class adapter) {
		if(adapter == IFolder.class) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	public int getType() {
		return IResource.FOLDER;
	}

}
