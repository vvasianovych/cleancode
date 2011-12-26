/*******************************************************************************
 * Copyright (c) 2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Philip Graf               - implementation
 *     Andrei Loskutov           - bug fixes, refactored to standalone class
 *******************************************************************************/
package com.vectrace.MercurialEclipse.dialogs;

import static com.vectrace.MercurialEclipse.MercurialEclipsePlugin.logError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import com.vectrace.MercurialEclipse.commands.extensions.HgBookmarkClient;
import com.vectrace.MercurialEclipse.model.Bookmark;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.storage.DataLoader;
import com.vectrace.MercurialEclipse.storage.EmptyDataLoader;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;

/**
 * Proposal provider for the revision text field.
 */
public class RevisionContentProposalProvider implements IContentProposalProvider {

	private final Future<SortedSet<ChangeSet>> changeSets;
	private final Future<List<Bookmark>> bookmarks;

	public RevisionContentProposalProvider(final DataLoader dataLoader) {

		ExecutorService executor = Executors.newFixedThreadPool(2);
		if(dataLoader instanceof EmptyDataLoader){
			changeSets = executor.submit(new Callable<SortedSet<ChangeSet>>() {
				public SortedSet<ChangeSet> call() throws Exception {
					return Collections.unmodifiableSortedSet(new TreeSet<ChangeSet>());
				}
			});

			bookmarks = executor.submit(new Callable<List<Bookmark>>() {
				public List<Bookmark> call() throws Exception {
					return Collections.unmodifiableList(new ArrayList<Bookmark>());
				}
			});
		} else {
			changeSets = executor.submit(new Callable<SortedSet<ChangeSet>>() {
				public SortedSet<ChangeSet> call() throws Exception {
					IResource resource = dataLoader.getResource();
					HgRoot hgRoot = dataLoader.getHgRoot();
					SortedSet<ChangeSet> result;
					LocalChangesetCache cache = LocalChangesetCache.getInstance();
					if(resource != null) {
						result = cache.getOrFetchChangeSets(resource);
					} else {
						result = cache.getOrFetchChangeSets(hgRoot);
					}
					if(result.isEmpty() || result.first().getChangesetIndex() > 0) {
						if(resource != null) {
							cache.fetchRevisions(resource, false, 0, 0, false);
							result = cache.getOrFetchChangeSets(resource);
						} else {
							cache.fetchRevisions(hgRoot, false, 0, 0, false);
							result = cache.getOrFetchChangeSets(hgRoot);
						}

						if(result == null) {
							// fetching the change sets failed
							result = Collections.unmodifiableSortedSet(new TreeSet<ChangeSet>());
						}
					}
					return result;
				}
			});

			bookmarks = executor.submit(new Callable<List<Bookmark>>() {
				public List<Bookmark> call() throws Exception {
					return HgBookmarkClient.getBookmarks(dataLoader.getHgRoot());
				}
			});
		}
		executor.shutdown();
	}

	public IContentProposal[] getProposals(String contents, int position) {
		List<IContentProposal> result = new LinkedList<IContentProposal>();
		String filter = contents.substring(0, position).toLowerCase();
		try {
			for (ChangeSet changeSet : changeSets.get()) {
				if (changeSet.getName().toLowerCase().startsWith(filter)
						|| changeSet.getChangeset().startsWith(filter)) {
					result.add(0, new ChangeSetContentProposal(changeSet, ContentType.REVISION));
				} else {
					String value = getTagsStartingWith(filter, changeSet);
					if (value.length() > 0) {
						result.add(0, new ChangeSetContentProposal(changeSet, ContentType.TAG, value));
					} else if (changeSet.getBranch().toLowerCase().startsWith(filter)) {
						result.add(0, new ChangeSetContentProposal(changeSet, ContentType.BRANCH, changeSet.getBranch()));
					}
				}
			}
		} catch (InterruptedException e) {
			logError(Messages.getString("RevisionChooserDialog.error.loadChangesets"), e); //$NON-NLS-1$
		} catch (ExecutionException e) {
			logError(Messages.getString("RevisionChooserDialog.error.loadChangesets"), e); //$NON-NLS-1$
		}
		try {
			for (Bookmark bookmark : bookmarks.get()) {
				if (bookmark.getName().toLowerCase().startsWith(filter)) {
					result.add(new BookmarkContentProposal(bookmark));
				}
			}
		} catch (InterruptedException e) {
			logError(Messages.getString("RevisionChooserDialog.error.loadBookmarks"), e); //$NON-NLS-1$
		} catch (ExecutionException e) {
			logError(Messages.getString("RevisionChooserDialog.error.loadBookmarks"), e); //$NON-NLS-1$
		}
		return result.toArray(new IContentProposal[result.size()]);
	}

	private String getTagsStartingWith(String filter, ChangeSet changeSet) {
		StringBuilder builder = new StringBuilder();
		for(Tag tag: changeSet.getTags()) {
			if(tag.getName().toLowerCase().startsWith(filter)) {
				builder.append(tag.getName()).append(", "); //$NON-NLS-1$
			}
		}
		if(builder.length() > 2) {
			// truncate the trailing ", "
			builder.setLength(builder.length() - 2);
		}
		return builder.toString();
	}

	public static enum ContentType {REVISION, TAG, BRANCH, AUTHOR, DATE, SUMMARY}

	public static class ChangeSetContentProposal implements IContentProposal {

		private static final Pattern LABEL_SPLITTER = Pattern.compile("\\.\\s|[\\n\\r]"); //$NON-NLS-1$

		protected final ChangeSet changeSet;
		private final RevisionContentProposalProvider.ContentType type;
		protected final String value;
		private String label;
		private String description;

		/**
		 * @param changeSet non null
		 * @param type
		 */
		public ChangeSetContentProposal(ChangeSet changeSet, RevisionContentProposalProvider.ContentType type) {
			this.changeSet = changeSet;
			this.type = type;
			value = null;
		}

		public ChangeSetContentProposal(ChangeSet changeSet, RevisionContentProposalProvider.ContentType type, String value) {
			this.changeSet = changeSet;
			this.type = type;
			this.value = value;
		}

		public String getContent() {
			return changeSet.getName();
		}

		public int getCursorPosition() {
			return getContent().length();
		}

		public String getDescription() {
			if(description == null) {
				description = createDescription();
			}
			return description;
		}

		private String createDescription() {
			StringBuilder builder = new StringBuilder();

			// summary
			builder.append(changeSet.getSummary()).append("\n\n"); //$NON-NLS-1$

			// branch (optional)
			String branch = changeSet.getBranch();
			if(branch != null && branch.length() > 0) {
				builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.branch")); //$NON-NLS-1$
				builder.append(": ").append(branch).append('\n'); //$NON-NLS-1$
			}

			// tag (optional)
			String tags = ChangeSetUtils.getPrintableTagsString(changeSet);
			if(tags.length() > 0) {
				builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.tags")); //$NON-NLS-1$
				builder.append(": ").append(tags).append('\n'); //$NON-NLS-1$
			}

			// author
			builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.author")); //$NON-NLS-1$
			builder.append(": ").append(changeSet.getAuthor()).append("\n"); //$NON-NLS-1$

			// date
			builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.date")); //$NON-NLS-1$
			builder.append(": ").append(changeSet.getDateString()).append('\n'); //$NON-NLS-1$

			// revision
			builder.append(Messages.getString("RevisionChooserDialog.fieldassist.description.changeset.revision")); //$NON-NLS-1$
			builder.append(": ").append(changeSet.getName()); //$NON-NLS-1$

			return builder.toString();
		}

		public String getLabel() {
			if(label == null) {
				label = createLabel();
			}
			return label;
		}

		private String createLabel() {
			StringBuilder builder = new StringBuilder(String.valueOf(changeSet.getChangesetIndex()));
			builder.append(": "); //$NON-NLS-1$

			String text;
			switch(type) {
				case TAG:
				case BRANCH:
				case AUTHOR:
				case DATE:
					text = "[" + value + "] " + changeSet.getSummary(); //$NON-NLS-1$ //$NON-NLS-2$
					break;

				case SUMMARY:
				case REVISION:
				default:
					text = changeSet.getSummary();
					break;
			}

			// shorten label text if necessary
			int maxLineLength = getMaxLineLength();
			if(text.length() > maxLineLength) {
				// extract first sentence or line
				text = LABEL_SPLITTER.split(text, 2)[0].trim();
				// shorten it if still too long
				if(text.length() > maxLineLength) {
					text = text.substring(0, maxLineLength - 7).trim() + "..."; //$NON-NLS-1$
				}
				builder.append(text);
			} else {
				builder.append(text);
			}

			return builder.toString();
		}

		protected int getMaxLineLength() {
			return 50;
		}

	}

	public static final class BookmarkContentProposal implements IContentProposal {

		private final Bookmark bookmark;

		private BookmarkContentProposal(Bookmark bookmark) {
			this.bookmark = bookmark;
		}

		public String getContent() {
			return bookmark.getRevision() + ":" + bookmark.getShortNodeId(); //$NON-NLS-1$
		}

		public int getCursorPosition() {
			return getContent().length();
		}

		public String getDescription() {
			return bookmark.getRevision() + ":" + bookmark.getShortNodeId() + "\n\n" + bookmark.getName(); //$NON-NLS-1$ //$NON-NLS-2$
		}

		public String getLabel() {
			return bookmark.getRevision() + ": " + bookmark.getName(); //$NON-NLS-1$
		}
	}
}