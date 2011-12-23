/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre - implementation
 *     Bastian Doetsch - javadocs and new qualified name MERGE_COMMIT_OFFERED
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.QualifiedName;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.menu.CommitMergeHandler;
import com.vectrace.MercurialEclipse.views.MergeView;

/**
 * Contains the name of the properties set on IResources.
 *
 * @see IResource#setPersistentProperty(QualifiedName, String)
 * @see IResource#setSessionProperty(QualifiedName, Object)
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public final class ResourceProperties {

	private ResourceProperties() {
		// hide constructor of utility class
	}

	/**
	 * Qualified name for a session property on a project that signifies that
	 * the commit dialog has already been shown by the merge view for either
	 * a merge or rebase.
	 *
	 * @see MergeView
	 * @see CommitMergeHandler
	 */
	public static final QualifiedName MERGE_COMMIT_OFFERED = new QualifiedName(
			MercurialEclipsePlugin.ID, MergeView.ID + ".commitOffered"); //$NON-NLS-1$

	/**
	 * Qualified name for a workspace session property that signifies whether to
	 * use Imerge extension or hg resolve for merging. Makes MercurialEclipse
	 * check exactly once for hg resolve and remember it until Eclipse is
	 * restarted.
	 */
	public static final QualifiedName RESOLVE_AVAILABLE = new QualifiedName(
			MercurialEclipsePlugin.ID, MergeView.ID + ".useResolve"); //$NON-NLS-1$

	/**
	 * Qualified name for a workspace session property that signifies whether
	 * rebase is available.
	 */
	public static final QualifiedName REBASE_AVAILABLE = new QualifiedName(
			MercurialEclipsePlugin.ID, "hgext.rebase="); //$NON-NLS-1$

	/**
	 * Qualified name for a project session property that stores the canonical
	 * path of a hg root. To create a {@link java.io.File} use the constructor
	 * new File(project.getPersistentProperty(ResourceProperties.HG_ROOT))
	 */
	public static final QualifiedName HG_ROOT = new QualifiedName(
			MercurialEclipsePlugin.ID, "hgRoot"); //$NON-NLS-1$

	/**
	 * Qualified name for a workspace session property that signifies whether
	 * the bookmarks extension is available.
	 */
	public static final QualifiedName EXT_BOOKMARKS_AVAILABLE = new QualifiedName(
			MercurialEclipsePlugin.ID, "hgext.bookmarks="); //$NON-NLS-1$

	/**
	 * Qualified name for a workspace session property that signifies whether
	 * the forest extension is available.
	 */
	public static final QualifiedName EXT_FOREST_AVAILABLE = new QualifiedName(
			MercurialEclipsePlugin.ID, "hgforest"); //$NON-NLS-1$

	/**
	 * Qualified name for a workspace session property that signifies whether
	 * the HgSubversion extension is available.
	 */
	public static final QualifiedName EXT_HGSUBVERSION_AVAILABLE = new QualifiedName(
			MercurialEclipsePlugin.ID, "hgsubversion"); //$NON-NLS-1$

	/**
	 * Qualified name for a workspace session property that signifies whether
	 * the HgAttic extension is available.
	 */
	public static final QualifiedName EXT_HGATTIC_AVAILABLE = new QualifiedName(
			MercurialEclipsePlugin.ID, "hgattic"); //$NON-NLS-1$

}
