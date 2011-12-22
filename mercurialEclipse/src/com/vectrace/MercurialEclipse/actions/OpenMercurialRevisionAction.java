/*******************************************************************************
 * Copyright (c) 2007-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     VecTrace (Zingo Andersen) - implementation
 *     Stefan Groschupf          - logError
 *     Stefan C                  - Code cleanup
 *     Andrei Loskutov           - bug fixes
 *******************************************************************************/

/**
 * Open an "old" revision in an editor from like "History" view.
 */
package com.vectrace.MercurialEclipse.actions;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.part.Page;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class OpenMercurialRevisionAction extends BaseSelectionListenerAction {

	public static class MercurialRevisionEditorInput extends PlatformObject implements
			IWorkbenchAdapter, IStorageEditorInput {

		private final IFileRevision fileRevision;
		private final MercurialRevisionStorage storage;
		private final IEditorDescriptor descriptor;
		private final String fileName;

		public MercurialRevisionEditorInput(IFileRevision revision) {
			this.fileRevision = revision;
			MercurialRevisionStorage tmpStore = null;
			try {
				tmpStore = (MercurialRevisionStorage) revision.getStorage(new NullProgressMonitor());
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			} finally {
				storage = tmpStore;
			}
			if(storage != null){
				IFile file = storage.getResource();
				if(file != null){
					fileName = file.getName();
				} else {
					fileName = storage.getName();
				}
			} else {
				fileName = fileRevision.getName();
			}
			IEditorDescriptor tmpId = null;
			try {
				tmpId = initDescriptor();
			} catch (CoreException e) {
				MercurialEclipsePlugin.logError(e);
			} finally {
				descriptor = tmpId;
			}
		}

		public Object[] getChildren(Object o) {
			return new Object[0];
		}

		public ImageDescriptor getImageDescriptor(Object object) {
			return descriptor.getImageDescriptor();
		}

		public String getLabel(Object o) {
			if (storage != null) {
				return storage.getName();
			}
			return fileRevision.getName();
		}

		public Object getParent(Object o) {
			return null;
		}

		public IStorage getStorage() throws CoreException {
			return storage;
		}

		public boolean exists() {
			return true;
		}

		public ImageDescriptor getImageDescriptor() {
			return descriptor.getImageDescriptor();
		}

		public String getName() {
			return fileName;
		}

		public IPersistableElement getPersistable() {
			return null; // Can to save editor changes
		}

		public String getToolTipText() {
			if (storage != null) {
				return "" + storage.getFullPath();
			}
			return getName();
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object getAdapter(Class adapter) {
			if (adapter == IWorkbenchAdapter.class) {
				return this;
			}
			if (adapter == IFileRevision.class) {
				return fileRevision;
			}
			return super.getAdapter(adapter);
		}

		public String getEditorID() {
			if (descriptor == null || descriptor.isOpenExternal()) {
				return EditorsUI.DEFAULT_TEXT_EDITOR_ID;
			}
			return descriptor.getId();
		}

		private IEditorDescriptor initDescriptor() throws CoreException {
			IContentType type = null;
			IContentTypeManager typeManager = Platform.getContentTypeManager();
			if(storage != null){
				InputStream contents = storage.getContents();
				if (contents != null) {
					try {
						type = typeManager.findContentTypeFor(contents, fileName);
					} catch (IOException e) {
						MercurialEclipsePlugin.logError(e);
					} finally {
						try {
							contents.close();
						} catch (IOException e) {
							MercurialEclipsePlugin.logError(e);
						}
					}
				}
			}
			if (type == null) {
				type = typeManager.findContentTypeFor(fileName);
			}
			IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
			return registry.getDefaultEditor(fileName, type);
		}
	}

	private IStructuredSelection selection;
	private Shell shell;
	private IWorkbenchPage wPage;

	public OpenMercurialRevisionAction(String text) {
		super(text);
	}

	@Override
	public void run() {

		IStructuredSelection structSel = selection;

		Object[] objArray = structSel.toArray();

		for (int i = 0; i < objArray.length; i++) {
			Object tempRevision = objArray[i];

			final IFileRevision revision = (IFileRevision) tempRevision;
			if (revision == null || !revision.exists()) {
				MessageDialog.openError(shell,
						Messages.getString("OpenMercurialRevisionAction.error.deletedRevision"), Messages.getString("OpenMercurialRevisionAction.error.cantOpen")); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				SafeUiJob runnable = new SafeUiJob(Messages.getString("OpenMercurialRevisionAction.job.openingEditor")) { //$NON-NLS-1$

					@Override
					public IStatus runSafe(IProgressMonitor monitor) {
						IStorage file;
						try {
							file = revision.getStorage(monitor);

							if (file instanceof IFile) {
								// if this is the current workspace file, open it
								ResourceUtils.openEditor(wPage, (IFile) file);
							} else {
								// not current revision
								MercurialRevisionEditorInput fileRevEditorInput =
									new MercurialRevisionEditorInput(revision);
								if (!editorAlreadyOpenOnContents(fileRevEditorInput)) {
									String id = fileRevEditorInput.getEditorID();
									wPage.openEditor(fileRevEditorInput, id);
								}
							}
							return super.runSafe(monitor);
						} catch (CoreException e) {
							MercurialEclipsePlugin.logError(e);
							return e.getStatus();
						}
					}
				};
				runnable.schedule();
			}

		}
	}


	@Override
	protected boolean updateSelection(IStructuredSelection selection1) {
		this.selection = selection1;
		return shouldShow();
	}

	public void setPage(Page page) {
		this.shell = page.getSite().getShell();
		this.wPage = page.getSite().getPage();
	}

	public void setPart(IWorkbenchPart part) {
		this.shell = part.getSite().getShell();
		this.wPage = part.getSite().getPage();
	}

	private boolean shouldShow() {
		if (selection.isEmpty()) {
			return false;
		}
		Object[] objArray = selection.toArray();
		for (int i = 0; i < objArray.length; i++) {
			MercurialRevision revision = (MercurialRevision) objArray[i];
			// check to see if any of the selected revisions are deleted revisions
			if (revision != null && (!revision.isFile() || !revision.exists())) {
				return false;
			}
		}
		return true;
	}

	private boolean editorAlreadyOpenOnContents(
			MercurialRevisionEditorInput input) {
		IEditorReference[] editorRefs = wPage.getEditorReferences();
		IFileRevision inputRevision = (IFileRevision) input.getAdapter(IFileRevision.class);
		for (IEditorReference editorRef : editorRefs) {
			IEditorPart part = editorRef.getEditor(false);
			if (part != null
					&& part.getEditorInput() instanceof MercurialRevisionEditorInput) {
				IFileRevision editorRevision = (IFileRevision) part
						.getEditorInput().getAdapter(IFileRevision.class);

				if (inputRevision.equals(editorRevision)) {
					// make the editor that already contains the revision
					// current
					wPage.activate(part);
					return true;
				}
			}
		}
		return false;
	}

}
