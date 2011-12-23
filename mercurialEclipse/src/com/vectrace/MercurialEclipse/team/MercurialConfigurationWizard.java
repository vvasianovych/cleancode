/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - some updates
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Bastian Doetsch           - make map operation asynchronous
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/

/**
 * Mercurial create Wizard
 *
 * This wizard will will create a mercurial repository if there is no one yet, or simply
 * map the project with the Mercurial team provider
 *
 * It will follow the dirictory chain to the bottom to se is
 * there is a .hg directory someware, is so it will suggest that you use it
 * instead of creating a new repository.
 */
package com.vectrace.MercurialEclipse.team;


import java.io.File;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.ui.IConfigurationWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgRootClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.InitOperation;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.wizards.ConfigurationWizardMainPage;
import com.vectrace.MercurialEclipse.wizards.NewLocationWizard;

/**
 * @author zingo
 */
public class MercurialConfigurationWizard extends Wizard implements IConfigurationWizard {

	private class NewWizardPage extends WizardPage implements SelectionListener {
		private Button restoreProjectDirButton;
		private Button restoreExistingDirButton;
		private final boolean newMercurialRoot;
		private Link link;

		NewWizardPage(boolean newMercurialRoot) {
			super(WizardPage.class.getName());
			this.newMercurialRoot = newMercurialRoot;
			if (newMercurialRoot) {
				setTitle(Messages.getString("MercurialConfigurationWizard.titleNew")); //$NON-NLS-1$
				setDescription(Messages.getString("MercurialConfigurationWizard.descriptionNew")); //$NON-NLS-1$
			} else {
				setTitle(Messages.getString("MercurialConfigurationWizard.titleExisting")); //$NON-NLS-1$
				setDescription("An existing Mercurial repository was found in the parent directory. "
						+ "Please choose which repository should be connected with the project.");
			}
			setPageComplete(true);
		}

		public void createControl(final Composite parent) {

			Composite mainControl = new Composite(parent, SWT.NONE);
			mainControl.setLayout(new GridLayout(2, false));

			Label label = new Label(mainControl, SWT.CENTER);
			label.setText(Messages.getString("MercurialConfigurationWizard.selectDirectory")); //$NON-NLS-1$

			directoryText = new Text(mainControl, SWT.BORDER);
			directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			directoryText.setText(hgPath);
			directoryText.setEditable(false);

			if (!newMercurialRoot) {
				Group buttonsPanel = new Group(mainControl, SWT.NONE);
				buttonsPanel.setText("Choose the Mercurial repository to connect the project with");
				GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
				buttonsPanel.setLayout(new GridLayout(1, false));
				gridData.horizontalSpan = 2;
				buttonsPanel.setLayoutData(gridData);

				restoreExistingDirButton = new Button(buttonsPanel, SWT.CENTER | SWT.RADIO);
				restoreExistingDirButton.setText(Messages.getString("MercurialConfigurationWizard.useExistingHgDir")); //$NON-NLS-1$

				restoreProjectDirButton = new Button(buttonsPanel, SWT.CENTER | SWT.RADIO);
				restoreProjectDirButton.setText(Messages.getString("MercurialConfigurationWizard.useProjectRoot")); //$NON-NLS-1$

				restoreExistingDirButton.setSelection(true);
				restoreProjectDirButton.addSelectionListener(this);
				restoreExistingDirButton.addSelectionListener(this);
			}

			link = new Link(mainControl, SWT.NONE);
			GridData gridData = new GridData();
			gridData.horizontalSpan = 2;
			link.setLayoutData(gridData);
			link.setText("<a>Create new Mercurial repository at another location...</a>");
			link.addSelectionListener(this);

			setControl(mainControl);
			setPageComplete(true);
		}

		public void widgetSelected(SelectionEvent e) {
			if (e.widget == link){
				NewLocationWizard wizard = new NewLocationWizard();
				Properties properties = new Properties();
				properties.setProperty(ConfigurationWizardMainPage.PROP_URL, hgPath);
				wizard.setProperties(properties);
				getShell().close();
				WizardDialog dialog = new WizardDialog(MercurialEclipsePlugin.getActiveShell(), wizard);
				dialog.open();
			} else if (e.widget == restoreProjectDirButton){
				hgPath = ResourceUtils.getPath(project).toOSString();
				directoryText.setText(hgPath);
			}else if (e.widget == restoreExistingDirButton){
				hgPath = foundhgPath.getAbsolutePath();
				directoryText.setText(hgPath);
			}
		}

		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}
	}

	private IProject project;
	private String hgPath;
	private Text directoryText;
	private NewWizardPage page;
	private HgRoot foundhgPath;

	public MercurialConfigurationWizard() {
		setWindowTitle(Messages.getString("MercurialConfigurationWizard.wizardTitle")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		foundhgPath = MercurialTeamProvider.hasHgRoot(project);
		IPath path = ResourceUtils.getPath(project);
		if(foundhgPath == null) {
			try {
				foundhgPath = HgRootClient.getHgRoot(path.toFile());
			} catch (HgException e) {
				// ignore, we just looking for a *possible* root
			}
		}
		if (foundhgPath == null || foundhgPath.getIPath().equals(path)) {
			hgPath = path.toOSString();
			page = new NewWizardPage(true);
		} else {
			hgPath = foundhgPath.getAbsolutePath();
			page = new NewWizardPage(false);
		}
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		if (directoryText != null) {
			hgPath = directoryText.getText();
		}
		try {
			getContainer().run(true, true,
					new InitOperation(getContainer(), project, new File(hgPath)));
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			page.setErrorMessage(e.getMessage());
			return false;
		}
		return true;
	}

	public void init(IWorkbench workbench, IProject proj) {
		this.project = proj;
		if (!MercurialUtilities.isHgExecutableCallable()) {
			MercurialUtilities.configureHgExecutable();
		}
	}

}
