/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov	 -   implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

import com.vectrace.MercurialEclipse.dialogs.RevisionContentProposalProvider.ChangeSetContentProposal;
import com.vectrace.MercurialEclipse.dialogs.RevisionContentProposalProvider.ContentType;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;


/**
 * @author andrei
 */
public class HistoryContentProposalProvider implements IContentProposalProvider  {

	private final MercurialHistoryPage page;

	/**
	 * @param page non null
	 */
	public HistoryContentProposalProvider(MercurialHistoryPage page) {
		this.page = page;
	}

	public IContentProposal[] getProposals(String contents, int position) {
		contents = contents.trim();
		if(position > contents.length()) {
			position = contents.length();
		}
		List<IContentProposal> result = new LinkedList<IContentProposal>();
		List<MercurialRevision> revisions = page.getMercurialHistory().getRevisions();
		String filter = contents.substring(0, position).toLowerCase();
		for (MercurialRevision revision : revisions) {
			ChangeSet changeSet = revision.getChangeSet();
			if (changeSet.getName().startsWith(filter)
					|| changeSet.getChangeset().startsWith(filter)) {
				result.add(new RevisionContentProposal(revision, ContentType.REVISION, null));
				continue;
			}
			String author = revision.getAuthor();
			if(author != null && author.toLowerCase().contains(filter)) {
				result.add(new RevisionContentProposal(revision, ContentType.AUTHOR, author));
				continue;
			}
			String comment = revision.getComment();
			if(comment != null && comment.toLowerCase().contains(filter)) {
				result.add(new RevisionContentProposal(revision, ContentType.SUMMARY, null));
				continue;
			}
			String tags = ChangeSetUtils.getPrintableTagsString(revision.getChangeSet());
			if(tags.toLowerCase().contains(filter)) {
				result.add(new RevisionContentProposal(revision, ContentType.TAG, tags));
				continue;
			}
			String branch = revision.getChangeSet().getBranch();
			if(branch != null && branch.toLowerCase().contains(filter)) {
				result.add(new RevisionContentProposal(revision, ContentType.BRANCH, branch));
				continue;
			}
			String date = revision.getChangeSet().getDateString();
			if(date != null && date.startsWith(filter)) {
				result.add(new RevisionContentProposal(revision, ContentType.DATE, date));
				continue;
			}
		}
		return result.toArray(new IContentProposal[result.size()]);
	}


	public static class RevisionContentProposal extends ChangeSetContentProposal {

		private final MercurialRevision revision;

		/**
		 * @param revision non null
		 * @param type non null
		 */
		public RevisionContentProposal(MercurialRevision revision, ContentType type, String value) {
			super(revision.getChangeSet(), type, value);
			this.revision = revision;
		}

		/**
		 * @return the revision, never null
		 */
		public MercurialRevision getRevision() {
			return revision;
		}

		@Override
		public String getContent() {
			return value == null? changeSet.getName() : value;
		}

		@Override
		protected int getMaxLineLength() {
			return 500;
		}
	}

}
