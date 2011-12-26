/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.swt.graphics.Image;

import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;

/**
 * @author bastian
 *
 */
public class MercurialTextSearchTableLabelProvider extends MercurialTextSearchTreeLabelProvider {

	/**
	 * @param page
	 * @param orderFlag
	 */
	public MercurialTextSearchTableLabelProvider(AbstractTextSearchViewPage page, int orderFlag) {
		super(page, orderFlag);
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof MercurialRevisionStorage) {
			MercurialRevisionStorage mrs = (MercurialRevisionStorage) element;
			String csInfo = getCsInfoString(mrs);
			return new StyledString(csInfo + mrs.getResource().getFullPath().toOSString());
		}

		return super.getStyledText(element);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof MercurialRevisionStorage) {
			MercurialRevisionStorage mrs = (MercurialRevisionStorage) element;
			return super.getResourceImage(mrs.getResource());
		}
		return super.getImage(element);
	}

}
