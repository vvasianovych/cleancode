/*******************************************************************************
 * Copyright (c) 2007-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Charles O'Farrell         - HgRevision
 *     Bastian Doetsch			 - some more info fields
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (Intland)   - bug fixes
 *     Adam Berkes (Intland)     - bug fixes
 *     Philip Graf               - bug fix
 *******************************************************************************/
package com.vectrace.MercurialEclipse.model;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.subscribers.CheckedInChangeSet;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.FileStatus.Action;
import com.vectrace.MercurialEclipse.properties.DoNotDisplayMe;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.utils.StringUtils;

@SuppressWarnings("restriction")
public class ChangeSet extends CheckedInChangeSet implements Comparable<ChangeSet> {

	private static final List<FileStatus> EMPTY_STATUS =
		Collections.unmodifiableList(new ArrayList<FileStatus>());
	private static final Tag[] EMPTY_TAGS = new Tag[0];
	private final IFile[] EMPTY_FILES = new IFile[0];
	private static final SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm Z");

	private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm");

	public static final Date UNKNOWN_DATE = new Date(0);

	public static enum Direction {
		INCOMING, OUTGOING, LOCAL;
	}

	private final HgRevision revision;
	private final int changesetIndex;
	private final String changeset;
	private final String branch;
	private final String user;
	private final String date;
	private String tagsStr;
	private List<FileStatus> changedFiles;
	private String comment;
	private String nodeShort;
	private String[] parents;
	private Date realDate;
	File bundleFile;
	private IHgRepositoryLocation repository;
	Direction direction;
	private final HgRoot hgRoot;
	Set<IFile> files;
	private Tag[] tags;

	/**
	 * A "dummy" changeset containing no additional information except given data
	 */
	public static class ShallowChangeSet extends ChangeSet {

		/**
		 * Creates a shallow changeset containing only provided data
		 * @param changesetIndex
		 * @param changeSet non null
		 * @param root non null
		 */
		public ShallowChangeSet(int changesetIndex, String changeSet, HgRoot root) {
			super(changesetIndex, changeSet, null, null, null, null, "", null, root);
		}
	}

	/**
	 * A more or less dummy changeset containing only index and global id. Such changeset is useful
	 * and can be constructed from the other changesets "parent" ids
	 */
	public static class ParentChangeSet extends ShallowChangeSet {

		/**
		 * @param indexAndId
		 *            a semicolon separated index:id pair
		 * @param child
		 *            this changeset's child from which we are constructing the parent
		 */
		public ParentChangeSet(String indexAndId, ChangeSet child) {
			super(getIndex(indexAndId), getChangeset(indexAndId), child.getHgRoot());
			this.bundleFile = child.getBundleFile();
			this.direction = child.direction;
		}

		static int getIndex(String parentId) {
			if (parentId == null || parentId.length() < 3) {
				return 0;
			}
			String[] parts = parentId.split(":");
			if (parts.length != 2) {
				return 0;
			}
			try {
				return Integer.valueOf(parts[0]).intValue();
			} catch (NumberFormatException e) {
				return 0;
			}
		}

		static String getChangeset(String parentId) {
			if (parentId == null || parentId.length() < 3) {
				return null;
			}
			String[] parts = parentId.split(":");
			if (parts.length != 2) {
				return null;
			}
			try {
				return parts[1];
			} catch (NumberFormatException e) {
				return null;
			}
		}
	}

	/**
	 * This class is getting too tangled up with everything else, has a a large amount of fields
	 * (17) and worse is that it is not immutable, which makes the entanglement even more dangerous.
	 *
	 * My plan is to make it immutable by using the builder pattern and remove all setters.
	 * FileStatus fetching may(or may not) be feasable to put elsewhere or fetched "on-demand" by
	 * this class itself. Currently, it has no operations and it purely a data class which isn't
	 * very OO efficent.
	 *
	 * Secondly, remove getDirection by tester methods (isIncoming, isOutgoing, isLocal)
	 *
	 */
	public static class Builder {
		private ChangeSet cs;

		public Builder(int revision, String changeSet, String branch, String date, String user,
				HgRoot root) {
			this.cs = new ChangeSet(revision, changeSet, user, date, branch == null ? "" : branch,
					root);
		}

		public Builder tags(String tags) {
			this.cs.tagsStr = tags;
			return this;
		}

		public Builder description(String description) {
			cs.setComment(description);
			return this;
		}

		public Builder parents(String[] parents) {
			this.cs.setParents(parents);
			return this;
		}

		public Builder direction(Direction direction) {
			this.cs.direction = direction;
			return this;
		}

		public Builder changedFiles(FileStatus[] changedFiles) {
			this.cs.changedFiles = changedFiles == null ? EMPTY_STATUS : Collections
					.unmodifiableList(Arrays.asList(changedFiles));
			return this;
		}

		public Builder bundleFile(File bundleFile) {
			this.cs.bundleFile = bundleFile;
			return this;
		}

		public Builder repository(IHgRepositoryLocation repository) {
			this.cs.repository = repository;
			return this;
		}

		// nodeShort should be first X of changeset, this is superfluous
		public Builder nodeShort(String nodeShort) {
			this.cs.nodeShort = nodeShort;
			return this;
		}

		public ChangeSet build() {
			ChangeSet result = this.cs;
			this.cs = null;
			return result;
		}
	}

	ChangeSet(int changesetIndex, String changeSet, String tags, String branch, String user,
			String date, String description, String[] parents, HgRoot root) {
		this.changesetIndex = changesetIndex;
		this.changeset = changeSet;
		this.revision = new HgRevision(changeset, changesetIndex);
		this.tagsStr = tags;
		this.branch = branch;
		this.user = user;
		this.date = date;
		this.hgRoot = root;
		setComment(description);
		setParents(parents);
		// remember index:fullchangesetid
		setName(getIndexAndName());
	}

	private ChangeSet(int changesetIndex, String changeSet, String user, String date,
			String branch, HgRoot root) {
		this(changesetIndex, changeSet, null, branch, user, date, "", null, root); //$NON-NLS-1$
	}

	public int getChangesetIndex() {
		return changesetIndex;
	}

	public String getChangeset() {
		return changeset;
	}

	/**
	 * @return tags array (all tags associated with current changeset). May return empty array, but
	 *         never null
	 * @see ChangeSetUtils#getPrintableTagsString(ChangeSet)
	 */
	public Tag[] getTags() {
		if (tags == null) {
			if (!StringUtils.isEmpty(tagsStr)) {
				String[] tagsStrArr = tagsStr.split("_,_");
				List<Tag> tagList = new ArrayList<Tag>();
				for (String ctag : tagsStrArr) {
					if (StringUtils.isEmpty(ctag)) {
						continue;
					}
					Tag tag = new Tag(hgRoot, ctag, this, false);
					tagList.add(tag);
				}
				if (!tagList.isEmpty()) {
					tags = tagList.toArray(new Tag[tagList.size()]);
				}
			}
			if(tags == null) {
				tags = EMPTY_TAGS;
			}
		}
		return tags;
	}

	/**
	 * @param tags the tags to set
	 */
	public void setTags(Tag[] tags) {
		this.tags = tags;
	}

	/**
	 * @param tagsStr the tagsStr to set
	 */
	public void setTagsStr(String tagsStr) {
		this.tagsStr = tagsStr;
	}

	/**
	 * @return the tagsStr
	 */
	public String getTagsStr() {
		return tagsStr;
	}

	public String getBranch() {
		return branch;
	}

	public String getUser() {
		return user;
	}

	public String getDateString() {
		Date d = getRealDate();
		if (d != null) {
			// needed because static date format instances are not thread safe
			synchronized (DISPLAY_DATE_FORMAT) {
				return DISPLAY_DATE_FORMAT.format(d);
			}
		}
		return date;
	}

	@Override
	public String getComment() {
		return comment;
	}

	public HgRevision getRevision() {
		return revision;
	}

	@Override
	public String toString() {
		return getIndexAndName();

	}

	protected String getIndexAndName() {
		if (nodeShort != null) {
			return changesetIndex + ":" + nodeShort; //$NON-NLS-1$
		}
		return changesetIndex + ":" + changeset; //$NON-NLS-1$
	}

	/**
	 * @return the changedFiles, never null. The returned list is non modifiable so any
	 * attempt to modify it will lead to an exception.
	 */
	public List<FileStatus> getChangedFiles() {
		return changedFiles == null? EMPTY_STATUS : changedFiles;
	}

	/**
	 * @param changedFiles the changedFiles to set
	 */
	public void setChangedFiles(List<FileStatus> changedFiles) {
		this.changedFiles = (changedFiles == null ? EMPTY_STATUS : Collections
				.unmodifiableList(changedFiles));
	}

	public boolean hasFileStatus() {
		return changedFiles != null;
	}

	/**
	 * @param resource
	 *            non null
	 * @return true if the given resource was removed in this changeset
	 */
	public boolean isRemoved(IResource resource) {
		return contains(resource, FileStatus.Action.REMOVED);
	}

	/**
	 * @param resource
	 *            non null
	 * @return true if the given resource was moved in this changeset
	 */
	public boolean isMoved(IResource resource) {
		return contains(resource, FileStatus.Action.MOVED);
	}

	/**
	 * @param resource
	 *            non null
	 * @return true if the given resource was added in this changeset
	 */
	public boolean isAdded(IResource resource) {
		return contains(resource, FileStatus.Action.ADDED);
	}

	/**
	 * @param resource
	 *            non null
	 * @return true if the given resource was modified in this changeset
	 */
	public boolean isModified(IResource resource) {
		return contains(resource, FileStatus.Action.MODIFIED);
	}

	/**
	 * @param resource
	 *            non null
	 * @return file status object if this changeset contains given resource, null otherwise
	 */
	public FileStatus getStatus(IResource resource) {
		if (getChangedFiles().isEmpty()) {
			return null;
		}
		IPath path = ResourceUtils.getPath(resource);
		if(path.isEmpty()) {
			return null;
		}
		for (FileStatus fileStatus : getChangedFiles()) {
			if (path.equals(fileStatus.getAbsolutePath())) {
				return fileStatus;
			}
		}
		return null;
	}

	/**
	 * @param resource
	 *            non null
	 * @param action
	 *            non null
	 * @return true if this changeset contains a resource with given action state
	 */
	private boolean contains(IResource resource, Action action) {
		if (getChangedFiles().isEmpty()) {
			return false;
		}
		boolean match = false;
		IPath path = null;
		for (FileStatus fileStatus : getChangedFiles()) {
			if (fileStatus.getAction() == action) {
				if (path == null) {
					path = ResourceUtils.getPath(resource);
				}
				if (path.equals(fileStatus.getAbsolutePath())) {
					match = true;
					break;
				}
			}
		}
		return match;
	}

	/**
	 * @return the ageDate
	 */
	public String getAgeDate() {
		double delta = (System.currentTimeMillis() - getRealDate().getTime());

		delta /= 1000 * 60; // units is minutes

		if (delta <= 1) {
			return "less than a minute ago";
		}

		if (delta <= 60) {
			return makeAgeString(delta, "minute");
		}

		delta /= 60;
		if (delta <= 24) {
			return makeAgeString(delta, "hour");
		}

		// 1 day to 31 days
		delta /= 24; // units is days
		if (delta <= 31) {
			return makeAgeString(delta, "day");
		}

		// 4 weeks - 3 months
		if (delta / 7 <= 12) {
			return makeAgeString(delta / 7, "week");
		}

		// 3 months - 1 year
		if (delta / 30 <= 12) {
			return makeAgeString(delta / 30, "month");
		}

		return makeAgeString(delta / 365, "year");
	}

	private static String makeAgeString(double d, String unit) {
		int i = (int) Math.max(1, Math.round(d));

		return i + " " + unit + ((i == 1) ? "" : "s") + " ago";
	}

	/**
	 * @return the nodeShort
	 */
	public String getNodeShort() {
		return nodeShort;
	}

	public int compareTo(ChangeSet o) {
		if (o.getChangeset().equals(this.getChangeset())) {
			return 0;
		}
		int result = this.getChangesetIndex() - o.getChangesetIndex();
		if (result != 0) {
			return result;
		}
		if (getRealDate() != UNKNOWN_DATE && o.getRealDate() != UNKNOWN_DATE) {
			return getRealDate().compareTo(o.getRealDate());
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof ChangeSet) {
			ChangeSet other = (ChangeSet) obj;
			if (getChangeset().equals(other.getChangeset())
					&& getChangesetIndex() == other.getChangesetIndex()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 31 + ((changeset == null) ? 0 : changeset.hashCode()) + changesetIndex;
	}

	/**
	 * @return never returns null. Returns {@link ChangeSet#UNKNOWN_DATE} if the date can't be
	 *         parsed
	 */
	public Date getRealDate() {
		try {
			if (realDate == null) {
				if (date != null) {
					// needed because static date format instances are not thread safe
					synchronized (INPUT_DATE_FORMAT) {
						realDate = INPUT_DATE_FORMAT.parse(date);
					}
				} else {
					realDate = UNKNOWN_DATE;
				}
			}
		} catch (ParseException e) {
			realDate = UNKNOWN_DATE;
		}
		return realDate;
	}

	/**
	 * @return the bundleFile, may be null. The file can contain additional changeset information,
	 *         if this is a changeset used by "incoming" or "pull" operation
	 */
	public File getBundleFile() {
		return bundleFile;
	}

	public String[] getParents() {
		return parents;
	}

	public HgRevision getParentRevision(int ordinal, boolean bQuery) {
		if (bQuery && getChangesetIndex() != 0 && (parents == null || parents.length == 0)) {
			try {
				parents = HgParentClient.getParentNodeIds(this, "{rev}:{node}");
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		return getParentRevision(ordinal);
	}

	public HgRevision getParentRevision(int ordinal) {
		if (parents != null && 0 <= ordinal && ordinal < parents.length) {
			return HgRevision.parse(parents[ordinal]);
		}

		return null;
	}

	public void setParents(String[] parents) {
		// filter null parents (hg uses -1 to signify a null parent)
		if (parents != null) {
			List<String> temp = new ArrayList<String>(parents.length);
			for (int i = 0; i < parents.length; i++) {
				String parent = parents[i];
				if (parent.charAt(0) != '-') {
					temp.add(parent);
				}
			}
			this.parents = temp.toArray(new String[temp.size()]);
		}
	}

	/**
	 * @return True if this is a merge changeset.
	 */
	public boolean isMerge() {
		return parents != null && 1 < parents.length && !StringUtils.isEmpty(parents[0])
				&& !StringUtils.isEmpty(parents[1]);
	}

	public void setComment(String comment) {
		if (comment != null) {
			this.comment = comment;
		} else {
			this.comment = "";
		}
	}

	public String getSummary() {
		return StringUtils.removeLineBreaks(getComment());
	}

	/**
	 * @return the repository
	 */
	public IHgRepositoryLocation getRepository() {
		return repository;
	}

	/**
	 * @return the direction
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * @return the hgRoot file (always as <b>canonical path</b>)
	 * @see File#getCanonicalPath()
	 */
	public HgRoot getHgRoot() {
		return hgRoot;
	}

	@Override
	public boolean contains(IResource local) {
		return getFiles().contains(local);
	}

	public boolean contains(IPath local) {
		for (IFile resource : getFiles()) {
			if (local.equals(ResourceUtils.getPath(resource))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsChildren(final IResource local, int depth) {
		return contains(local);
	}

	/**
	 * This method should NOT be used directly by clients of Mercurial plugin except those from
	 * "synchronize" packages. It exists only to fulfill contract with Team "synchronize" API and is
	 * NOT performant, as it may create dynamic proxy objects. {@inheritDoc}
	 */
	@Override
	@DoNotDisplayMe
	public IFile[] getResources() {
		return getFiles().toArray(EMPTY_FILES);
	}

	@DoNotDisplayMe
	public FileFromChangeSet[] getChangesetFiles() {
		List<FileFromChangeSet> fcs = new ArrayList<FileFromChangeSet>();

		for (FileStatus fileStatus : getChangedFiles()) {
			int action = 0;
			int dir = 0;
			switch (fileStatus.getAction()) {
			case ADDED:
			case MOVED:
			case COPIED:
				action = Differencer.ADDITION;
				break;
			case MODIFIED:
				action = Differencer.CHANGE;
				break;
			case REMOVED:
				action = Differencer.DELETION;
				break;
			}
			switch (getDirection()) {
			case INCOMING:
				dir |= Differencer.LEFT;
				break;
			case OUTGOING:
				dir |= Differencer.RIGHT;
				break;
			case LOCAL:
				dir |= Differencer.RIGHT;
				break;
			}
			fcs.add(new FileFromChangeSet(this, fileStatus, action | dir));

			if(fileStatus.getAction() == FileStatus.Action.MOVED){
				// for moved files, include an extra FileFromChangeset for the deleted file
				FileStatus fs = new FileStatus(Action.REMOVED, fileStatus.getRootRelativeCopySourcePath().toString(), this.hgRoot);
				fcs.add(new FileFromChangeSet(this, fs, dir | Differencer.DELETION));
			}
		}
		return fcs.toArray(new FileFromChangeSet[0]);
	}

	/**
	 * @return not modifiable set of files changed/added/removed in this changeset, never null. The
	 *         returned file references might not exist (yet/anymore) on the disk or in the Eclipse
	 *         workspace.
	 */
	@DoNotDisplayMe
	public Set<IFile> getFiles() {
		if (files != null) {
			return files;
		}
		Set<IFile> files1 = new LinkedHashSet<IFile>();
		if (changedFiles != null) {
			for (FileStatus fileStatus : changedFiles) {
				IFile fileHandle = ResourceUtils.getFileHandle(fileStatus.getAbsolutePath());
				if (fileHandle != null) {
					files1.add(fileHandle);
				}
			}
		}
		files = Collections.unmodifiableSet(files1);
		return files;
	}

	@Override
	@DoNotDisplayMe
	public SyncInfoTree getSyncInfoSet() {
		return super.getSyncInfoSet();
	}

	@Override
	public boolean isEmpty() {
		return getChangedFiles().isEmpty();
	}

	@Override
	public String getAuthor() {
		return getUser();
	}

	@Override
	public Date getDate() {
		return getRealDate();
	}

	/**
	 * Returns index:fullchangesetid pair
	 */
	@Override
	public String getName() {
		return super.getName();
	}

	/**
	 * @return Whether the repository is currently on this revision
	 */
	public boolean isCurrent() {
		if (direction == Direction.OUTGOING && hgRoot != null) {
			try {
				return equals(LocalChangesetCache.getInstance().getChangesetForRoot(hgRoot));
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		return false;
	}

	@Override
	public void remove(IResource resource) {
		// not supported
	}

	@Override
	public void rootRemoved(IResource resource, int depth) {
		// not supported
	}
}
