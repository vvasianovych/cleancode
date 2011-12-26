/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Andrei Loskutov              - bug fixes
 *     Zsolt Koppany (Intland)
 *     Adam Berkes (Intland) - modifications
 *     Ilya Ivanov (Intland) - modifications
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.ui.IActionBars;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.ChangeSet.Direction;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.model.WorkingChangeSet;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.synchronize.MercurialSynchronizeParticipant;
import com.vectrace.MercurialEclipse.synchronize.Messages;
import com.vectrace.MercurialEclipse.synchronize.PresentationMode;
import com.vectrace.MercurialEclipse.synchronize.cs.ChangesetGroup;
import com.vectrace.MercurialEclipse.synchronize.cs.HgChangeSetActionProvider;

@SuppressWarnings("restriction")
public class MercurialSynchronizePageActionGroup extends ModelSynchronizeParticipantActionGroup {

	static final String EDIT_CHANGESET_ACTION = "hg.editChangeset";
	private static final String HG_COMMIT_GROUP = "hg.commit";
	private static final String HG_PUSH_PULL_GROUP = "hg.push.pull";

	// see org.eclipse.ui.IWorkbenchCommandConstants.EDIT_DELETE which was introduced in 3.5
	// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=54581
	// TODO replace with the constant as soon as we drop Eclipse 3.4 support
	public static final String EDIT_DELETE = "org.eclipse.ui.edit.delete";

	public static final String HG_CHANGESETS_GROUP = "hg.changesets";
	private final IAction expandAction;

	private final PreferenceAction allBranchesAction;

	private final ArrayList<PresentationModeAction> presentationModeActions;

	private OpenAction openAction;

	// constructor

	public MercurialSynchronizePageActionGroup() {
		super();
		expandAction = new Action("Expand All", MercurialEclipsePlugin.getImageDescriptor("elcl16/expandall.gif")) {
			@Override
			public void run() {
				Viewer viewer = getConfiguration().getPage().getViewer();
				if(viewer instanceof AbstractTreeViewer){
					AbstractTreeViewer treeViewer = (AbstractTreeViewer) viewer;
					treeViewer.expandAll();
				}
			}
		};

		presentationModeActions = new ArrayList<PresentationModeAction>();

		for (PresentationMode mode : PresentationMode.values()) {
			presentationModeActions.add(new PresentationModeAction(mode, MercurialEclipsePlugin
					.getDefault().getPreferenceStore()));
	}

		allBranchesAction = new PreferenceAction("Synchronize all branches", IAction.AS_CHECK_BOX, MercurialEclipsePlugin
				.getDefault().getPreferenceStore(), MercurialPreferenceConstants.PREF_SYNC_ONLY_CURRENT_BRANCH) {
			@Override
			public void run() {
				prefStore.setValue(prefKey, !isChecked());
				MercurialSynchronizeParticipant participant = (MercurialSynchronizeParticipant)getConfiguration().getParticipant();

				participant.refresh(getConfiguration().getSite().getWorkbenchSite(), participant
						.getContext().getScope().getMappings());
			}

			@Override
			protected void update() {
				setChecked(!prefStore.getBoolean(prefKey));
			}
		};
		allBranchesAction.setImageDescriptor(MercurialEclipsePlugin.getImageDescriptor("actions/branch.gif"));
		allBranchesAction.update();
	}

	// operations

	@Override
	public void initialize(ISynchronizePageConfiguration configuration) {
		super.initialize(configuration);

		String keyOpen = SynchronizePageConfiguration.P_OPEN_ACTION;
		Object property = configuration.getProperty(keyOpen);
		if(property instanceof Action){
			openAction = new OpenAction((Action) property, configuration);
			// override default action used on double click with our custom
			configuration.setProperty(keyOpen, openAction);
		}

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.FILE_GROUP,
				new OpenMergeEditorAction("Open In Merge Editor",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				ISynchronizePageConfiguration.FILE_GROUP,
				new ShowHistorySynchronizeAction("Show History",
						configuration, getVisibleRootsSelectionProvider()));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_PUSH_PULL_GROUP,
				new PushPullSynchronizeAction("Push",
						configuration, getVisibleRootsSelectionProvider(), false, false));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_PUSH_PULL_GROUP,
				new PushPullSynchronizeAction("Pull and Update",
						configuration, getVisibleRootsSelectionProvider(), true, true));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_PUSH_PULL_GROUP,
				new PushPullSynchronizeAction("Pull",
						configuration, getVisibleRootsSelectionProvider(), true, false));

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_CHANGESETS_GROUP,
				new CreateNewChangesetSynchronizeAction("Create New Change Set",
						configuration, getVisibleRootsSelectionProvider()));

		EditChangesetSynchronizeAction editAction = new EditChangesetSynchronizeAction("Edit Change Set...",
				configuration, getVisibleRootsSelectionProvider());

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_CHANGESETS_GROUP,
				editAction);
		// remember action to allow OpenAction re-use it on double click
		configuration.setProperty(EDIT_CHANGESET_ACTION, editAction);

		appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU,
				HG_CHANGESETS_GROUP,
				new SetDefaultChangesetSynchronizeAction("Set as Default Change Set",
						configuration, getVisibleRootsSelectionProvider()));
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		if(menu.find(DeleteAction.HG_DELETE_GROUP) == null){
			menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP, new Separator(
					DeleteAction.HG_DELETE_GROUP));
		}
		if(menu.find(HG_COMMIT_GROUP) == null){
			menu.insertBefore(DeleteAction.HG_DELETE_GROUP, new Separator(HG_COMMIT_GROUP));
		}
		if(menu.find(HG_CHANGESETS_GROUP) == null){
			menu.insertAfter(ISynchronizePageConfiguration.EDIT_GROUP, new Separator(HG_CHANGESETS_GROUP));
		}
		if (menu.find(HG_PUSH_PULL_GROUP) == null) {
			menu.insertAfter(ISynchronizePageConfiguration.NAVIGATE_GROUP, new Separator(HG_PUSH_PULL_GROUP));
		}

		addUndoMenu(menu);

		if (isSelectionUncommited()) {
			menu.insertAfter(
					HG_COMMIT_GROUP,
					new AddAction("Add...",
							getConfiguration(), getVisibleRootsSelectionProvider()));

			menu.insertAfter(
					HG_COMMIT_GROUP,
					new CommitSynchronizeAction("Commit...",
							getConfiguration(), getVisibleRootsSelectionProvider()));

			menu.insertAfter(
					HG_COMMIT_GROUP,
					new RevertSynchronizeAction("Revert...",
							getConfiguration(), getVisibleRootsSelectionProvider()));

			menu.insertAfter(
					HG_COMMIT_GROUP,
					new ResolveSynchronizeAction("Mark as Resolved",
							getConfiguration(), getVisibleRootsSelectionProvider()));
		} else if (isSelectionOutgoing()) {
			menu.insertAfter(
					HG_COMMIT_GROUP,
					new ExportPatchSynchronizeAction("Export as patch...",
							getConfiguration(), getVisibleRootsSelectionProvider()));
			menu.insertAfter(
					HG_COMMIT_GROUP,
					new SwitchToSynchronizeAction("Switch to", "Switch to parent",
							getConfiguration(), getVisibleRootsSelectionProvider()));
		}

		super.fillContextMenu(menu);
//		menu.remove("org.eclipse.team.ui.synchronizeLast");
		replaceCompareAndMoveDeleteAction(menu);
	}

	private boolean isSelectionUncommited() {
		Object[] selectedObjects = getSelectedObjects();

		if (selectedObjects.length == 0) {
			return false;
		}

		for (Object object : selectedObjects) {
			if (object instanceof WorkingChangeSet) {
				continue;
			} else if (object instanceof FileFromChangeSet) {
				FileFromChangeSet file = (FileFromChangeSet) object;
				if (!(file.getChangeset() instanceof WorkingChangeSet)) {
					return false;
				}
			} else {
				return false;
			}
		}

		return true;
	}

	private boolean isSelectionOutgoing() {
		Object[] selectedObjects = getSelectedObjects();

		if (selectedObjects.length == 0) {
			return false;
		}

		for (Object object : selectedObjects) {
			if (object instanceof ChangesetGroup) {
				ChangesetGroup csGroup = (ChangesetGroup) object;
				if (csGroup.getDirection() != Direction.OUTGOING) {
					return false;
				}
			} else if (object instanceof ChangeSet) {
				ChangeSet cs = (ChangeSet) object;
				if (cs.getDirection() != Direction.OUTGOING || cs instanceof WorkingChangeSet) {
					return false;
				}
			} else {
				return false;
			}
		}

		return true;
	}

	private void addUndoMenu(IMenuManager menu) {
		MenuManager submenu = new MenuManager("Undo",
				MercurialEclipsePlugin.getImageDescriptor("undo_edit.gif"), null);

		menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP, submenu);

		ISelection selection = getContext().getSelection();

		if (!(selection instanceof StructuredSelection)) {
			return;
		}

		StructuredSelection stSelection = (StructuredSelection) selection;
		if (stSelection.size() != 1) {
			return;
		}

		Object object = stSelection.iterator().next();
		if (object instanceof WorkingChangeSet) {
			return;
		}

		if (object instanceof ChangesetGroup) {
			ChangesetGroup csGroup = ((ChangesetGroup) object);
			if (csGroup.getChangesets().isEmpty() || csGroup.getDirection() != Direction.OUTGOING) {
				return;
			}

			HgRoot hgRoot = csGroup.getChangesets().iterator().next().getHgRoot();
			menu.insertBefore(ISynchronizePageConfiguration.NAVIGATE_GROUP,
					new RollbackSynchronizeAction("Rollback", getConfiguration(), hgRoot, null));
		} else if (object instanceof ChangeSet) {
			ChangeSet changeSet = (ChangeSet) object;

			if (changeSet.getDirection() != Direction.OUTGOING) {
				return;
			}

			HgRoot hgRoot = changeSet.getHgRoot();
			submenu.add(new BackoutSynchronizeAction("Backout...", getConfiguration(), hgRoot, changeSet));
			submenu.add(new StripSynchronizeAction("Strip...", getConfiguration(), hgRoot, changeSet));
		}
	}

	/**
	 * @return Not null.
	 */
	private Object[] getSelectedObjects() {
		ISelection selection = getContext().getSelection();
		Object[] arr = null;

		if (selection instanceof StructuredSelection) {
			arr = ((StructuredSelection) selection).toArray();
		}

		return PathAwareAction.normalize(arr);
	}

	/**
	 * Replaces default "OpenInCompareAction" action with our custom, moves delete action
	 *
	 * @see OpenInCompareAction
	 * @see ModelSynchronizeParticipantActionGroup
	 * @see HgChangeSetActionProvider
	 */
	private void replaceCompareAndMoveDeleteAction(IMenuManager menu) {
		if(openAction == null){
			return;
		}
		Object[] elements = ((IStructuredSelection) getContext().getSelection()).toArray();
		if (elements.length == 0) {
			return;
		}
		IContributionItem fileGroup = findGroup(menu, ISynchronizePageConfiguration.FILE_GROUP);
		if (fileGroup == null) {
			return;
		}
		IContributionItem[] items = menu.getItems();
		for (IContributionItem ci : items) {
			if(!(ci instanceof ActionContributionItem)){
				continue;
			}
			ActionContributionItem ai = (ActionContributionItem) ci;
			IAction action = ai.getAction();
			if(action instanceof OpenInCompareAction){
				menu.remove(ai);
				openAction.setImageDescriptor(action.getImageDescriptor());
				openAction.setText(action.getText());
				menu.prependToGroup(fileGroup.getId(), openAction);
			} else if(EDIT_DELETE.equals(action.getActionDefinitionId())){
				menu.remove(ai);
				menu.appendToGroup(DeleteAction.HG_DELETE_GROUP, ai);
			}
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		IToolBarManager manager = actionBars.getToolBarManager();
		appendToGroup(manager, ISynchronizePageConfiguration.NAVIGATE_GROUP, expandAction);
		appendToGroup(manager, ISynchronizePageConfiguration.MODE_GROUP, allBranchesAction);

		IMenuManager menu = actionBars.getMenuManager();
		IContributionItem group = findGroup(menu, ISynchronizePageConfiguration.LAYOUT_GROUP);
		if (menu != null && group != null) {
			MenuManager layout = new MenuManager(Messages
					.getString("MercurialSynchronizePageActionGroup.PresentationMode"));
			menu.appendToGroup(group.getId(), layout);

			for (PresentationModeAction action : presentationModeActions) {
				layout.add(action);
			}
		}
	}

	/**
	 * @see org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup#dispose()
	 */
	@Override
	public void dispose() {
		for (PresentationModeAction action : presentationModeActions) {
			action.dispose();
		}
		allBranchesAction.dispose();
		super.dispose();
	}

	// inner types

	/**
	 * Listens to a preference store. Must be disposed.
	 */
	private static abstract class PreferenceAction extends Action implements
			IPropertyChangeListener {
		protected final IPreferenceStore prefStore;
		protected final String prefKey;

		protected PreferenceAction(String name, int style, IPreferenceStore configuration,
				String prefKey) {
			super(name, style);

			this.prefStore = configuration;
			this.prefKey = prefKey;

			configuration.addPropertyChangeListener(this);
		}

		protected abstract void update();

		public void dispose() {
			prefStore.removePropertyChangeListener(this);
		}

		public void propertyChange(PropertyChangeEvent event) {
			if (event.getProperty().equals(prefKey)) {
				update();
			}
		}
	}

	private static class PresentationModeAction extends PreferenceAction {
		private final PresentationMode mode;

		protected PresentationModeAction(PresentationMode mode, IPreferenceStore configuration) {
			super(mode.toString(), IAction.AS_RADIO_BUTTON, configuration,
					PresentationMode.PREFERENCE_KEY);

			this.mode = mode;
			update();
		}

		@Override
		public void run() {
			mode.set();
		}

		@Override
		public void update() {
			setChecked(mode.isSet());
		}
	}
}
