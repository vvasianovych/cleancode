/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian Doetsch	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient;
import com.vectrace.MercurialEclipse.commands.extensions.HgTransplantClient.TransplantOptions;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.operations.TransplantOperation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;

/**
 * @author bastian
 *
 */
public class TransplantWizard extends HgOperationWizard {

	private final HgRoot hgRoot;

	public TransplantWizard(HgRoot hgRoot) {
		super(Messages.getString("TransplantWizard.title")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
		this.hgRoot = hgRoot;
	}

	@Override
	public void addPages() {
		super.addPages();
		TransplantPage transplantPage = new TransplantPage(Messages.getString("TransplantWizard.transplantPage.name"), //$NON-NLS-1$
				Messages.getString("TransplantWizard.transplantPage.title"), null, hgRoot); //$NON-NLS-1$
		initPage(Messages.getString("TransplantWizard.transplantPage.description"), //$NON-NLS-1$
				transplantPage);
		transplantPage.setShowCredentials(true);
		page = transplantPage;
		addPage(page);

		TransplantOptionsPage optionsPage = new TransplantOptionsPage(
				Messages.getString("TransplantWizard.optionsPage.name"),
				Messages.getString("TransplantWizard.optionsPage.title"), null, hgRoot); //$NON-NLS-1$
		initPage(Messages.getString("TransplantWizard.optionsPage.description"), optionsPage); //$NON-NLS-1$
		addPage(optionsPage);
	}

	/**
	 * @throws HgException
	 * @see com.vectrace.MercurialEclipse.wizards.HgOperationWizard#initOperation()
	 */
	@Override
	protected HgOperation initOperation() throws HgException {

		page.finish(new NullProgressMonitor());
		HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();
		TransplantOptions options = createOptions();

		IHgRepositoryLocation repo;
		if (options.branch) {
			repo = null;
		} else {
			repo = repoManager.fromProperties(hgRoot, page.getProperties());
		}

		return new TransplantOperation(getContainer(), hgRoot, options, repo);
	}

	@Override
	protected boolean operationSucceeded(HgOperation operation) throws HgException {
		TransplantOperation top = (TransplantOperation) operation;

		// It appears good. Stash the repo location.
		if (top.getRepo() != null) {
			MercurialEclipsePlugin.getRepoManager().addRepoLocation(hgRoot, top.getRepo());
		}

		return super.operationSucceeded(operation);
	}

	private HgTransplantClient.TransplantOptions createOptions() {
		HgTransplantClient.TransplantOptions options = new HgTransplantClient.TransplantOptions();
		TransplantPage transplantPage = (TransplantPage) page;
		TransplantOptionsPage optionsPage = (TransplantOptionsPage) page.getNextPage();

		options.all = transplantPage.isAll();
		options.branch = transplantPage.isBranch();
		if (options.branch && Branch.isDefault(transplantPage.getBranchName())) {
			// branch name, as command parameter is default if empty
			options.branchName = Branch.DEFAULT;
		} else {
			options.branchName = transplantPage.getBranchName();
		}
		options.filter = optionsPage.getFilter();
		options.filterChangesets = optionsPage.isFilterChangesets();
		options.merge = optionsPage.isMerge();
		options.mergeNodeId = optionsPage.getMergeNodeId();
		options.nodes = transplantPage.getSelectedChangesets();
		options.prune = optionsPage.isPrune();
		options.pruneNodeId = optionsPage.getPruneNodeId();
		return options;
	}
}
