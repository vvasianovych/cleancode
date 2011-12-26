/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     StefanC - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public final class CommitResourceLabelProvider extends LabelProvider  {

	private final ILabelProvider workbenchLabelProvider;

	public CommitResourceLabelProvider() {
		super();
		workbenchLabelProvider = WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();
		workbenchLabelProvider.addListener(new ILabelProviderListener() {
			public void labelProviderChanged(LabelProviderChangedEvent event) {
				fireLabelProviderChanged(event);
			}
		});
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	@Override
	public Image getImage(Object element) {
		if (!(element instanceof CommitResource)) {
			return null;
		}
		IResource resource = ((CommitResource) element).getResource();
		return workbenchLabelProvider.getImage(resource);
	}

	@Override
	public String getText(Object element) {
		if (!(element instanceof CommitResource)) {
			return null;
		}
		return ((CommitResource) element).getPath().toString();
	}

	@Override
	public void dispose() {
		workbenchLabelProvider.dispose();
		super.dispose();
	}

}