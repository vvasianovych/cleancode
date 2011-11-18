/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov 		- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * Exclusive rule for locking access to the resources related to same hg root.
 *
 * @author Andrei
 */
public class HgRootRule implements ISchedulingRule {

	private final HgRoot hgRoot;

	/**
	 * @param hgRoot non null
	 */
	public HgRootRule(HgRoot hgRoot) {
		Assert.isNotNull(hgRoot, "Trying to create HgRootRule without a hgRoot object");
		this.hgRoot = hgRoot;
	}

	public boolean contains(ISchedulingRule rule) {
		if (rule instanceof IResource) {
			IResource resource = (IResource) rule;
			IPath location = resource.getLocation();
			if(location != null) {
				return hgRoot.getIPath().isPrefixOf(location);
			}
		}
		return isConflicting(rule);
	}

	/**
	 * Note: this method (used by Job API to detect deadlocks) should avoid made locking calls to
	 * avoid deadlocks, see issues 13474, 13497
	 */
	public boolean isConflicting(ISchedulingRule rule) {
		if(!(rule instanceof HgRootRule)){
			if(rule instanceof IWorkspaceRoot){
				return false;
			}
			if (rule instanceof IResource) {
				// hasHgRoot() returns cached value, if any (see issue 13474, 13497)
				// if the value is not yet cached, we don't care
				HgRoot resourceRoot = MercurialRootCache.getInstance().hasHgRoot((IResource) rule, false, false);
				if(resourceRoot == null) {
					return false;
				}
				return getHgRoot().equals(resourceRoot);
			}
			return false;
		}
		HgRootRule rootRule = (HgRootRule) rule;
		return getHgRoot().equals(rootRule.getHgRoot());
	}

	public HgRoot getHgRoot() {
		return hgRoot;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("HgRootRule [");
		builder.append("hgRoot=");
		builder.append(hgRoot);
		builder.append("]");
		return builder.toString();
	}

}
