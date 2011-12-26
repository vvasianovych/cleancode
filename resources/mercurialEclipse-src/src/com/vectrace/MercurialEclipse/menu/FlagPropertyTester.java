/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre            - implementation
 *     Bastian Doetsch         - bug fixes
 *     Andrei Loskutov         - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.menu;

import static com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache.*;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.Bits;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class FlagPropertyTester extends org.eclipse.core.expressions.PropertyTester {

	public static final String PROPERTY_STATUS = "status"; //$NON-NLS-1$
	public static final String PROPERTY_ROOT = "root"; //$NON-NLS-1$
	public static final String PROPERTY_DEF_REPO = "hasDefaultRepo"; //$NON-NLS-1$

	@SuppressWarnings({ "serial", "boxing" })
	private static final Map<Object, Integer> BIT_MAP = Collections.unmodifiableMap(
		new HashMap<Object, Integer>() {
		{
			put("added", BIT_ADDED); //$NON-NLS-1$
			put("clean", BIT_CLEAN); //$NON-NLS-1$
			put("deleted", BIT_MISSING); //$NON-NLS-1$
			put("ignore", BIT_IGNORE); //$NON-NLS-1$
			put("modified", BIT_MODIFIED); //$NON-NLS-1$
			put("removed", BIT_REMOVED); //$NON-NLS-1$
			put("unknown", BIT_UNKNOWN); //$NON-NLS-1$
		}
	});

	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if(PROPERTY_STATUS.equals(property)) {
			try {
				IResource res = (IResource) receiver;
				int test = 0;
				for(Object arg: args) {
					Integer statusBit = BIT_MAP.get(arg);
					if(statusBit == null){
						String message = "Could not test status " + property + " on "  //$NON-NLS-1$ //$NON-NLS-2$
							+ receiver + " for argument: " + arg; //$NON-NLS-1$
						MercurialEclipsePlugin.logWarning(message, new IllegalArgumentException(message));
						continue;
					}
					test |= statusBit.intValue();
				}
				MercurialStatusCache cache = MercurialStatusCache.getInstance();
				Integer status = cache.getStatus(res);
				if (status != null) {
					test &= status.intValue();
					return test != 0;
				} else if(Bits.contains(test, MercurialStatusCache.BIT_UNKNOWN | MercurialStatusCache.BIT_IGNORE)) {
					// ignored or unknown files may be not yet tracked by cache, so the state may be null
					// we assume it is ignored or unknown if the project state is known
					return !(res instanceof IProject) &&  cache.isStatusKnown(res.getProject());
				}
			} catch (Exception e) {
				MercurialEclipsePlugin.logWarning("Could not test status " + property + " on " + receiver, e); //$NON-NLS-1$ //$NON-NLS-2$
				return false;
			}
		} else if(PROPERTY_ROOT.equals(property) && args[0] != null) {
			IResource res = (IResource) receiver;
			IPath location = ResourceUtils.getPath(res);
			if(location.isEmpty()) {
				return false;
			}
			HgRoot hgRoot = MercurialTeamProvider.getHgRoot(res);
			if(hgRoot == null){
				return false;
			}
			File file = location.toFile();
			boolean bool = Boolean.valueOf(args[0].toString()).booleanValue();
			if(bool) {
				return hgRoot.equals(file);
			}
			return !hgRoot.equals(file);
		}
		else if(PROPERTY_DEF_REPO.equals(property) && args[0] != null) {
				IResource res = (IResource) receiver;
				IPath location = ResourceUtils.getPath(res);
				if(location.isEmpty()) {
					return false;
				}
				HgRoot hgRoot = MercurialTeamProvider.getHgRoot(res);
				if(hgRoot == null){
					return false;
				}
				IHgRepositoryLocation repoLocation = MercurialEclipsePlugin.getRepoManager().getDefaultRepoLocation(hgRoot);
				boolean bool = Boolean.valueOf(args[0].toString()).booleanValue();
				if(bool) {
					return repoLocation != null;
				}
				return repoLocation == null;
			}
		return false;
	}

}
