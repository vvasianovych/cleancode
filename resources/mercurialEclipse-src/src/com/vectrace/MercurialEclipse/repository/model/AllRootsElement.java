/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 *     Andrei Loskutov              - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository.model;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.storage.HgRepositoryLocationManager;
import com.vectrace.MercurialEclipse.team.cache.MercurialRootCache;

/**
 * AllRootsElement is the model element for the repositories view.
 * Its children are the array of all known repository roots.
 *
 * Because we extend IAdaptable, we don't need to register this adapter
 */
public class AllRootsElement implements IWorkbenchAdapter, IAdaptable {

	public AllRootsElement() {
		super();
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return this;
		}
		return null;
	}

	/**
	 * Return non null array with all known local and remote repositories
	 * @param o ignored
	 */
	public IHgRepositoryLocation[] getChildren(Object o) {
		HgRepositoryLocationManager repoManager = MercurialEclipsePlugin.getRepoManager();
		Collection<IHgRepositoryLocation> repoLocations = repoManager.getAllRepoLocations();
		Collection<HgRoot> hgRoots = MercurialRootCache.getInstance().getKnownHgRoots();

		// remove local repos which are known as hg roots
		for (IHgRepositoryLocation hgRoot : hgRoots) {
			Iterator<IHgRepositoryLocation> iterator = repoLocations.iterator();
			while (iterator.hasNext()) {
				IHgRepositoryLocation repoLoc = iterator.next();
				if(repoLoc.equals(hgRoot)){
					iterator.remove();
				}
			}
		}

		// remove roots which have default repos set
		for (IHgRepositoryLocation repo : repoLocations) {
			Iterator<HgRoot> iterator = hgRoots.iterator();
			while (iterator.hasNext()) {
				HgRoot hgRoot = iterator.next();
				if(repo.equals(repoManager.getDefaultRepoLocation(hgRoot))){
					iterator.remove();
				}
			}
		}

		IHgRepositoryLocation [] result = new IHgRepositoryLocation[repoLocations.size() + hgRoots.size()];
		System.arraycopy(repoLocations.toArray(), 0, result, 0, repoLocations.size());
		System.arraycopy(hgRoots.toArray(), 0, result, repoLocations.size(), hgRoots.size());
		return result;
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	public String getLabel(Object o) {
		return null;
	}

	public Object getParent(Object o) {
		return null;
	}
}

