/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.File;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author Andrei
 */
public class CreateRepoPage extends ConfigurationWizardMainPage {

	private Button createCheckBox;

	public CreateRepoPage() {
		super(Messages.getString("NewLocationWizard.repoCreationPage.name"), //$NON-NLS-1$
				Messages.getString("NewLocationWizard.repoCreationPage.title"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("wizards/share_wizban.png") //$NON-NLS-1$
		);

		setShowCredentials(true);
		setShowBundleButton(false);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite composite = (Composite) getControl();
		createCheckBox = SWTWidgetHelper.createCheckBox(composite,
			"Init Mercurial repository (creates hg root at given location)");
		createCheckBox.setSelection(true);
		createCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validateFields();
			}
		});
	}

	@Override
	protected boolean validateFields() {
		if(createCheckBox == null){
			return super.validateFields();
		}
		// first check the url of the repository
		String url = getUrlText();

		if (url.length() == 0) {
			setErrorMessage(null);
			createCheckBox.setEnabled(true);
			setAuthCompositeEnabled(false);
			return false;
		}
		File localDirectory = getLocalDirectory(url);
		if(localDirectory != null){
			createCheckBox.setEnabled(true);
			setAuthCompositeEnabled(false);
		} else {
			boolean isSsh = url.startsWith("ssh:");
			createCheckBox.setEnabled(isSsh);
			setAuthCompositeEnabled(true);
		}

		try {
			HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();
			IHgRepositoryLocation repoLocation = repoManager.getRepoLocation(url);
			if(repoManager.getAllRepoLocations().contains(repoLocation)){
				setErrorMessage("Repository location already known!");
				return false;
			}
		} catch (HgException e) {
			// do not report error here
		}
		if(localDirectory != null){
			if(!localDirectory.exists() && !createCheckBox.getSelection()){
				setErrorMessage("Please give a valid url or an existing directory!");
				return false;
			}
			File hgRepo = new File(localDirectory, ".hg");
			if(!hgRepo.isDirectory() && !createCheckBox.getSelection()){
				setErrorMessage("Directory " + localDirectory + " does not contain a valid hg repository!");
				return false;
			}
			IFile fileHandle = ResourceUtils.getFileHandle(new Path(localDirectory.getAbsolutePath()));
			if(fileHandle != null && (fileHandle.exists() || fileHandle.getProject() != null)){
				setErrorMessage("You can not create new repository inside existing project!");
				return false;
			}
		}
		setErrorMessage(null);
		return true;
	}

	/**
	 * @param urlString non null
	 * @return non null file object if the given url can be threated as local directory
	 */
	@Override
	protected File getLocalDirectory(String urlString) {
		if(urlString == null){
			return null;
		}
		urlString = urlString.trim();

		if (urlString.length() == 0 || urlString.contains("http:")
				|| urlString.contains("https:") || urlString.contains("ftp:")
				|| urlString.contains("ssh:")) {
			return null;
		}

		if (urlString.contains("file:")){
			try {
				// Supporting file:// URLs
				URL url = new URL(urlString);
				return new File(url.getPath());
			} catch (Exception e) {
				return null;
			}
		}

		File dir = new File(urlString);
		if (dir.isFile()) {
			return null;
		}
		return dir;
	}

	public File getLocalRepo(){
		return getLocalDirectory(getUrlText());
	}

	public boolean shouldInitRepo(){
		return createCheckBox.getSelection();
	}

	@Override
	protected boolean urlChanged() {
		return super.urlChanged();
	}
}
