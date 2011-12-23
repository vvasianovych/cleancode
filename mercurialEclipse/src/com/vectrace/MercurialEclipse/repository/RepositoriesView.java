/*******************************************************************************
 * Copyright (c) 2003, 2006 Subclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Subclipse project committers - initial API and implementation
 *     Bastian Doetsch              - adaptation
 *     Andrei Loskutov              - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.repository;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.internal.dialogs.PropertyDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.commands.HgInitClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgPath;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.repository.actions.RemoveRootAction;
import com.vectrace.MercurialEclipse.repository.model.AllRootsElement;
import com.vectrace.MercurialEclipse.repository.model.RemoteContentProvider;
import com.vectrace.MercurialEclipse.ui.HgProjectPropertyPage;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;
import com.vectrace.MercurialEclipse.wizards.CloneRepoWizard;
import com.vectrace.MercurialEclipse.wizards.ImportProjectsFromRepoWizard;
import com.vectrace.MercurialEclipse.wizards.NewLocationWizard;

/**
 * RepositoriesView is a view on a set of known Hg repositories
 */
public class RepositoriesView extends ViewPart implements ISelectionListener {

	private final class InitAction extends Action {

		private final Shell shell;

		private InitAction(String text, ImageDescriptor image, Shell shell) {
			super(text, image);
			this.shell = shell;
		}

		@Override
		public void run() {
			IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
			if(selection.size() != 1 || !(selection.getFirstElement() instanceof IHgRepositoryLocation)){
				return;
			}
			final IHgRepositoryLocation repo = (IHgRepositoryLocation) selection.getFirstElement();
			final String [] initResult = new String[]{""};
			Job job = new Job("Init for " + repo.toString()) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						initResult[0] = HgInitClient.init(repo);
					} catch (HgException e) {
						initResult[0] = e.getMessage();
						MercurialEclipsePlugin.logError(e);
						return e.getStatus();
					}
					return Status.OK_STATUS;
				}
			};
			job.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(final IJobChangeEvent event) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							String message;
							if (event.getResult().isOK()) {
								message = "Hg init for repository '" + repo.toString()
										+ "' was successful!";
							} else {
								message = "Hg init for repository '" + repo.toString()
										+ "' failed!";
							}
							if (initResult[0] != null && initResult[0].length() > 0) {
								message += "\nHg said: " + initResult[0];
							}
							MessageDialog.openInformation(shell, "Hg init result", message);
						}
					});
				}
			});
			IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) getSite()
					.getService(IWorkbenchSiteProgressService.class);
			service.schedule(job);
		}
	}

	public static final String VIEW_ID = "com.vectrace.MercurialEclipse.repository.RepositoriesView"; //$NON-NLS-1$

	// The root
	private AllRootsElement root;

	// Actions
	private Action newAction;
	private RemoveRootAction removeRootAction;

	// The tree viewer
	protected TreeViewer treeViewer;

	// Drill down adapter
	private DrillDownAdapter drillPart; // Home, back, and "drill into"

	private Action refreshAction;
	private Action refreshPopupAction;
	private Action collapseAllAction;
	private Action propertiesAction;
	private Action cloneAction;

	private RemoteContentProvider contentProvider;

	/** this listener is used when a repository is added, removed or changed */
	private final IRepositoryListener repositoryListener = new IRepositoryListener() {
		public void repositoryAdded(final IHgRepositoryLocation loc) {
			refresh(loc);
		}

		public void repositoryRemoved(IHgRepositoryLocation loc) {
			refresh(null);
		}

		public void repositoriesChanged(IHgRepositoryLocation[] roots) {
			refresh(null);
		}

		private void refresh(final Object object) {
			Display display = getViewer().getControl().getDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					refreshViewer(object, false);
				}
			});
		}

		public void repositoryModified(IHgRepositoryLocation loc) {
			refresh(null);
		}
	};

	private Action importAction;

	private Action initAction;

	public RepositoriesView() {
		super();
	}

	/**
	 * Contribute actions to the view
	 */
	protected void contributeActions() {

		final Shell shell = getShell();

		// Create actions

		// New Repository (popup)
		newAction = new Action(Messages.getString("RepositoriesView.createRepo"), MercurialEclipsePlugin //$NON-NLS-1$
				.getImageDescriptor("wizards/newlocation_wiz.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				NewLocationWizard wizard = new NewLocationWizard();
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
		};

		// Clone Repository (popup)
		cloneAction = new Action(Messages.getString("RepositoriesView.cloneRepo"), MercurialEclipsePlugin //$NON-NLS-1$
				.getImageDescriptor("clone_repo.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				CloneRepoWizard wizard = new CloneRepoWizard();
				wizard.init(PlatformUI.getWorkbench(), selection);
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
		};

		// Init Repository (popup)
		initAction = new InitAction(Messages.getString("RepositoriesView.initRepo"), MercurialEclipsePlugin //$NON-NLS-1$
				.getImageDescriptor("release_rls.gif"), shell);

		// Import project (popup)
		importAction = new Action(Messages.getString("RepositoriesView.importProject"),
				MercurialEclipsePlugin.getImageDescriptor("import_project.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();

				ImportProjectsFromRepoWizard wizard = new ImportProjectsFromRepoWizard();
				wizard.init(PlatformUI.getWorkbench(), selection);
				WizardDialog dialog = new WizardDialog(shell, wizard);
				dialog.open();
			}
		};

		// Properties
		propertiesAction = new SelectionProviderAction(treeViewer, "Properties") {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
				if(selection.isEmpty()){
					return;
				}
				Object firstElement = selection.getFirstElement();
				PreferenceDialog dialog = createDialog(firstElement);
				if (dialog != null) {
					dialog.open();
				}
				if(!selection.isEmpty()) {
					treeViewer.refresh(firstElement);
				}
			}

			public PreferenceDialog createDialog(Object element) {
				String initialPageId = null;
				if(element instanceof IProject){
					initialPageId = HgProjectPropertyPage.ID;
				}
				return PropertyDialog.createDialogOn(shell, initialPageId, element);
			}
		};

		IActionBars bars = getViewSite().getActionBars();
		bars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), propertiesAction);
		IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
		if (selection.size() == 1) {
			propertiesAction.setEnabled(true);
		} else {
			propertiesAction.setEnabled(false);
		}

		getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				propertiesAction.setEnabled(ss.size() == 1);
			}
		});

		// Remove Root
		removeRootAction = new RemoveRootAction(treeViewer.getControl().getShell());
		removeRootAction.selectionChanged((IStructuredSelection) null);

		bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), removeRootAction);

		// Refresh action (toolbar)
		refreshAction = new Action(Messages.getString("RepositoriesView.refreshRepos"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("elcl16/refresh.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				refreshViewer(null, true);
			}
		};
		refreshAction.setToolTipText(Messages.getString("RepositoriesView.refresh"));  //$NON-NLS-1$
		refreshAction.setDisabledImageDescriptor(MercurialEclipsePlugin
				.getImageDescriptor("dlcl16/refresh.gif")); //$NON-NLS-1$
		refreshAction.setHoverImageDescriptor(MercurialEclipsePlugin
				.getImageDescriptor("clcl16/refresh.gif")); //$NON-NLS-1$
		getViewSite().getActionBars().setGlobalActionHandler(ActionFactory.REFRESH.getId(),
				refreshAction);

		refreshPopupAction = new Action(
				Messages.getString("RepositoriesView.refresh"), MercurialEclipsePlugin //$NON-NLS-1$
						.getImageDescriptor("clcl16/refresh.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				refreshViewerNode();
			}
		};

		// Collapse action
		collapseAllAction = new Action(Messages.getString("RepositoriesView.collapseAll"), //$NON-NLS-1$
				MercurialEclipsePlugin.getImageDescriptor("elcl16/collapseall.gif")) { //$NON-NLS-1$
			@Override
			public void run() {
				collapseAll();
			}
		};
		collapseAllAction.setToolTipText(Messages.getString("RepositoriesView.collapseAll")); //$NON-NLS-1$
		collapseAllAction.setHoverImageDescriptor(MercurialEclipsePlugin
				.getImageDescriptor("clcl16/collapseall.gif")); //$NON-NLS-1$

		// Create the popup menu
		MenuManager menuMgr = new MenuManager();
		Tree tree = treeViewer.getTree();
		Menu menu = menuMgr.createContextMenu(tree);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				addWorkbenchActions(manager);
			}

		});
		menuMgr.setRemoveAllWhenShown(true);
		tree.setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);

		// Create the local tool bar
		IToolBarManager tbm = bars.getToolBarManager();
		IMenuManager mm = bars.getMenuManager();
		drillPart.addNavigationActions(tbm);
		drillPart.addNavigationActions(mm);
		tbm.add(new Separator());
		mm.add(new Separator());
		tbm.add(newAction);
		mm.add(newAction);
		tbm.add(refreshAction);
		mm.add(refreshAction);
		tbm.add(new Separator());
		mm.add(new Separator());
		tbm.add(collapseAllAction);
		mm.add(collapseAllAction);
		tbm.update(false);
		mm.update(false);


		bars.updateActionBars();
	}

	protected void addWorkbenchActions(IMenuManager manager) {
		IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();

		// File actions go first (view file)
		manager.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
		// Misc additions
		manager.add(new Separator("historyGroup")); //$NON-NLS-1$
		manager.add(new Separator("checkoutGroup")); //$NON-NLS-1$
		manager.add(new Separator("exportImportGroup")); //$NON-NLS-1$

		boolean singleFile = selection.size() == 1
			&& selection.getFirstElement() instanceof HgPath;

		if(singleFile) {
			HgPath path = (HgPath) selection.getFirstElement();
			if(path.isDirectory()) {
				manager.add(importAction);
			}
		}

		manager.add(new Separator("miscGroup")); //$NON-NLS-1$


		boolean singleRepoSelected = selection.size() == 1
				&& selection.getFirstElement() instanceof IHgRepositoryLocation;

		if(singleRepoSelected){
			manager.add(cloneAction);
			IHgRepositoryLocation repo = (IHgRepositoryLocation) selection.getFirstElement();
			if(!repo.isLocal() && repo.getLocation().startsWith("ssh:")) {
				manager.add(initAction);
			}
			manager.add(refreshPopupAction);
		}


		removeRootAction.selectionChanged(selection);
		if (removeRootAction.isEnabled()) {
			manager.add(removeRootAction);
		}

		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		if (selection.size() == 1) {
			manager.add(propertiesAction);
		}
	}

	@Override
	public void createPartControl(Composite parent) {
		treeViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		contentProvider = new RemoteContentProvider();
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
		root = new AllRootsElement();
		treeViewer.setInput(root);
		treeViewer.setSorter(new RepositorySorter());
		drillPart = new DrillDownAdapter(treeViewer);

		contributeActions();

		initializeListeners();
		MercurialEclipsePlugin.getRepoManager().addRepositoryListener(repositoryListener);
		getSite().setSelectionProvider(treeViewer);
	}

	protected void initializeListeners() {
		getSite().getWorkbenchWindow().getSelectionService()
				.addPostSelectionListener(this);
		treeViewer.addSelectionChangedListener(removeRootAction);

		// when F5 is pressed, refresh this view
		treeViewer.getControl().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent event) {
				if (event.keyCode == SWT.F5) {
					refreshAction.run();
				}
			}
		});

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent e) {
				handleDoubleClick(e);
			}
		});
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		String msg = getStatusLineMessage(selection);
		getViewSite().getActionBars().getStatusLineManager().setMessage(msg);
	}

	/**
	 * When selection is changed we update the status line
	 */
	private String getStatusLineMessage(ISelection selection) {
		if (!(selection instanceof IStructuredSelection) || selection.isEmpty()) {
			return ""; //$NON-NLS-1$
		}
		IStructuredSelection s = (IStructuredSelection) selection;

		if (s.size() > 1) {
			return String.valueOf(s.size()) + Messages.getString("RepositoriesView.multiSelected"); //$NON-NLS-1$
		}
		return Messages.getString("RepositoriesView.oneSelected"); //$NON-NLS-1$
	}

	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	protected Shell getShell() {
		return treeViewer.getTree().getShell();
	}

	public TreeViewer getViewer() {
		return treeViewer;
	}

	/**
	 * this is called whenever a new repository location is added for example or
	 * when user wants to refresh
	 */
	public void refreshViewer(Object object,
			boolean refreshRepositoriesFolders) {
		if (treeViewer == null || treeViewer.getControl() == null || treeViewer.getControl().isDisposed()) {
			return;
		}
		if (refreshRepositoriesFolders) {
			try {
				MercurialEclipsePlugin.getRepoManager().refreshRepositories(null);
			} catch (HgException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}
		treeViewer.refresh();
		if(object != null) {
			treeViewer.setSelection(new StructuredSelection(object), true);
		}
	}

	@SuppressWarnings("rawtypes")
	protected void refreshViewerNode() {
		IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
		Iterator iter = selection.iterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			if (object instanceof IHgRepositoryLocation) {
				refreshAction.run();
				break;
			}
			treeViewer.refresh(object);
		}
	}

	public void collapseAll() {
		if (treeViewer == null) {
			return;
		}
		treeViewer.getControl().setRedraw(false);
		treeViewer.collapseToLevel(treeViewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
		treeViewer.getControl().setRedraw(true);
	}

	/**
	 * The mouse has been double-clicked in the tree, perform appropriate
	 * behaviour.
	 */
	private void handleDoubleClick(DoubleClickEvent e) {
		// Only act on single selection
		ISelection selection = e.getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection structured = (IStructuredSelection) selection;
		if (structured.size() != 1) {
			return;
		}
		Object firstElement = structured.getFirstElement();
		IResource resource = ResourceUtils.getResource(firstElement);
		if (resource != null && resource.exists() && resource.getType() == IResource.FILE) {
			ResourceUtils.openEditor(null, (IFile) resource);
			return;
		}
		propertiesAction.run();
	}

	@Override
	public void dispose() {
		MercurialEclipsePlugin.getRepoManager().removeRepositoryListener(repositoryListener);
		super.dispose();
		treeViewer = null;
	}

}
