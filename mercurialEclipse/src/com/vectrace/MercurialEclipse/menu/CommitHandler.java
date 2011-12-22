/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.dialogs.CommitDialog;
import com.vectrace.MercurialEclipse.dialogs.CommitDialog.Options;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class CommitHandler extends MultipleResourcesHandler {

	private int result;
	private Options options;

	@Override
	public void run(List<IResource> resources) throws HgException {

		if(resources.isEmpty() && options != null && options.allowEmptyCommit && options.hgRoot != null) {
			CommitDialog commitDialog = new CommitDialog(getShell(), options.hgRoot, resources);
			commitDialog.setOptions(options);
			commitDialog.setBlockOnOpen(true);
			result = commitDialog.open();
		} else {
			Map<HgRoot, List<IResource>> byRoot = ResourceUtils.groupByRoot(resources);
			Set<Entry<HgRoot, List<IResource>>> entrySet = byRoot.entrySet();

			for (Entry<HgRoot, List<IResource>> entry : entrySet) {
				HgRoot hgRoot = entry.getKey();
				if(MercurialStatusCache.getInstance().isMergeInProgress(hgRoot)){
					new CommitMergeHandler().commitMergeWithCommitDialog(hgRoot, getShell());
					return;
				}
				if(options == null){
					options = new Options();
				}
				CommitDialog commitDialog = new CommitDialog(getShell(), hgRoot, entry.getValue());
				commitDialog.setOptions(options);
				commitDialog.setBlockOnOpen(true);
				result = commitDialog.open();
			}
		}
	}

	/**
	 * @param options commit dialog options
	 */
	public void setOptions(Options options) {
		this.options = options;
	}

	/**
	 * @return {@link Window#OK} if the commit dialog was finished with the OK button
	 */
	public int getResult() {
		return result;
	}

}
