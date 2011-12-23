/*******************************************************************************
 * Copyright (c) 2008 Bastian Doetsch and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Brian Wallis              - adaptation to branches
 *     Bastian Doetsch           - adaptation to bookmarks
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.List;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.extensions.HgBookmarkClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.Bookmark;
import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class BookmarkTable extends Composite {

	private static final Font PARENT_FONT = JFaceResources.getFontRegistry()
			.getBold(JFaceResources.DIALOG_FONT);

	private final Table table;

	public BookmarkTable(Composite parent) {
		super(parent, SWT.NONE);
		this.setLayout(new GridLayout());
		this.setLayoutData(new GridData());

		table = new Table(this, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION
				| SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 150;
		data.minimumHeight = 50;
		table.setLayoutData(data);

		String[] titles = { Messages.getString("BookmarkTable.column.rev"), Messages.getString("BookmarkTable.column.global"), Messages.getString("BookmarkTable.column.name"), Messages.getString("BookmarkTable.column.state") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		int[] widths = { 60, 150, 300, 70 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}
	}

	public void updateTable(HgRoot hgRoot) {
		try {
			List<Bookmark> bookmarks = HgBookmarkClient.getBookmarks(hgRoot);
			setBookmarks(bookmarks.toArray(new Bookmark[bookmarks.size()]));
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
	}

	public void setBookmarks(Bookmark[] bookmarks) {
		table.removeAll();
		for (Bookmark bm : bookmarks) {
			TableItem row = new TableItem(table, SWT.NONE);
			row.setText(0, Integer.toString(bm.getRevision()));
			row.setText(1, bm.getShortNodeId());
			row.setText(2, bm.getName());
			row.setText(3, bm.isActive() ? Messages.getString("BookmarkTable.stateActive") : Messages.getString("BookmarkTable.stateInactive")); //$NON-NLS-1$ //$NON-NLS-2$
			row.setData(bm);
			if (bm.isActive()) {
				row.setFont(PARENT_FONT);
			}
		}
	}

	public Bookmark getSelection() {
		TableItem[] selection = table.getSelection();
		if (selection.length == 0) {
			return null;
		}
		return (Bookmark) selection[0].getData();
	}

	public void addSelectionListener(SelectionListener listener) {
		table.addSelectionListener(listener);
	}

}
