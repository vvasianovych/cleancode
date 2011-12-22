/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;


import java.util.Set;

import org.eclipse.jface.resource.ImageDescriptor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgPathsClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;

/**
 * @author bastian
 *
 */
public class PushRepoPage extends PushPullPage {

	public PushRepoPage(String pageName, String title,
			ImageDescriptor titleImage, HgRoot hgRoot) {
		super(hgRoot, pageName, title, titleImage);
		setShowRevisionTable(false);
	}

	@Override
	public boolean canFlipToNextPage() {
		try {
			String urlText = getUrlText();
			if (urlText != null && urlText.length() > 0) {
				OutgoingPage outgoingPage = (OutgoingPage) getNextPage();
				outgoingPage.setHgRoot(getHgRoot());
				IHgRepositoryLocation loc = MercurialEclipsePlugin
						.getRepoManager().getRepoLocation(urlText,
								getUserText(),
								getPasswordText());
				outgoingPage.setLocation(loc);
				outgoingPage.setSvn(isSvnSelected());
				outgoingPage.setForce(isForceSelected());
				setErrorMessage(null);
				return isPageComplete()	&& (getWizard().getNextPage(this) != null);
			}
		} catch (HgException e) {
			setErrorMessage(e.getLocalizedMessage());
		}
		return false;
	}

	@Override
	protected IHgRepositoryLocation getRepoFromRoot(){
		HgRepositoryLocationManager mgr = MercurialEclipsePlugin.getRepoManager();
		IHgRepositoryLocation defaultLocation = mgr.getDefaultRepoLocation(getHgRoot());
		Set<IHgRepositoryLocation> repos = mgr.getAllRepoLocations(getHgRoot());
		if (defaultLocation == null) {
			for (IHgRepositoryLocation repo : repos) {
				if (HgPathsClient.DEFAULT_PUSH.equals(repo.getLogicalName())
						|| HgPathsClient.DEFAULT.equals(repo.getLogicalName())) {
					defaultLocation = repo;
					break;
				}
			}
		}
		return defaultLocation;
	}
}
