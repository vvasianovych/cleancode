/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 * 		Andrei Loskutov 	- bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.operations.BundleOperation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * @author Bastian
 */
public class BundleRepoWizard extends HgWizard implements IExportWizard {
	private HgRoot root;
	private IStructuredSelection selection;
	@SuppressWarnings("hiding")
	private BundleRepoPage page;
	private OutgoingPage outgoingPage;

	public BundleRepoWizard() {
		super("Export Mercurial Repository as Bundle");
	}

	public void init(IWorkbench workbench, IStructuredSelection s) {
		this.selection = s;
		if (this.selection.isEmpty()) {
			return;
		}
		PlatformObject po = (PlatformObject) selection.getFirstElement();
		IResource res = (IResource) po.getAdapter(IResource.class);
		try {
			root = MercurialTeamProvider.getHgRoot(res);
			if (root != null) {
				this.page = new BundleRepoPage("bundleRepoPage",
						"Export Mercurial Repository as Bundle", null, root);
				initPage(page.getDescription(), page);
				addPage(page);
				outgoingPage = new OutgoingPage("outgoingPage");
				initPage(outgoingPage.getDescription(), outgoingPage);
				addPage(outgoingPage);
			} else {
				throw new HgException(
						"Could not find a Mercurial repository for export.");
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
			MercurialEclipsePlugin.showError(e);
		}
	}

	@Override
	public boolean performFinish() {
		// finish work in each page
		page.finish(new NullProgressMonitor());
		outgoingPage.finish(new NullProgressMonitor());

		String bundleFile = page.getBundleFile();

		// only use a target rev if checkbox was selected
		ChangeSet cs = outgoingPage.isRevisionSelected() ? outgoingPage
				.getRevision()
				: null;

		// base will be null if nothing was selected or checkbox is not selected
		ChangeSet base = page.getBaseRevision();

		// can be null or empty
		String repo = page.getUrlText();

		// create operation
		BundleOperation op = new BundleOperation(getContainer(), root, cs,
				base, bundleFile, repo);
		try {
			// and run it...
			getContainer().run(true, true, op);
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			page.setErrorMessage(e.getLocalizedMessage());
			outgoingPage.setErrorMessage(e.getLocalizedMessage());
		}
		return super.performFinish();
	}
}
