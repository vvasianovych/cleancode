/*******************************************************************************
 * Copyright (c) 2010 Andrei Loskutov.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * The page allows to select projects located inside a hg root by root.
 *
 * @author Andrei
 */
public class SelectProjectsToSyncPage extends WizardPage {

	/**
	 * @author Andrei
	 */
	public class HgRootsContentProvider implements ITreeContentProvider {

		public void dispose() {
			// ignore
		}

		public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
			// ignore
		}

		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof IWorkspaceRoot){
				return roots.toArray();
			}
			List<IResource> list = byRoot.get(parentElement);
			return list == null? new Object[0] : list.toArray();
		}

		public Object getParent(Object element) {
			if(element instanceof HgRoot){
				return ResourcesPlugin.getWorkspace().getRoot();
			}
			if(!(element instanceof IProject)){
				return null;
			}
			Set<Entry<HgRoot, List<IResource>>> entrySet = byRoot.entrySet();
			for (Entry<HgRoot, List<IResource>> entry : entrySet) {
				if(entry.getValue().contains(element)){
					return entry.getKey();
				}
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if(element instanceof IWorkspaceRoot){
				return !roots.isEmpty();
			}
			List<IResource> list = byRoot.get(element);
			return list != null;
		}

	}

	private final IProject[] projects;
	private ContainerCheckedTreeViewer viewer;
	private final Set<HgRoot> roots;
	private final Map<HgRoot, List<IResource>> byRoot;
	private final HgRootsContentProvider contentProvider;

	protected SelectProjectsToSyncPage(IProject[] projects) {
		super("Select projects to Synchronize");
		this.projects = projects;
		byRoot = ResourceUtils.groupByRoot(MercurialTeamProvider.getKnownHgProjects());
		roots = Collections.unmodifiableSet(byRoot.keySet());
		contentProvider = new HgRootsContentProvider();
	}

	public void createControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NULL);
		initializeDialogUnits(top);
		top.setLayout(new GridLayout());

		GridData data = new GridData(GridData.FILL_BOTH);
		top.setLayoutData(data);
		setControl(top);

		Label label = new Label(top, SWT.NULL);
		label.setText("Available hg roots to Synchronize:");

		viewer = createViewer(top);
		viewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if(event.getChecked()){
					Object element = event.getElement();
					HgRoot root;
					if(element instanceof IProject){
						root = (HgRoot) contentProvider.getParent(element);
					} else {
						root = (HgRoot) element;
					}
					Object[] elements = viewer.getCheckedElements();
					for (Object object : elements) {
						if(object instanceof IProject){
							HgRoot other = (HgRoot) contentProvider.getParent(object);
							if(!root.equals(other)) {
								viewer.setChecked(object, false);
							}
						} else {
							if(!root.equals(object)) {
								viewer.setChecked(object, false);
							}
						}
					}
				}
				setPageComplete(areAnyElementsChecked());
			}
		});
		setMessage("Select the projects or entire hg roots to be synchronized.\n"
				+ "You can synchronize only one hg root at time.");
	}

	private ContainerCheckedTreeViewer createViewer(Composite parent) {
		ContainerCheckedTreeViewer treeViewer = new ContainerCheckedTreeViewer(parent,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 200;
		data.widthHint = 300;
		treeViewer.getControl().setLayoutData(data);
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(new WorkbenchLabelProvider());
		treeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		Object[] selectedRoots = ResourceUtils.groupByRoot(Arrays.asList(projects)).keySet().toArray();
		if(selectedRoots.length == 1) {
			// only pre-select projects if they are from the same root
			treeViewer.setCheckedElements(projects);
		}
		treeViewer.setExpandedElements(selectedRoots);
		return treeViewer;
	}

	/**
	 * Allow the finish button to be pressed if there are checked resources.
	 */
	protected void updateOKStatus() {
		if(viewer != null) {
			setPageComplete(areAnyElementsChecked());
		} else {
			setPageComplete(false);
		}
	}

	private boolean areAnyElementsChecked() {
		return viewer.getCheckedElements().length > 0;
	}

	public boolean isCreated(){
		return viewer != null;
	}

	protected HgRoot[] getRoots() {
		return roots.toArray(new HgRoot[roots.size()]);
	}

	@SuppressWarnings("unchecked")
	public IProject[] getSelectedProjects() {
		Set<IProject> selected = new HashSet<IProject>();
		Object[] checkedElements = viewer.getCheckedElements();
		for (Object object : checkedElements) {
			if(viewer.getGrayed(object)){
				continue;
			}
			if(object instanceof IProject){
				selected.add((IProject) object);
			} else {
				selected.addAll((Collection<? extends IProject>) byRoot.get(object));
			}
		}
		return selected.toArray(new IProject[selected.size()]);
	}

	public HgRoot getSelectedRoot() {
		Object[] checkedElements = viewer.getCheckedElements();
		for (Object object : checkedElements) {
			if(object instanceof IProject){
				return (HgRoot) contentProvider.getParent(object);
			}
			return (HgRoot) object;
		}
		return null;
	}

	@Override
	public boolean isPageComplete() {
		return areAnyElementsChecked();
	}

	@Override
	public IWizardPage getNextPage() {
		ConfigurationWizardMainPage nextPage = (ConfigurationWizardMainPage) super.getNextPage();
		nextPage.setProperties(MercurialParticipantSynchronizeWizard.initProperties(getSelectedRoot()));
		return nextPage;
	}
}
