/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov 			- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.cs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.synchronize.actions.OpenFileInSystemEditorAction;
import org.eclipse.team.ui.mapping.SynchronizationActionProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.properties.OpenPropertiesAction;
import com.vectrace.MercurialEclipse.synchronize.actions.DeleteAction;
import com.vectrace.MercurialEclipse.synchronize.actions.MercurialSynchronizePageActionGroup;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * This class adds some actions supporting different objects in the sync view
 * @author Andrei
 */
@SuppressWarnings("restriction")
public class HgChangeSetActionProvider extends SynchronizationActionProvider {

	private OpenFileInSystemEditorAction openFileAction;
	private ISynchronizePageConfiguration configuration;
	private DeleteAction deleteAction;
	private Action propertiesAction;

	public HgChangeSetActionProvider() {
		super();
	}

	@Override
	protected void initializeOpenActions() {
		super.initializeOpenActions();

		ICommonViewerSite cvs = getActionSite().getViewSite();
		configuration = getSynchronizePageConfiguration();
		if (cvs instanceof ICommonViewerWorkbenchSite && configuration != null) {
			ICommonViewerWorkbenchSite cvws = (ICommonViewerWorkbenchSite) cvs;
			final IWorkbenchPartSite wps = cvws.getSite();
			if (wps instanceof IViewSite) {
				openFileAction = new OpenFileInSystemEditorAction(wps.getPage());
				deleteAction = new DeleteAction("Delete",
						configuration, wps.getSelectionProvider());
				deleteAction.setActionDefinitionId(MercurialSynchronizePageActionGroup.EDIT_DELETE);
				deleteAction.setId(MercurialSynchronizePageActionGroup.EDIT_DELETE);
				propertiesAction = new Action("Properties",
						AbstractUIPlugin.imageDescriptorFromPlugin("org.eclipse.ui",
								"/icons/full/eview16/prop_ps.gif")) {
					{
						setId(OpenPropertiesAction.class.getName());
					}
					@Override
					public void run() {
						new OpenPropertiesAction().run(this);
					}
				};
			}
		}
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		if(menu.find(DeleteAction.HG_DELETE_GROUP) == null){
			menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP, new Separator(
					DeleteAction.HG_DELETE_GROUP));
		}
		ISelection selection = getSite().getSelectionProvider().getSelection();
		if (selection instanceof IStructuredSelection && !hasFileMenu(menu)) {
			fillOpenWithMenu(menu, ISynchronizePageConfiguration.FILE_GROUP,
					(IStructuredSelection) selection);
		}
		if (selection instanceof IStructuredSelection && !hasDeleteMenu(menu)) {
			fillDeleteMenu(menu, DeleteAction.HG_DELETE_GROUP,
					(IStructuredSelection) selection);
		}
		if (selection instanceof IStructuredSelection && !hasPropertiesMenu(menu)) {
			fillPropertiesMenu(menu, ISynchronizePageConfiguration.OBJECT_CONTRIBUTIONS_GROUP,
					(IStructuredSelection) selection);
		}
	}

	private void fillDeleteMenu(IMenuManager menu, String groupId, IStructuredSelection selection) {
		// Only supported if at least one file is selected.
		if (selection == null || selection.size() != 1) {
			return;
		}

		Object element = selection.getFirstElement();
		if(element instanceof ChangeSet && !(element instanceof WorkingChangeSet)){
			return;
		}
		Object selected;
		if(element instanceof WorkingChangeSet && !((WorkingChangeSet)element).isDefault()) {
			selected = element;
		} else {
			IResource resource = ResourceUtils.getResource(element);
			if ((!(resource instanceof IFile) && !(resource instanceof IFolder)) || !resource.exists()) {
				return;
			}
			selected = resource;
		}
		if (deleteAction != null) {
			deleteAction.selectionChanged(new StructuredSelection(selected));
			menu.appendToGroup(groupId, deleteAction);
		}
	}

	private void fillPropertiesMenu(IMenuManager menu, String groupId, IStructuredSelection selection) {
		// Only supported if at least one changeset is selected.
		if (selection == null || selection.size() != 1) {
			return;
		}

		Object element = selection.getFirstElement();
		if(!(element instanceof ChangeSet) || element instanceof WorkingChangeSet) {
			return;
		}
		if (propertiesAction != null) {
			menu.appendToGroup(groupId, propertiesAction);
		}
	}

	/**
	 * Adds the OpenWith submenu to the context menu.
	 *
	 * @param menu the context menu
	 * @param selection the current selection
	 */
	private void fillOpenWithMenu(IMenuManager menu, String groupId, IStructuredSelection selection) {
		// Only supported if at least one file is selected.
		if (selection == null || selection.size() != 1) {
			return;
		}

		Object element = selection.getFirstElement();
		if(element instanceof ChangeSet){
			return;
		}

		IResource resource = ResourceUtils.getResource(element);
		if (!(resource instanceof IFile) || !resource.exists()) {
			return;
		}

		if (openFileAction != null) {
			openFileAction.selectionChanged(new StructuredSelection(resource));
			menu.appendToGroup(groupId, openFileAction);
		}

		IWorkbenchSite ws = getSite().getWorkbenchSite();
		if (ws != null) {
			MenuManager submenu = new MenuManager(TeamUIMessages.OpenWithActionGroup_0);
			submenu.add(new OpenWithMenu(ws.getPage(), resource));
			menu.appendToGroup(groupId, submenu);
		}
	}

	private ISynchronizePageSite getSite() {
		return configuration.getSite();
	}

	private boolean hasFileMenu(IMenuManager menu) {
		return openFileAction != null && menu.find(openFileAction.getId()) != null;
	}
	private boolean hasDeleteMenu(IMenuManager menu) {
		return deleteAction != null && menu.find(deleteAction.getId()) != null;
	}
	private boolean hasPropertiesMenu(IMenuManager menu) {
		return propertiesAction != null && menu.find(propertiesAction.getId()) != null;
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);
	}
}
