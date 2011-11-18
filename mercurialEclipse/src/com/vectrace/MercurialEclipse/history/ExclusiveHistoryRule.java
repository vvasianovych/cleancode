/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import com.vectrace.MercurialEclipse.team.cache.HgRootRule;

/**
 * A rule which disallows parallel execution of jobs using it
 */
public final class ExclusiveHistoryRule implements ISchedulingRule {
	public boolean isConflicting(ISchedulingRule rule) {
		return contains(rule);
	}

	public boolean contains(ISchedulingRule rule) {
		return rule instanceof ExclusiveHistoryRule || rule instanceof IResource || rule instanceof HgRootRule;
	}
}