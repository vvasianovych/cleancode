/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov           - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import org.eclipse.core.resources.IResource;

import com.vectrace.MercurialEclipse.model.Branch;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

/**
 * A key to identify remote cache. The key is direction-insensitive, e.g. it can be
 * used in both outgoing/incoming cache
 * @author Andrei
 */
public class RemoteKey {

	private final HgRoot root;
	private final IHgRepositoryLocation repo;

	/**
	 * The branch name, or null to indicate all branches.
	 */
	private final String branch;

	/**
	 * True if unrelated compare is not an error
	 */
	private final boolean allowUnrelated;

	/**
	 * @param branch can be null (means all branches)
	 */
	public RemoteKey(HgRoot root, IHgRepositoryLocation repo, String branch) {
		this(root, repo, branch, false);
	}

	public RemoteKey(HgRoot root, IHgRepositoryLocation repo, String branch, boolean force) {
		this.root = root;
		this.repo = repo;
		this.branch = branch != null && Branch.isDefault(branch)? Branch.DEFAULT : branch;
		this.allowUnrelated = force;
	}

	public static RemoteKey create(IResource res, IHgRepositoryLocation repo, String branch){
		HgRoot hgRoot = MercurialTeamProvider.getHgRoot(res);
		if(hgRoot != null) {
			return new RemoteKey(hgRoot, repo, branch);
		}
		return new RemoteKey(null, repo, Branch.DEFAULT);
	}

	public IHgRepositoryLocation getRepo() {
		return repo;
	}

	public HgRoot getRoot() {
		return root;
	}

	public boolean isAllowUnrelated() {
		return allowUnrelated;
	}

	/**
	 * Can be null (means all branches)
	 */
	public String getBranch() {
		return branch;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((repo == null) ? 0 : repo.hashCode());
		result = prime * result + ((root == null) ? 0 : root.hashCode());
		result = prime * result + ((branch == null) ? 0 : branch.hashCode());
		result = prime * result + (allowUnrelated ? 0 : 73);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RemoteKey)) {
			return false;
		}
		RemoteKey other = (RemoteKey) obj;
		if (repo == null) {
			if (other.repo != null) {
				return false;
			}
		} else if (!repo.equals(other.repo) || allowUnrelated != other.allowUnrelated) {
			return false;
		}
		if (root == null) {
			if (other.root != null) {
				return false;
			}
		} else if (!root.equals(other.root)) {
			return false;
		}
		return Branch.same(branch, other.branch);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RemoteKey [");
		if (branch != null) {
			builder.append("branch=");
			builder.append(branch);
			builder.append(", ");
		}
		if (repo != null) {
			builder.append("repo=");
			builder.append(repo);
			builder.append(", ");
		}
		if (root != null) {
			builder.append("root=");
			builder.append(root);
			builder.append(", ");
		}

		builder.append("force=");
		builder.append(allowUnrelated);

		builder.append("]");
		return builder.toString();
	}
}
