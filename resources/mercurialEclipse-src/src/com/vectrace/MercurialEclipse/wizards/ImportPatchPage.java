/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     steeven                    - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.ui.LocationChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.ui.LocationChooser.Location;

/**
 * A wizard page which will allow the user to choose location to export patch.
 */
public class ImportPatchPage extends HgWizardPage implements Listener {

	private LocationChooser locationChooser;

	private Text txtProject;

	private final HgRoot hgRoot;

	public ImportPatchPage(HgRoot hgRoot) {
		super(Messages.getString("ImportPatchWizard.pageName"), Messages //$NON-NLS-1$
				.getString("ImportPatchWizard.pageTitle"), null); // TODO icon //$NON-NLS-1$
		this.hgRoot = hgRoot;
	}

	protected boolean validatePage() {
		String msg = locationChooser.validate();
		if (msg == null && hgRoot == null) {
			msg = Messages.getString("ImportPatchPage.InvalidProject"); // possible? //$NON-NLS-1$
		}
		if (msg == null) {
			setMessage(null);
		}
		setErrorMessage(msg);
		setPageComplete(msg == null);
		return msg == null;
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 2);
		Group group = SWTWidgetHelper.createGroup(composite, Messages
				.getString("ExportPatchWizard.PathLocation"), 2, GridData.FILL_HORIZONTAL); //$NON-NLS-1$

		locationChooser = new LocationChooser(group, false, getDialogSettings());
		locationChooser.addStateListener(this);
		locationChooser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		SWTWidgetHelper.createLabel(composite, Messages
				.getString("ImportPatchPage.ProjectName")); //$NON-NLS-1$
		txtProject = SWTWidgetHelper.createTextField(composite);
		txtProject.setEditable(false);
		if (hgRoot != null) {
			txtProject.setText(hgRoot.getName());
		}

		setControl(composite);
		validatePage();
	}

	public void handleEvent(Event event) {
		validatePage();
	}

	public Location getLocation() {
		return locationChooser.getCheckedLocation();
	}

	@Override
	public boolean finish(IProgressMonitor monitor) {
		locationChooser.saveSettings();
		return super.finish(monitor);
	}
}
