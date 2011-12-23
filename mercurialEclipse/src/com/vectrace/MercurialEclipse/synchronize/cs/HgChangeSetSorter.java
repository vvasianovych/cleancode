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
package com.vectrace.MercurialEclipse.synchronize.cs;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

import com.vectrace.MercurialEclipse.history.ChangeSetComparator;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;

/**
 * @author Andrei
 */
public class HgChangeSetSorter extends ViewerSorter {

	private final ChangeSetComparator csComparator;

	public HgChangeSetSorter() {
		super();
		csComparator = new ChangeSetComparator();
	}

	@Override
	public int category(Object element) {
		if(element instanceof WorkingChangeSet){
			return 0;
		}
		if(element instanceof ChangesetGroup){
			return 1;
		}
		if(element instanceof ChangeSet){
			return 2;
		}
		if(element instanceof IResource){
			return 3;
		}
		return super.category(element);
	}

	public void setConfiguration(ISynchronizePageConfiguration configuration) {
		// not used yet
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1 = category(e1);
		int cat2 = category(e2);

		if (cat1 != cat2) {
			return cat1 - cat2;
		}
		if(e1 instanceof ChangeSet && e2 instanceof ChangeSet){
			ChangeSet cs1 = (ChangeSet) e1;
			ChangeSet cs2 = (ChangeSet) e2;
			if(cs1 instanceof WorkingChangeSet) {
				WorkingChangeSet wcs1 = (WorkingChangeSet) cs1;
				if(wcs1.isDefault()) {
					return -1;
				}
			}
			if(cs2 instanceof WorkingChangeSet) {
				WorkingChangeSet wcs2 = (WorkingChangeSet) cs2;
				if(wcs2.isDefault()) {
					return 1;
				}
			}
			return csComparator.compare(cs1, cs2);
		}
		if(e1 instanceof ChangesetGroup && e2 instanceof ChangesetGroup){
			ChangesetGroup group1 = (ChangesetGroup) e1;
			ChangesetGroup group2 = (ChangesetGroup) e2;
			if(group1.getDirection() == group2.getDirection()){
				return compareByName(viewer, e1, e2);
			}
			if(group1.getDirection() == Direction.LOCAL){
				return -1;
			}
			if(group1.getDirection() == Direction.OUTGOING){
				return 0;
			}
			return 1;
		}
		return compareByName(viewer, e1, e2);
	}


	@SuppressWarnings("unchecked")
	private int compareByName(Viewer viewer, Object e1, Object e2) {
		String name1;
		String name2;

		if (viewer == null || !(viewer instanceof ContentViewer)) {
			name1 = e1.toString();
			name2 = e2.toString();
		} else {
			IBaseLabelProvider prov = ((ContentViewer) viewer)
					.getLabelProvider();
			if (prov instanceof ILabelProvider) {
				ILabelProvider lprov = (ILabelProvider) prov;
				name1 = lprov.getText(e1);
				name2 = lprov.getText(e2);
			} else {
				name1 = e1.toString();
				name2 = e2.toString();
			}
		}
		if (name1 == null) {
			name1 = ""; //$NON-NLS-1$
		}
		if (name2 == null) {
			name2 = ""; //$NON-NLS-1$
		}

		// use the comparator to compare the strings
		return getComparator().compare(name1, name2);
	}

}
