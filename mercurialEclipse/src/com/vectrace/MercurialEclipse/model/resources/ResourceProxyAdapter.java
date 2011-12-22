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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class ResourceProxyAdapter implements IResourceProxy {

	private final HgResourceAdapter resource;

	public ResourceProxyAdapter(HgResourceAdapter resource) {
		this.resource = resource;
	}

	public long getModificationStamp() {
		return resource.getModificationStamp();
	}

	public boolean isAccessible() {
		return resource.isAccessible();
	}

	public boolean isDerived() {
		return resource.isDerived();
	}

	public boolean isLinked() {
		return resource.isLinked();
	}

	public boolean isPhantom() {
		return resource.isPhantom();
	}

	public boolean isHidden() {
		return resource.isHidden();
	}

	public boolean isTeamPrivateMember() {
		return resource.isTeamPrivateMember();
	}

	public String getName() {
		return resource.getName();
	}

	public Object getSessionProperty(QualifiedName key) {
		try {
			return resource.getSessionProperty(key);
		} catch (CoreException e) {
			MercurialEclipsePlugin.logError(e);
			return null;
		}
	}

	public int getType() {
		return resource.getType();
	}

	public IPath requestFullPath() {
		return resource.getFullPath();
	}

	public IResource requestResource() {
		return resource;
	}

}
