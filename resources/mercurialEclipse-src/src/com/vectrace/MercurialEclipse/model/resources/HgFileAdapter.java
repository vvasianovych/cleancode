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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentTypeManager;

import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class HgFileAdapter extends HgResourceAdapter implements IFile {

	public HgFileAdapter(File file, HgRoot root, HgContainerAdapter parent) {
		super(file, root, parent);
	}

	public void appendContents(InputStream source, boolean force, boolean keepHistory,
			IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
		appendContents(source, updateFlags, monitor);
	}

	public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void create(InputStream source, boolean force, IProgressMonitor monitor)
			throws CoreException {
		create(source, (force ? IResource.FORCE : IResource.NONE), monitor);
	}

	public void create(InputStream source, int updateFlags, IProgressMonitor monitor)
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

	public String getCharset() throws CoreException {
		return getHgRoot().getEncoding();
	}

	public String getCharset(boolean checkImplicit) throws CoreException {
		return getCharset();
	}

	public String getCharsetFor(Reader reader) throws CoreException {
		// tries to obtain a description from the contents provided
		IContentDescription description;
		try {
			IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
			description = contentTypeManager.getDescriptionFor(reader, getName(), new QualifiedName[] {IContentDescription.CHARSET});
		} catch (IOException e) {
			throw new HgException("Failed to retrieve contents charset", e);
		}
		if (description != null) {
			String charset;
			if ((charset = description.getCharset()) != null) {
				return charset;
			}
		}
		return getCharset();
	}

	public IContentDescription getContentDescription() throws CoreException {
		return null;
	}

	public InputStream getContents() throws CoreException {
		try {
			return new FileInputStream(toFile());
		} catch (IOException e) {
			throw new HgException("Failed to open stream", e);
		}
	}

	public InputStream getContents(boolean force) throws CoreException {
		return getContents();
	}

	@SuppressWarnings("deprecation")
	public int getEncoding() throws CoreException {
		return IFile.ENCODING_UTF_8;
	}

	public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
		return new IFileState[0];
	}

	public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		throwEx();
	}

	public void setCharset(String newCharset) throws CoreException {
		throwEx();
	}

	public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
		throwEx();
	}

	public void setContents(InputStream source, boolean force, boolean keepHistory,
			IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
		setContents(source, updateFlags, monitor);
	}

	public void setContents(IFileState source, boolean force, boolean keepHistory,
			IProgressMonitor monitor) throws CoreException {
		int updateFlags = force ? IResource.FORCE : IResource.NONE;
		updateFlags |= keepHistory ? IResource.KEEP_HISTORY : IResource.NONE;
		setContents(source.getContents(), updateFlags, monitor);
	}

	public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		if(source == null) {
			source = new ByteArrayInputStream(new byte[0]);
		}
		try {
			FileOutputStream fos = new FileOutputStream(toFile());
			byte [] buf = new byte [8096];
			int size;
			while((size = source.read(buf)) > 0) {
				fos.write(buf, 0, size);
			}
		} catch (IOException e) {
			throw new HgException("Failed to write into file", e);
		}

	}

	public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor)
			throws CoreException {
		setContents(source.getContents(), updateFlags, monitor);
	}

	public int getType() {
		return IResource.FILE;
	}

	@Override
	public Object getAdapter(Class adapter) {
		if(adapter == IFile.class) {
			return this;
		}
		return super.getAdapter(adapter);
	}

	public String getFileExtension() {
		return getLocation().getFileExtension();
	}

	public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
		visitor.visit(createProxy());
	}

	public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
		visitor.visit(this);
	}
}
