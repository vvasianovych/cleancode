/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *     Bastian Doetsch           - adaptation to patches
 *     Andrei Loskutov - bug fixes
 *     Philip Graf               - refactoring: replaced Table with TableViewer
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.vectrace.MercurialEclipse.model.Patch;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class PatchTable extends Composite {

	private static final Font APPLIED_FONT = JFaceResources.getFontRegistry().getBold(
			JFaceResources.DIALOG_FONT);

	private static Color appliedColor;
	private final TableViewer viewer;

	public PatchTable(Composite parent) {
		super(parent, SWT.NONE);

		if (appliedColor == null) {
			appliedColor = new Color(getDisplay(), new RGB(225, 255, 172));
		}

		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		setLayout(tableColumnLayout);


		viewer = new TableViewer(this, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
				| SWT.H_SCROLL);

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new PatchTableLabelProvider());

		String[] titles = {
				Messages.getString("PatchTable.index"), //$NON-NLS-1$
				Messages.getString("PatchTable.applied"), //$NON-NLS-1$
				Messages.getString("PatchTable.name"), //$NON-NLS-1$
				Messages.getString("PatchTable.summary") }; //$NON-NLS-1$
		ColumnLayoutData[] columnWidths = {
				new ColumnPixelData(20, false, true),
				new ColumnPixelData(75, false, true),
				new ColumnWeightData(25, 200, true),
				new ColumnWeightData(75, 200, true) };
		for (int i = 0; i < titles.length; i++) {
			TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
			column.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {
					Patch patch = (Patch) cell.getElement();
					ITableLabelProvider labelProvider = (ITableLabelProvider) viewer
							.getLabelProvider();
					cell.setText(labelProvider.getColumnText(patch, cell.getColumnIndex()));
					cell.setImage(labelProvider.getColumnImage(patch, cell.getColumnIndex()));
					if (patch.isApplied()) {
						cell.setFont(APPLIED_FONT);
					} else {
						cell.setFont(null);
					}
				}
			});
			column.getColumn().setText(titles[i]);
			tableColumnLayout.setColumnData(column.getColumn(), columnWidths[i]);

		}

		Table table = viewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
	}

	public void setPatches(List<Patch> patches) {
		viewer.setInput(patches);
	}

	/**
	 * @return The first selected patch, or {@code null} if the selection is empty.
	 */
	public Patch getSelection() {
		return (Patch) ((IStructuredSelection) viewer.getSelection()).getFirstElement();
	}

	/**
	 * @return A list of the selected patches. If the selection is empty an empty list is returned,
	 *         never {@code null}.
	 */
	@SuppressWarnings("unchecked")
	public List<Patch> getSelections() {
		return ((IStructuredSelection) viewer.getSelection()).toList();
	}

	@Override
	public void dispose() {
		super.dispose();
		appliedColor.dispose();
	}

	public TableViewer getTableViewer() {
		return viewer;
	}

	private static class PatchTableLabelProvider extends LabelProvider implements ITableLabelProvider {

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			Patch patch = (Patch) element;
			switch (columnIndex) {
				case 0:
					return String.valueOf(patch.getIndex());
				case 1:
					return patch.isApplied() ? Messages.getString("PatchTable.statusApplied") : Messages.getString("PatchTable.statusUnapplied"); //$NON-NLS-1$ //$NON-NLS-2$
				case 2:
					return patch.getName();
				case 3:
					return patch.getSummary();
			}
			return null;
		}

	}

}
