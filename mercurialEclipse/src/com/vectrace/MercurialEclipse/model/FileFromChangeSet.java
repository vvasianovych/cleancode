/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.properties.DoNotDisplayMe;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetResourceMapping;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Andrei
 */
public class FileFromChangeSet implements IAdaptable{

	/**
	 * May be null. Eg a resource is removed
	 */
	private final IFile file;

	/** May be null. Only not null when a file has been copied or moved. */
	private final IFile copySrcFile;

	/**
	 * Not null
	 */
	private final ChangeSet changeset;

	private final int kind;

	private FileStatus fileStatus;

	/**
	 * @param changeset non null
	 * @param fileStatus non null
	 * @param diffKind see {@link Differencer}
	 */
	public FileFromChangeSet(ChangeSet changeset, FileStatus fileStatus, int diffKind) {
		this(changeset,
				ResourceUtils.getFileHandle(fileStatus.getAbsolutePath()),
				ResourceUtils.getFileHandle(fileStatus.getAbsoluteCopySourcePath()),
				diffKind);
		this.fileStatus = fileStatus;
	}

	/**
	 * @param changeset non null
	 * @param file may be null
	 * @param diffKind
	 */
	public FileFromChangeSet(ChangeSet changeset, IFile file, IFile copySourceFile, int diffKind) {
		this.changeset = changeset;
		this.file = file;
		this.copySrcFile = copySourceFile;
		this.kind = diffKind;
	}

	public Object getAdapter(Class adapter) {
		// Resource adapter is enabled for "working" changeset only to avoid "dirty"
		// decorations shown in the tree on changeset files from already commited changesets
		if(changeset instanceof WorkingChangeSet && adapter == IResource.class){
			return file;
		}
		if (adapter == IFile.class /* || adapter == IResource.class*/) {
			return file;
		}
		if(adapter == ResourceMapping.class){
			return new HgChangeSetResourceMapping(this);
		}
		return null;
	}

	/**
	 * @return see {@link Differencer}
	 */
	@DoNotDisplayMe
	public int getDiffKind() {
		return kind;
	}

	/**
	 * @return Path relative to the hg root or the containing project, whichever has the greater
	 *         number of segments. May return null.
	 */
	public IPath getPath() {
		if (fileStatus != null) {
			return fileStatus.getRootRelativePath();
		} else if (file != null) {
			HgRoot root = changeset.getHgRoot();
			if (root == null) {
				root = MercurialTeamProvider.getHgRoot(file);
			}

			IPath path1, path2 = file.getProjectRelativePath();
			if (root != null && (path1 = root.toRelative(file)) != null)  {
				if (path2 == null) {
					return path1;
				}
				return path1.segmentCount() > path2.segmentCount() ? path1 : path2;
			}
			return path2;
		}
		return null;
	}

	@Override
	public String toString() {
		return file != null? file.toString() : fileStatus.getAbsolutePath().toOSString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((changeset == null) ? 0 : changeset.hashCode());
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof FileFromChangeSet)) {
			return false;
		}
		FileFromChangeSet other = (FileFromChangeSet) obj;
		if (changeset == null) {
			if (other.changeset != null) {
				return false;
			}
		} else if (!changeset.equals(other.changeset)) {
			return false;
		}
		if (file == null) {
			if (other.file != null) {
				return false;
			}
		} else if (!file.equals(other.file)) {
			return false;
		}
		return true;
	}

	public IFile getFile() {
		return file;
	}

	/**
	 * Getter for the original source file handle of copied or moved files (if any)
	 * @return source file handle (must not exist) if the file was copied or moved, null otherwise
	 */
	public IFile getCopySourceFile(){
		return copySrcFile;
	}

	public boolean isCopy(){
		return copySrcFile != null;
	}

	@DoNotDisplayMe
	public ChangeSet getChangeset() {
		return changeset;
	}
}
