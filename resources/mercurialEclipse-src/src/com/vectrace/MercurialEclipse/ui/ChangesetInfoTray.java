/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian       implementation
 * Philip Graf   bug fix
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.SimpleLabelImageProvider;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.utils.ChangeSetUtils;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 *
 */
public class ChangesetInfoTray extends org.eclipse.jface.dialogs.DialogTray {

	private Composite comp;
	private final ChangeSet changeset;
	private ChangedFilesTable changedFileTable;

	private static class ChangesetInfoPathLabelProvider extends DecoratingLabelProvider implements
			ITableLabelProvider {

		public ChangesetInfoPathLabelProvider() {
			super(new SimpleLabelImageProvider(), PlatformUI.getWorkbench().getDecoratorManager()
					.getLabelDecorator());
		}

		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof FileStatus)) {
				return null;
			}
			return getImage(element);
		}

		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof FileStatus)) {
				return null;
			}
			return getText(element);
		}
	}

	public ChangesetInfoTray(ChangeSet cs) {
		this.changeset = cs;
	}

	/**
	 * @see org.eclipse.jface.dialogs.DialogTray#createContents(org.eclipse.swt.widgets .Composite)
	 */
	@Override
	public Control createContents(Composite parent) {
		comp = SWTWidgetHelper.createComposite(parent, 1);
		GridData layoutData = new GridData(GridData.FILL_BOTH);
		layoutData.minimumWidth = 100;
		comp.setLayoutData(layoutData);
		createChangesetInfoGroup();
		createChangedFilesTable();
		// populate viewer
		changedFileTable.getViewer().setInput(changeset);
		return comp;
	}

	/**
	 *
	 */
	private void createChangedFilesTable() {
		Group g = SWTWidgetHelper.createGroup(comp, Messages.getString("ChangesetInfoTray.changedFiles"), 1, GridData.FILL_BOTH); //$NON-NLS-1$
		changedFileTable = new ChangedFilesTable(g, changeset);

	}

	private void createChangesetInfoGroup() {
		Group g = SWTWidgetHelper.createGroup(comp, Messages
				.getString("ChangesetInfoTray.changesetToBeAmended"), 2, GridData.FILL_BOTH); //$NON-NLS-1$
		SWTWidgetHelper.createLabel(g, Messages.getString("ChangesetInfoTray.changeset")); //$NON-NLS-1$
		SWTWidgetHelper.createLabel(g, ChangeSetUtils.getPrintableRevisionShort(changeset));
		SWTWidgetHelper.createLabel(g, Messages.getString("ChangesetInfoTray.tags")); //$NON-NLS-1$
		SWTWidgetHelper.createLabel(g, ChangeSetUtils.getPrintableTagsString(changeset));
		SWTWidgetHelper.createLabel(g, Messages.getString("ChangesetInfoTray.user")); //$NON-NLS-1$
		SWTWidgetHelper.createLabel(g, changeset.getAuthor());
		SWTWidgetHelper.createLabel(g, Messages.getString("ChangesetInfoTray.date")); //$NON-NLS-1$
		SWTWidgetHelper.createLabel(g, String.valueOf(changeset.getDate()));
		SWTWidgetHelper.createLabel(g, Messages.getString("ChangesetInfoTray.comment")); //$NON-NLS-1$
		SWTWidgetHelper.createWrappingLabel(g, changeset.getComment(), 0);
	}

	// inner types

	public static class ChangedFilesTable extends Composite {

		private TableViewer viewer;

		public ChangedFilesTable(Composite parent, final ChangeSet changeset) {
			super(parent, SWT.NONE);

			GridLayout layout = new GridLayout();
			layout.verticalSpacing = 3;
			layout.horizontalSpacing = 0;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			setLayout(layout);
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.heightHint = 150;
			data.minimumHeight = 50;
			setLayoutData(data);

			Table table = new Table(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
			table.setLinesVisible(true);
			table.setHeaderVisible(false);
			data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.heightHint = 150;
			data.minimumHeight = 50;
			table.setLayoutData(data);
			viewer = new TableViewer(table);

			viewer.setLabelProvider(new ChangesetInfoPathLabelProvider());
			IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
				public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
					viewer = (TableViewer) viewer1;
				}

				public void dispose() {

				}

				public Object[] getElements(Object inputElement) {
					return changeset.getChangedFiles().toArray();
				}
			};
			viewer.setContentProvider(contentProvider);
			IDoubleClickListener listener = new IDoubleClickListener() {

				public void doubleClick(DoubleClickEvent event) {
					FileStatus clickedFileStatus = (FileStatus) ((IStructuredSelection) event
							.getSelection()).getFirstElement();
					ChangeSet cs = changeset;
					IPath fileAbsPath = cs.getHgRoot().toAbsolute(
							clickedFileStatus.getRootRelativePath());
					IFile file = ResourceUtils.getFileHandle(fileAbsPath);
					if (file != null) {
						try {
							String[] parents = HgParentClient.getParentNodeIds(file, cs);
							// our amend changeset was a merge changeset. diff is difficult...
							if (parents == null || parents.length == 2) {
								return;
							}
							MercurialRevisionStorage left = new MercurialRevisionStorage(file, cs
									.getChangesetIndex());
							MercurialRevisionStorage right = new MercurialRevisionStorage(file,
									parents[0]);
							CompareUtils.openEditor(left, right, true);
						} catch (HgException e) {
							MercurialEclipsePlugin.logError(e);
						}
					}
				}
			};
			viewer.addDoubleClickListener(listener);
		}

		public StructuredViewer getViewer() {
			return viewer;
		}

	}
}
