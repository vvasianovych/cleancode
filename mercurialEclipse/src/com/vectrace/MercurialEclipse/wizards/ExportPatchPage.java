/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     steeven                    - implementation
 *     Andrei Loskutov            - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.ui.CommitFilesChooser;
import com.vectrace.MercurialEclipse.ui.LocationChooser;
import com.vectrace.MercurialEclipse.ui.SWTWidgetHelper;
import com.vectrace.MercurialEclipse.ui.LocationChooser.Location;
import com.vectrace.MercurialEclipse.utils.StringUtils;

/**
 * A wizard page which will allow the user to choose location to export patch.
 *
 */
public abstract class ExportPatchPage extends HgWizardPage implements Listener {

	private LocationChooser locationChooser;

	// constructors

	private ExportPatchPage() {
		super(Messages.getString("ExportPatchWizard.pageName"), Messages //$NON-NLS-1$
				.getString("ExportPatchWizard.pageTitle"), null); // TODO icon //$NON-NLS-1$
	}

	// operations

	protected boolean validatePage() {
		String msg = locationChooser.validate();
		if (msg == null && getSelectedItems().length == 0) {
			msg = "Please select at least one file to export"; //$NON-NLS-1$
		}
		if (msg == null) {
			setMessage(null);
		}
		setErrorMessage(msg);
		setPageComplete(msg == null);
		return msg == null;
	}

	public void createControl(Composite parent) {
		Composite composite = SWTWidgetHelper.createComposite(parent, 1);
		Group group = SWTWidgetHelper.createGroup(composite, Messages
				.getString("ExportPatchWizard.PathLocation")); //$NON-NLS-1$
		locationChooser = new LocationChooser(group, true, getDialogSettings(), getFileName());
		locationChooser.addStateListener(this);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalAlignment = SWT.FILL;
		locationChooser.setLayoutData(data);

		createItemChooser(composite);
		setControl(composite);
		validatePage();
	}

	/**
	 * @return The file name to use, or null to use most recent
	 */
	protected abstract String getFileName();

	protected abstract void createItemChooser(Composite composite);

	public abstract Object[] getSelectedItems();

	public void handleEvent(Event event) {
		validatePage();
	}

	public Location getLocation() {
		return locationChooser.getCheckedLocation();
	}

	@Override
	public boolean finish(IProgressMonitor monitor) {
		locationChooser.saveSettings();
		return super.finish(monitor);
	}

	public static ExportPatchPage create(final List<IResource> resource) {
		return new ResourceExportPatchPage(resource);
	}

	public static ExportPatchPage create(final ChangeSet cs) {
		return new ChangeSetExportPatchPage(cs);
	}

	// inner types

	private static final class ResourceExportPatchPage extends ExportPatchPage {

		private final List<IResource> resource;
		private CommitFilesChooser commitFiles;

		private ResourceExportPatchPage(List<IResource> resource) {
			this.resource = resource;
		}

		@Override
		protected void createItemChooser(Composite composite) {
			// TODO no diff for untracked files, bug?
			commitFiles = new CommitFilesChooser(composite, true, resource, false, false, false);
			commitFiles.setLayoutData(new GridData(GridData.FILL_BOTH));
			commitFiles.addStateListener(this);
		}

		@Override
		public Object[] getSelectedItems() {
			List<IResource> l = commitFiles.getCheckedResources();

			return l.toArray(new IResource[l.size()]);
		}

		/**
		 * @see com.vectrace.MercurialEclipse.wizards.ExportPatchPage#getFileName()
		 */
		@Override
		protected String getFileName() {
			return "UncommittedChanges.patch";
		}
	}

	private static final class ChangeSetExportPatchPage extends ExportPatchPage {

		private final ChangeSet cs;

		private ChangeSetExportPatchPage(ChangeSet cs) {
			this.cs = cs;
		}

		/**
		 * @see com.vectrace.MercurialEclipse.wizards.ExportPatchPage#createItemChooser(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		protected void createItemChooser(Composite composite) {

			Composite c = new Composite(composite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.verticalSpacing = 3;
			layout.horizontalSpacing = 0;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			c.setLayout(layout);

			c.setLayoutData(SWTWidgetHelper.getFillGD(150));

			Table table = new Table(c, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.READ_ONLY
					| SWT.HIDE_SELECTION);
			table.setHeaderVisible(false);
			table.setLinesVisible(true);
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			table.setLayoutData(data);

			TableViewer viewer = new TableViewer(table);
			viewer.setContentProvider(new ArrayContentProvider());

			viewer.setLabelProvider(new LabelProvider() {
				/**
				 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
				 */
				@Override
				public Image getImage(Object element) {
					if (element instanceof ChangeSet) {
						return MercurialEclipsePlugin.getImage("elcl16/changeset_obj.gif");
					}

					return super.getImage(element);
				}

				/**
				 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
				 */
				@Override
				public String getText(Object element) {
					if (element instanceof ChangeSet) {

						ChangeSet cset = (ChangeSet) element;
						StringBuilder sb = new StringBuilder();
						sb.append(cset.getChangesetIndex());

						sb.append(" [").append(cset.getAuthor()).append(']');
						sb.append(" (").append(cset.getAgeDate()).append(')');
						if (!StringUtils.isEmpty(cset.getBranch())
								&& !"default".equals(cset.getBranch())) {
							sb.append(' ').append(cset.getBranch()).append(':');
						}
						sb.append(' ').append(cset.getSummary());

						return sb.toString();
					}

					return super.getText(element);
				}
			});
			viewer.setComparator(new ViewerComparator());
			viewer.setInput(new Object[] { cs });
		}

		@Override
		public Object[] getSelectedItems() {
			return new ChangeSet[] { cs };
		}

		/**
		 * @see com.vectrace.MercurialEclipse.wizards.ExportPatchPage#getFileName()
		 */
		@Override
		protected String getFileName() {
			HgRoot root = cs.getHgRoot();
			List<IProject> projects = (root == null) ? null : MercurialTeamProvider.getKnownHgProjects(root);
			String sFile = cs.getChangeset().substring(0, 10) + ".patch";

			if (projects != null)
			{
				for (IProject proj : projects) {
					sFile = proj.getName() + "-" + sFile;
				}
			}

			return sFile;
		}
	}
}
