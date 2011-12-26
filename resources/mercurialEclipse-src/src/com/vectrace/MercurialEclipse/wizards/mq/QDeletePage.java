/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards.mq;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.extensions.mq.HgQAppliedClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.Patch;
import com.vectrace.MercurialEclipse.ui.ChangesetTable;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.wizards.HgWizardPage;

/**
 * @author bastian
 *
 */
public class QDeletePage extends HgWizardPage {

	private final boolean showRevSelector;
	private IResource resource;
	private ListViewer patchViewer;
	private ChangesetTable changesetTable;
	private Button revCheckBox;
	private Button keepCheckBox;

	public QDeletePage(String pageName, String title,
			ImageDescriptor titleImage, String description, IResource resource, boolean showRevSelector) {
		super(pageName, title, titleImage, description);
		this.resource = resource;
		this.showRevSelector = showRevSelector;
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 2);
		Group g = SWTWidgetHelper.createGroup(composite,
				Messages.getString("QDeletePage.patchGroup.title")); //$NON-NLS-1$

		IBaseLabelProvider labelProvider = new LabelProvider() {
			@Override
			public String getText(Object element) {
				return element.toString();
			}
		};

		this.patchViewer = SWTWidgetHelper.createListViewer(g, Messages.getString("QDeletePage.patchViewer.title"), 100, //$NON-NLS-1$
				labelProvider);
		populatePatchViewer();

		g = SWTWidgetHelper.createGroup(composite, Messages.getString("QDeletePage.optionGroup.title")); //$NON-NLS-1$
		this.keepCheckBox = SWTWidgetHelper.createCheckBox(g, Messages.getString("QDeletePage.keepCheckBox.title")); //$NON-NLS-1$

		if (showRevSelector) {
			this.revCheckBox = SWTWidgetHelper.createCheckBox(g,
					Messages.getString("QDeletePage.revCheckBox.title")); //$NON-NLS-1$

			SelectionListener revListener = new SelectionListener() {

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}

				public void widgetSelected(SelectionEvent e) {
					changesetTable.setEnabled(revCheckBox.getSelection());
					patchViewer.getControl().setEnabled(!revCheckBox.getSelection());
				}

			};

			revCheckBox.addSelectionListener(revListener);

			GridData gridData = new GridData(GridData.FILL_BOTH);
			gridData.heightHint = 150;
			gridData.minimumHeight = 50;
			this.changesetTable = new ChangesetTable(g, resource.getProject());
			this.changesetTable.setLayoutData(gridData);
			this.changesetTable.setEnabled(false);
		}

		setControl(composite);
	}

	/**
	 *
	 */
	private void populatePatchViewer() {
		try {
			List<Patch> patches = HgQAppliedClient.getUnappliedPatches(resource);
			for (Patch patch : patches) {
				patchViewer.add(patch);
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			setErrorMessage(e.getLocalizedMessage());
		}
	}

	/**
	 * @return the patchViewer
	 */
	public ListViewer getPatchViewer() {
		return patchViewer;
	}

	/**
	 * @return the resource
	 */
	public IResource getResource() {
		return resource;
	}

	/**
	 * @param resource the resource to set
	 */
	public void setResource(IResource resource) {
		this.resource = resource;
	}

	/**
	 * @return the revCheckBox
	 */
	public Button getRevCheckBox() {
		return revCheckBox;
	}

	/**
	 * @return the keepCheckBox
	 */
	public Button getKeepCheckBox() {
		return keepCheckBox;
	}

	public ChangeSet getSelectedChangeset() {
		if (changesetTable == null) {
			return null;
		}

		return changesetTable.getSelection();
	}

}
