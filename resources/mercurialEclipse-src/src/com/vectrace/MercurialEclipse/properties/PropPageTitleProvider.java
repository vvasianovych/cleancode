/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov			- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.properties;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;

import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.Tag;

/**
 * Label provider used to represent some mercurial objects in the properties view
 * @author andrei
 */
public class PropPageTitleProvider implements ILabelProvider {

	public void addListener(ILabelProviderListener listener) {
		// noop
	}

	public void dispose() {
		// noop
	}

	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	public void removeListener(ILabelProviderListener listener) {
		// noop
	}

	public Image getImage(Object element) {
		return null;
	}

	public String getText(Object element) {
		if(element instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) element;
			element = selection.getFirstElement();
		}
		if(element instanceof MercurialRevision) {
			MercurialRevision revision = (MercurialRevision) element;
			return "Revision " + revision.getRevision() + " of " + revision.getName();
		}
		if(element instanceof FileStatus) {
			FileStatus status = (FileStatus) element;
			return status.getRootRelativePath().lastSegment();
		}
		if(element instanceof Tag) {
			Tag tag = (Tag) element;
			return tag.getName();
		}
		if(element instanceof ChangeSet) {
			ChangeSet cs = (ChangeSet) element;
			return cs.getName();
		}
		if(element instanceof FileFromChangeSet) {
			FileFromChangeSet fcs = (FileFromChangeSet) element;
			IPath path = fcs.getPath();
			if(path != null) {
				return path.toOSString();
			}
			return fcs.toString();
		}
		if(element == null) {
			return null;
		}
		if(element.getClass().isArray()) {
			int length = ((Object[])element).length;
			return length > 0? "" + length : "";
		}
		if(Collection.class.isAssignableFrom(element.getClass())) {
			int length = ((Collection<?>)element).size();
			return length > 0? "" + length : "";
		}
		return element.getClass().getSimpleName();
	}

}
