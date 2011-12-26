/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 	   Bastian	implementation
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (Intland)   - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.util.Set;

import org.eclipse.team.core.history.ITag;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet.ShallowChangeSet;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * @author Bastian
 */
public class Tag implements ITag, Comparable<Tag> {
	private static final String TIP = HgRevision.TIP.getChangeset();


	/** name of the tag, unique in the repository */
	private final String name;
	private final int revision;
	private final String globalId;
	private final boolean local;

	private ChangeSet changeSet;


	private final HgRoot hgRoot;

	public Tag(HgRoot hgRoot, String name, int revision, String globalId, boolean local) {
		super();
		this.hgRoot = hgRoot;
		this.name = name;
		this.revision = revision;
		this.globalId = globalId;
		this.local = local;
	}

	public Tag(HgRoot hgRoot, String name, ChangeSet changeSet, boolean local) {
		this(hgRoot, name, changeSet.getChangesetIndex(), changeSet.getChangeset(), local);
		this.changeSet = changeSet;
	}

	/**
	 * <b>Note: this method may trigger Mercurial call</b> to retrieve the changeset info, if
	 * the tag was created with {@link #Tag(HgRoot, String, int, String, boolean)} constructor
	 * and the local changeset cache doesn't contain the tag version!
	 * @return never null
	 */
	public ChangeSet getChangeSet() {
		if(changeSet != null) {
			return changeSet;
		}
		LocalChangesetCache cache = LocalChangesetCache.getInstance();
		try {
			changeSet = cache.getOrFetchChangeSetById(hgRoot,
					revision + ":" + globalId);
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		if(changeSet == null) {
			try {
				Set<ChangeSet> revisions = cache.fetchRevisions(hgRoot, true, 1, revision, false);
				if(revisions.size() == 1) {
					changeSet = revisions.iterator().next();
				}
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
			if(changeSet == null) {
				changeSet = new ShallowChangeSet(revision, globalId, hgRoot);
			}
		}
		return changeSet;
	}

	public String getName() {
		return name;
	}

	public int getRevision() {
		return revision;
	}

	public String getGlobalId() {
		return globalId;
	}

	public boolean isLocal() {
		return local;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Tag other = (Tag) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return name + " [" + revision +  ':' + globalId + ']';
	}

	public int compareTo(Tag tag) {
		/* "tip" must be always the first in the collection */
		if (tag == null || name == null || isTip()) {
			return -1;
		}

		if (tag.isTip()) {
			return 1;
		}

		int cmp = tag.getRevision() - revision;
		if(cmp != 0){
			// sort by revision first
			return cmp;
		}

		// sort by name
		cmp = name.compareToIgnoreCase(tag.getName());
		if (cmp == 0) {
			// Check it case sensitive
			cmp = name.compareTo(tag.getName());
		}
		return cmp;
	}

	public boolean isTip(){
		return TIP.equals(name);
	}
}
