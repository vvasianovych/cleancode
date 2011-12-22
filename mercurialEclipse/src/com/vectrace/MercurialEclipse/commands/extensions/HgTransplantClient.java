/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch           - implementation
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands.extensions;

import java.net.URI;
import java.util.SortedSet;

import com.vectrace.MercurialEclipse.commands.AbstractShellCommand;
import com.vectrace.MercurialEclipse.commands.HgCommand;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

public final class HgTransplantClient {

	public static class TransplantOptions {
		public boolean all;
		public boolean branch;
		public boolean continueLastTransplant;
		public boolean filterChangesets;
		public boolean merge;
		public boolean prune;
		public String branchName;
		public String filter;
		public String mergeNodeId;
		public String pruneNodeId;
		/** changesets sorted in the ascending revision order */
		public SortedSet<ChangeSet> nodes;

		/**
		 * @return Human readable description
		 */
		public String getDescription() {
			if (continueLastTransplant) {
				return "Continuing transplant";
			} else if (all) {
				return "Transplanting all revisions from source";
			} else if (nodes == null) {
				return "Transplanting";
			} else if (nodes.size() > 1) {
				return "Transplanting " + nodes.size() + " revisions";
			}

			return "Transplanting revision " + nodes.first().getChangeset();
		}
	}

	private HgTransplantClient() {
		// hide constructor of utility class.
	}

	/**
	 * Cherrypicks given ChangeSets from repository or branch.
	 */
	public static String transplant(HgRoot hgRoot,
			IHgRepositoryLocation repo, TransplantOptions options) throws HgException {

		AbstractShellCommand command = new HgCommand("transplant", options.getDescription(), hgRoot, false); //$NON-NLS-1$
		command.setUsePreferenceTimeout(MercurialPreferenceConstants.PULL_TIMEOUT);
		command.addOptions("--config", "extensions.hgext.transplant="); //$NON-NLS-1$ //$NON-NLS-2$
		command.addOptions("--log"); //$NON-NLS-1$
		if (options.continueLastTransplant) {
			command.addOptions("--continue"); //$NON-NLS-1$
		} else {
			if (options.branch) {
				command.addOptions("--branch"); //$NON-NLS-1$
				command.addOptions(options.branchName);
				if (options.all) {
					command.addOptions("--all"); //$NON-NLS-1$
				} else {
					// the exact revision will be specified below via changeset id
				}
			} else {
				command.addOptions("--source"); //$NON-NLS-1$
				URI uri = repo.getUri();
				if (uri != null) {
					command.addOptions(uri.toASCIIString());
				} else {
					command.addOptions(repo.getLocation());
				}
			}

			if (options.prune) {
				command.addOptions("--prune"); //$NON-NLS-1$
				command.addOptions(options.pruneNodeId);
			}

			if (options.merge) {
				command.addOptions("--merge"); //$NON-NLS-1$
				command.addOptions(options.mergeNodeId);
			}

			if (!options.all && options.nodes != null && options.nodes.size() > 0) {
				for (ChangeSet node : options.nodes) {
					command.addOptions(node.getChangeset());
				}
			}

			if (options.filterChangesets) {
				command.addOptions("--filter", options.filter); //$NON-NLS-1$
			}
		}
		return new String(command.executeToBytes());
	}
}
