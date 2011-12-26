/*******************************************************************************
 * Copyright (c) See next comment
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan C - Copy code
 *******************************************************************************/
/*******************************************************************************
 * TableViewerSorting Example
 *
 * Adam Cabler
 *
 * revised example by Tom Schindl
 * http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.jface.snippets/Eclipse%20JFace%20Snippets/org/eclipse/jface/snippets/viewers/Snippet040TableViewerSorting.java?view=markup
 *
 * removed ColumnViewer references
 * added ITableLabelProvider
 * used label provider for text compare
 *
 * minor change by Stefan Chysser
 *   - only cycle between ASC and DESC, the go-back to None is confusing
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *
 *******************************************************************************/

package com.vectrace.MercurialEclipse;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.TableColumn;

public abstract class TableColumnSorter extends ViewerComparator {
	public static final int ASC = 1;

	public static final int NONE = 0;

	public static final int DESC = -1;

	private int direction;

	private final TableColumn column;

	private final TableViewer viewer;

	public TableColumnSorter(TableViewer viewer, TableColumn column) {
		this.column = column;
		this.viewer = viewer;
		this.column.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				if (TableColumnSorter.this.viewer.getComparator() != null) {
					if (TableColumnSorter.this.viewer.getComparator() == TableColumnSorter.this) {
						int tdirection = TableColumnSorter.this.direction;

						if (tdirection == ASC) {
							setSorter(TableColumnSorter.this, DESC);
						} else if (tdirection == DESC) {
							setSorter(TableColumnSorter.this, ASC);
						}
					} else {
						setSorter(TableColumnSorter.this, ASC);
					}
				} else {
					setSorter(TableColumnSorter.this, ASC);
				}
			}
		});
	}

	public void setSorter(TableColumnSorter sorter, int direction) {
		if (direction == NONE) {
			column.getParent().setSortColumn(null);
			column.getParent().setSortDirection(SWT.NONE);
			viewer.setComparator(null);
		} else {
			column.getParent().setSortColumn(column);
			sorter.direction = direction;

			if (direction == ASC) {
				column.getParent().setSortDirection(SWT.DOWN);
			} else {
				column.getParent().setSortDirection(SWT.UP);
			}

			if (viewer.getComparator() == sorter) {
				viewer.refresh();
			} else {
				viewer.setComparator(sorter);
			}

		}
	}

	@Override
	public int compare(Viewer cViewer, Object e1, Object e2) {
		return direction * doCompare(cViewer, e1, e2);
	}

	protected abstract int doCompare(Viewer tableViewer, Object e1, Object e2);
}
