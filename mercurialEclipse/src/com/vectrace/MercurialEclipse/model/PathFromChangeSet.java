/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;


public abstract class PathFromChangeSet implements IAdaptable {

	private final Object parent;

	private final String label;

	protected IResource resource;

	protected PathFromChangeSet(Object prnt, String leadingSegment) {
		parent = prnt;
		label = leadingSegment;
	}

	@Override
	public String toString() {
		if(isProjectClosed()) {
			return label + " (closed!)";
		}
		return label;
	}

	public boolean isProjectClosed() {
		return resource!= null && !resource.getProject().isOpen();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof PathFromChangeSet) {
			PathFromChangeSet o = (PathFromChangeSet) other;
			if (o.label.equals(label) && o.parent.equals(parent)) {
				if (o.resource == null) {
					return resource == null;
				}
				return o.resource.equals(resource);
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 73 ^ label.hashCode() + parent.hashCode();
	}

	public final Object getAdapter(Class adapter) {
		if (IResource.class.equals(adapter)) {
			return resource;
		}
		return null;
	}

	public abstract Object[] getChildren();

	/**
	 * @return non null set with all files (recursive) contained under the given path
	 */
	public abstract Set<FileFromChangeSet> getFiles();

}