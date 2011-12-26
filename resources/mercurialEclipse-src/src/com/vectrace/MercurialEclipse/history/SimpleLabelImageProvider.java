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
package com.vectrace.MercurialEclipse.history;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.model.FileStatus;

public final class SimpleLabelImageProvider extends LabelProvider {

	private final Image fileImg;

	public SimpleLabelImageProvider() {
		fileImg = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
	}

	@Override
	public Image getImage(Object element) {
		return fileImg;
	}

	@Override
	public String getText(Object element) {
		if (!(element instanceof FileStatus)) {
			return null;
		}

		FileStatus status = (FileStatus) element;
		if(status.isCopied()){
			return " " + status.getRootRelativePath().toOSString() + " (" + status.getRootRelativeCopySourcePath().toOSString() + ")"; //$NON-NLS-1$
		}
		return " " + status.getRootRelativePath().toOSString(); //$NON-NLS-1$
	}
}