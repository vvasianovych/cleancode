/*******************************************************************************
 * Copyright (c) 2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Andrei Loskutov           - bug fixes
 *     Zsolt Koppany (Intland)   - bug fixes
 *     Philip Graf               - bug fix
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;

/**
 * @author Jerome Negre <jerome+hg@jnegre.org>
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 */
public class TagTable extends Composite {
	private static final Font PARENT_FONT = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

	private final Table table;
	private int[] parents;
	private boolean showTip;

	private HgRoot hgRoot;
	private ItemMediator[] data;

	public TagTable(Composite parent, HgRoot hgRoot) {
		super(parent, SWT.NONE);
		showTip = true;
		this.hgRoot = hgRoot;

		this.setLayout(new GridLayout());
		this.setLayoutData(new GridData());

		table = new Table(this, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
				| SWT.H_SCROLL | SWT.VIRTUAL);
		table.setItemCount(0);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		String[] titles = {
				Messages.getString("TagTable.column.rev"), Messages.getString("TagTable.column.global"), Messages.getString("TagTable.column.tag"), Messages.getString("TagTable.column.local"), Messages.getString("ChangesetTable.column.summary") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		int[] widths = { 60, 150, 200, 70, 300 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}

		table.addListener(SWT.SetData, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				TableItem item = (TableItem) event.item;
				int index = table.indexOf(item);

				if (data != null && 0 <= index && index < data.length) {
					data[index].setTableItem(item);
				}
			}
		});
	}

	public void hideTip() {
		this.showTip = false;
	}

	public void highlightParents(int[] newParents) {
		this.parents = newParents;
	}

	public void setHgRoot(HgRoot newRoot) {
		this.hgRoot = newRoot;
		table.removeAll();
	}

	public void setTags(Tag[] tags) {
		List<ItemMediator> filtered = new ArrayList<ItemMediator>(tags.length);

		for (Tag tag : tags) {
			if (showTip || !tag.isTip()) {
				filtered.add(new ItemMediator(tag));
			}
		}

		data = filtered.toArray(new ItemMediator[filtered.size()]);
		table.clearAll();
		table.setItemCount(data.length);

		fetchChangesetInfo(data);
	}

	/**
	 * Fetch changeset comments.
	 *
	 * @param tags That tags for which to get comments
	 */
	void fetchChangesetInfo(final ItemMediator[] tags) {
		final LocalChangesetCache cache = LocalChangesetCache.getInstance();
		Job fetchJob = new Job("Retrieving changesets info") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// this can cause UI hang for big projects. Should be done in a job.
				// the only reason we need this is to show the changeset comments, so we can complete
				// this data in background
				Map<String, ChangeSet> tagged = new HashMap<String, ChangeSet>();
				try {
					Set<ChangeSet> allLocalRevisions = cache.refreshAllLocalRevisions(hgRoot, false, false);
					for (ChangeSet cs : allLocalRevisions) {
						if(monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						for (Tag tag : cs.getTags()) {
							tagged.put(tag.getName(), cs);
						}
					}
				} catch (HgException e1) {
					MercurialEclipsePlugin.logError(e1);
				}
				final Map<ItemMediator, ChangeSet> tagToCs = new HashMap<ItemMediator, ChangeSet>();
				for (ItemMediator tag : tags) {
					if(monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					if (showTip || !tag.tag.isTip()) {
						ChangeSet changeSet = tagged.get(tag.tag.getName());
						if(changeSet != null) {
							tagToCs.put(tag, changeSet);
						}
					}
				}

				Runnable updateTable = new Runnable() {
					public void run() {
						for (ItemMediator item : data) {
							if (tagToCs.get(item) != null) {
								item.setSummary(tagToCs.get(item).getSummary());
							}
						}
					}
				};
				MercurialEclipsePlugin.getStandardDisplay().asyncExec(updateTable);
				return Status.OK_STATUS;
			}
		};
		fetchJob.schedule();
	}

	public Tag getSelection() {
		TableItem[] selection = table.getSelection();
		if (selection.length == 0) {
			return null;
		}
		return (Tag) selection[0].getData();
	}

	public void addSelectionListener(SelectionListener listener) {
		table.addSelectionListener(listener);
	}

	private boolean isParent(int r) {
		if (parents == null) {
			return false;
		}
		switch (parents.length) {
		case 2:
			if (r == parents[1]) {
				return true;
			}
			//$FALL-THROUGH$
		case 1:
			if (r == parents[0]) {
				return true;
			}
			//$FALL-THROUGH$
		default:
			return false;
		}
	}

	// inner types

	/**
	 * This would require synchronization, but it's only being called from the UI thread.
	 */
	private class ItemMediator
	{
		public final Tag tag;

		private TableItem item;

		private String summary;

		// constructor

		public ItemMediator(Tag tag) {
			this.tag = tag;
		}

		// operations

		/**
		 * Set the summary. Must be called from the UI thread.
		 * @param summary The summary to set
		 */
		public void setSummary(String summary) {
			this.summary = summary;

			if (item != null && !item.isDisposed()) {
				item.setText(4, summary);
			}
		}

		/**
		 * Apply the tag information to the row. Must be called from the UI thread.
		 *
		 * @param curItem
		 *            The table row
		 */
		public void setTableItem(TableItem curItem) {
			if (isParent(tag.getRevision())) {
				curItem.setFont(PARENT_FONT);
			}
			curItem.setText(0, Integer.toString(tag.getRevision()));
			curItem.setText(1, tag.getGlobalId());
			curItem.setText(2, tag.getName());
			curItem.setText(3, tag.isLocal() ? Messages.getString("TagTable.stateLocal") //$NON-NLS-1$
					: Messages.getString("TagTable.stateGlobal"));
			curItem.setData(tag);

			this.item = curItem;

			if (summary != null) {
				item.setText(4, summary);
			}
		}
	}
}
