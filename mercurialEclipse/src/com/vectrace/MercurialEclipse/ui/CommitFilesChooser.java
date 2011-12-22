/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Administrator	implementation
 *     Andrei Loskutov         - bug fixes
 *     Zsolt Koppany (Intland) - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.TableColumnSorter;
import com.vectrace.MercurialEclipse.commands.HgStatusClient;
import com.vectrace.MercurialEclipse.dialogs.CommitResource;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceLabelProvider;
import com.vectrace.MercurialEclipse.dialogs.CommitResourceUtil;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * TODO enable tree/flat view switch
 *
 * @author steeven
 */
public class CommitFilesChooser extends Composite {
	private final UntrackedFilesFilter untrackedFilesFilter;
	private final CommittableFilesFilter committableFilesFilter;
	private final boolean selectable;
	private Button showUntrackedFilesButton;
	private Button selectAllButton;
	private final CheckboxTableViewer viewer;
	private final boolean showUntracked;
	private final boolean missing;
	private final ListenerList stateListeners = new ListenerList();
	protected Control trayButton;
	protected boolean trayClosed = true;
	protected IFile selectedFile;
	private final boolean showClean;

	public CheckboxTableViewer getViewer() {
		return viewer;
	}

	public CommitFilesChooser(HgRoot hgRoot, Composite container,
			boolean selectable, boolean showUntracked, boolean showMissing,
			boolean showClean) {
		this(container, selectable, null, showUntracked, showMissing, showClean);
		setResources(hgRoot);
	}

	public CommitFilesChooser(Composite container, boolean selectable,
			List<IResource> resources, boolean showUntracked,
			boolean showMissing, boolean showClean) {
		super(container, container.getStyle());
		this.selectable = selectable;
		this.showUntracked = showUntracked;
		this.missing = showMissing;
		this.showClean = showClean;
		this.untrackedFilesFilter = new UntrackedFilesFilter(missing);
		this.committableFilesFilter = new CommittableFilesFilter();

		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 3;
		layout.horizontalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		setLayout(layout);

		setLayoutData(SWTWidgetHelper.getFillGD(150));

		Table table = createTable();
		viewer = new CheckboxTableViewer(table);
		viewer.setContentProvider(new ArrayContentProvider());
		CommitResourceLabelProvider labelProvider = new CommitResourceLabelProvider();
		labelProvider.addListener(new ILabelProviderListener() {
			public void labelProviderChanged(LabelProviderChangedEvent event) {
				int count = viewer.getTable().getItemCount();
				for (int i = 0; i < count; i++) {
					CommitResource commitResource = (CommitResource) viewer
							.getElementAt(i);
					viewer.update(commitResource, null);
				}
			}
		});
		viewer.setLabelProvider(labelProvider);
		viewer.setComparator(new ViewerComparator());
		viewer.addFilter(committableFilesFilter);
		if (!showUntracked) {
			viewer.addFilter(untrackedFilesFilter);
		}

		createOptionCheckbox();
		createShowDiffButton(container);
		createFileSelectionListener();

		makeActions();
		if (resources != null) {
			setResources(resources);
		}
	}

	private void createFileSelectionListener() {
		getViewer().addSelectionChangedListener(
				new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						ISelection selection = event.getSelection();

						if (selection instanceof IStructuredSelection) {
							IStructuredSelection sel = (IStructuredSelection) selection;
							CommitResource commitResource = (CommitResource) sel
									.getFirstElement();
							if (commitResource != null) {
								IFile oldSelectedFile = selectedFile;
								selectedFile = (IFile) commitResource
										.getResource();
								if (oldSelectedFile == null
										|| !oldSelectedFile
												.equals(selectedFile)) {
									trayButton.setEnabled(true);
								}
							}

						}
					}

				});
	}

	private void createShowDiffButton(Composite container) {
		trayButton = SWTWidgetHelper.createPushButton(container, Messages
				.getString("CommitFilesChooser.showDiffButton.text"), //$NON-NLS-1$
				1);
		trayButton.setEnabled(false);
		trayButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				showDiffForSelection();
			}
		});
	}

	private Table createTable() {
		int flags = SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER;
		if (selectable) {
			flags |= SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI;
		} else {
			flags |= SWT.READ_ONLY | SWT.HIDE_SELECTION;
		}
		Table table = new Table(this, flags);
		table.setHeaderVisible(false);
		table.setLinesVisible(true);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(data);
		return table;
	}

	private void createOptionCheckbox() {
		if (!selectable) {
			return;
		}
		selectAllButton = new Button(this, SWT.CHECK);
		selectAllButton.setText(Messages
				.getString("Common.SelectOrUnselectAll")); //$NON-NLS-1$
		selectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		if (!showUntracked) {
			return;
		}
		showUntrackedFilesButton = new Button(this, SWT.CHECK);
		showUntrackedFilesButton.setText(Messages
				.getString("Common.ShowUntrackedFiles")); //$NON-NLS-1$
		showUntrackedFilesButton.setLayoutData(new GridData(
				GridData.FILL_HORIZONTAL));
	}

	private void makeActions() {
		getViewer().addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				showDiffForSelection();
			}
		});
		getViewer().addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				fireStateChanged();
			}
		});

		if (selectable) {
			selectAllButton.setSelection(false); // Start not selected

			selectAllButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (selectAllButton.getSelection()) {
						getViewer().setAllChecked(true);
					} else {
						getViewer().setAllChecked(false);
					}
					fireStateChanged();
				}
			});
		}

		if (selectable && showUntracked) {
			showUntrackedFilesButton.setSelection(true); // Start selected.
			showUntrackedFilesButton
					.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (showUntrackedFilesButton.getSelection()) {
								getViewer().removeFilter(untrackedFilesFilter);
							} else {
								getViewer().addFilter(untrackedFilesFilter);
							}
							getViewer().refresh(true);
							fireStateChanged();
						}
					});
		}

		final Table table = getViewer().getTable();
		TableColumn[] columns = table.getColumns();
		for (int ci = 0; ci < columns.length; ci++) {
			TableColumn column = columns[ci];
			final int colIdx = ci;
			new TableColumnSorter(getViewer(), column) {
				@Override
				protected int doCompare(Viewer v, Object e1, Object e2) {
					StructuredViewer v1 = (StructuredViewer) v;
					ITableLabelProvider lp = (ITableLabelProvider) v1.getLabelProvider();
					String t1 = lp.getColumnText(e1, colIdx);
					String t2 = lp.getColumnText(e2, colIdx);
					if (t1 != null) {
						return t1.compareTo(t2);
					}
					return 0;
				}
			};
		}
	}

	/**
	 * Set the resources, and from those select resources, which are tracked by
	 * Mercurial
	 *
	 * @param resources
	 *            non null
	 */
	public void setResources(List<IResource> resources) {
		CommitResource[] commitResources = createCommitResources(resources);
		getViewer().setInput(commitResources);

		IPreferenceStore store = MercurialEclipsePlugin.getDefault().getPreferenceStore();
		boolean preSelectAll = store.getBoolean(MercurialPreferenceConstants.PREF_PRESELECT_UNTRACKED_IN_COMMIT_DIALOG);
		if (preSelectAll) {
			getViewer().setCheckedElements(commitResources);
		} else {
			List <CommitResource> tracked = CommitResourceUtil.filterForTracked(commitResources);
			getViewer().setCheckedElements(tracked.toArray());
		}
		if (selectable && (!showUntracked || preSelectAll)) {
			selectAllButton.setSelection(true);
		}
		// show clean file, if we are called on a single, not modified file
		// (revert to any version in the past)
		if (showClean && resources.size() == 1 && commitResources.length == 0) {
			IResource resource = resources.get(0);
			if (resource.getType() == IResource.FILE) {
				HgRoot hgRoot = MercurialTeamProvider.getHgRoot(resource);
				if(hgRoot == null) {
					return;
				}
				File path = new File(hgRoot.toRelative(ResourceUtils.getFileHandle(resource)));
				CommitResource cr = new CommitResource(MercurialStatusCache.BIT_CLEAN,
						resource, path);
				CommitResource[] input = new CommitResource[] { cr };
				getViewer().setInput(input);
				getViewer().setCheckedElements(input);
			}
		}
	}

	/**
	 * Set the all the modified resources from given hg root, and from those
	 * select resources, which are tracked by Mercurial
	 *
	 * @param hgRoot
	 *            non null
	 */
	public void setResources(HgRoot hgRoot) {
		List<IResource> resources = new ArrayList<IResource>();
		// Future: get this from the status cache
		// get the dirty files...
		try {
			Set<IPath> dirtyFilePaths = HgStatusClient
					.getDirtyFilePaths(hgRoot);
			for (IPath path : dirtyFilePaths) {
				IFile fileHandle = ResourceUtils.getFileHandle(path);
				// XXX this would NOT add files which are not under Eclipse
				// control (outside of a project)
				if (fileHandle != null) {
					resources.add(fileHandle);
				}
			}
		} catch (HgException e) {
			MercurialEclipsePlugin.logError(e);
		}
		setResources(resources);
	}

	/**
	 * Create the Commit-resources' for a set of resources
	 *
	 * @param res
	 */
	private CommitResource[] createCommitResources(List<IResource> res) {
		return CommitResourceUtil.getCommitResources(res);
	}

	/**
	 * Set the selected resources. Obviously this will only select those
	 * resources, which are displayed...
	 *
	 * @param resources
	 */
	public void setSelectedResources(List<IResource> resources) {
		CommitResource[] allCommitResources = (CommitResource[]) getViewer()
				.getInput();
		if (allCommitResources == null) {
			return;
		}
		List<CommitResource> selected = CommitResourceUtil
				.filterForResources(Arrays.asList(allCommitResources),
						resources);
		getViewer().setCheckedElements(selected.toArray());
	}

	public List<IResource> getCheckedResources(String... status) {
		return getViewerResources(true, status);
	}

	public List<IResource> getUncheckedResources(String... status) {
		return getViewerResources(false, status);
	}

	public List<IResource> getViewerResources(boolean checked, String... status) {
		TableItem[] children = getViewer().getTable().getItems();
		List<IResource> list = new ArrayList<IResource>(children.length);
		for (TableItem item : children) {
			if (item.getChecked() == checked
					&& item.getData() instanceof CommitResource) {
				CommitResource resource = (CommitResource) item.getData();
				if (status == null || status.length == 0) {
					list.add(resource.getResource());
				} else {
					for (String stat : status) {
						if (resource.getStatusMessage().equals(stat)) {
							list.add(resource.getResource());
							break;
						}
					}
				}
			}
		}
		return list;
	}

	public void addStateListener(Listener listener) {
		stateListeners.add(listener);
	}

	protected void fireStateChanged() {
		for (Object obj : stateListeners.getListeners()) {
			((Listener) obj).handleEvent(null);
		}
	}

	private void showDiffForSelection() {
		if (selectedFile != null) {
			MercurialRevisionStorage iStorage = new MercurialRevisionStorage(selectedFile);
			try {
				CompareUtils.openEditor(selectedFile, iStorage, true, null);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
	}

	public boolean isSelectable() {
		return selectable;
	}
}
