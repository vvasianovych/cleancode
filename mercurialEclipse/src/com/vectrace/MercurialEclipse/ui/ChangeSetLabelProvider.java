package com.vectrace.MercurialEclipse.ui;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.vectrace.MercurialEclipse.model.ChangeSet;
/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian       implementation
 * Philip Graf   bug fix
 *******************************************************************************/
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;

public class ChangeSetLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		ChangeSet rev = (ChangeSet) element;
		switch(columnIndex) {
			case 0:
				return Integer.toString(rev.getChangesetIndex());
			case 1:
				return rev.getChangeset();
			case 2:
				return rev.getDateString();
			case 3:
				return rev.getUser();
			case 4:
				return rev.getBranch();
			case 5:
				return ChangeSetUtils.getPrintableTagsString(rev);
			case 6:
				return rev.getSummary();
		}
		return null;
	}
}
