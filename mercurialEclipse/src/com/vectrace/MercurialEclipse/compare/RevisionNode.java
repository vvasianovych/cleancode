/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.compare;

import java.io.InputStream;

import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;

public class RevisionNode extends ResourceNode {
	private final MercurialRevisionStorage rev;

	public RevisionNode(MercurialRevisionStorage rev) {
		super(rev.getResource());
		this.rev = rev;
	}

	@Override
	public String getName() {
		return getResource().getName();
	}

	public String getLabel()
	{
		return rev.getName();
	}

	@Override
	public InputStream getContents() throws CoreException {
		// prefetch byte content
		getContent();
		return super.getContents();
	}

	@Override
	public long getModificationDate() {
		if (rev.getChangeSet() != null)
		{
			return rev.getChangeSet().getDate().getTime();
		}

		return super.getModificationDate();
	}

	public int getRevision() {
		return rev.getRevision();
	}

	public ChangeSet getChangeSet(){
		return rev.getChangeSet();
	}

	@Override
	protected InputStream createStream() throws CoreException {
		return rev.getContents();
	}

	@Override
	public boolean equals(Object other) {
		boolean superResult = super.equals(other);
		if(!superResult){
			return false;
		}
		// ResourceNode has a bug/feature, that it only compares names, NOT full resource path
		// it means, two index.htm files from different folders are considered equal...
		// See also issue #10757.
		if(!(other instanceof ResourceNode)){
			return false;
		}
		IResource resource1 = getResource();
		IResource resource2 = ((ResourceNode) other).getResource();
		return resource1.equals(resource2);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}