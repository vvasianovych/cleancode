/*******************************************************************************
 * Copyright (c) ?
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Charles O'Farrell - implementation (based on subclipse)
 *     StefanC           - jobs framework, code cleanup
 *     Andrei Loskutov   - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.annotations;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.text.source.LineRange;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.IHistoryPage;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension4;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.SafeUiJob;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.history.MercurialHistoryPage;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.team.cache.LocalChangesetCache;
import com.vectrace.MercurialEclipse.team.cache.MercurialStatusCache;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class ShowAnnotationOperation extends TeamOperation {
	public static final class MercurialRevision extends Revision {
		private final CommitterColors colors;

		private final ChangeSet entry;

		private final String string;

		private final AnnotateBlock block;

		private MercurialRevision(CommitterColors colors, ChangeSet entry,
				String string, AnnotateBlock block) {
			this.colors = colors;
			this.entry = entry;
			this.string = string;
			this.block = block;
		}

		@Override
		public Object getHoverInfo() {
			return entry.getUser()
					+ " " + string + " " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(block.getDate()) + "\n\n" + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					(entry.getComment() != null ? entry.getComment() : ""); //$NON-NLS-1$
		}

		/**
		 * @return the entry
		 */
		public ChangeSet getChangeSet() {
			return entry;
		}

		@Override
		public String getAuthor() {
			return block.getUser();
		}

		@Override
		public String getId() {
			return string;
		}

		@Override
		public Date getDate() {
			return block.getDate();
		}

		@Override
		public RGB getColor() {
			return colors.getCommitterRGB(getAuthor());
		}
	}

	private static final String DEFAULT_TEXT_EDITOR_ID = EditorsUI.DEFAULT_TEXT_EDITOR_ID;
	private final IResource res;

	public ShowAnnotationOperation(IWorkbenchPart part, IResource remoteFile) {
		super(part);
		this.res = remoteFile;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask(null, 100);
		try {
			if (!MercurialStatusCache.getInstance().isSupervised(res)) {
				return;
			}

			final AnnotateBlocks annotateBlocks = new AnnotateCommand(res).execute();

			// this is not needed if there is no live annotate
			final RevisionInformation information = createRevisionInformation(
					annotateBlocks, monitor);

			// We aren't running from a UI thread
			new SafeUiJob(Messages.getString("ShowAnnotationOperation.job.name")) { //$NON-NLS-1$
				@Override
				protected IStatus runSafe(IProgressMonitor moni) {
					moni.beginTask(Messages.getString("ShowAnnotationOperation.beginAnnotation"), //$NON-NLS-1$
							IProgressMonitor.UNKNOWN);
					final ITextEditorExtension4 editor = getEditor();
					if (editor != null) {
						editor
								.showRevisionInformation(
										information,
										HgPristineCopyQuickDiffProvider.HG_REFERENCE_PROVIDER);
						final IWorkbenchPage page= getPart().getSite().getPage();
						showHistoryView(page, (ITextEditor)editor);
						page.activate((ITextEditor)editor);
					}
					moni.done();
					return super.runSafe(moni);
				}
			}.schedule();

		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Shows the history view, creating it if necessary, but does not give it focus.
	 *
	 * @param page the workbench page to operate in
	 * @param editor the editor that is showing the file
	 * @return the history view
	 * @throws PartInitException
	 */
	private IHistoryView showHistoryView(IWorkbenchPage page, ITextEditor editor) {
		Object object = res;
		if (object == null) {
			object = editor.getEditorInput();
		}
		IHistoryView historyView= TeamUI.showHistoryFor(page, object, null);
		IHistoryPage historyPage = historyView.getHistoryPage();
		if (historyPage instanceof MercurialHistoryPage){
			MercurialHistoryPage mercurialHistoryPage = (MercurialHistoryPage) historyPage;
			// We need to call link to ensure that the history page gets linked
			// even if the page input did not change
			mercurialHistoryPage.linkWithEditor();
		}
		return historyView;
	}


	@Override
	protected IAction getGotoAction() {
		return super.getGotoAction();
	}

	private ITextEditorExtension4 getEditor() {
		IEditorReference[] references = MercurialEclipsePlugin.getActivePage()
				.getEditorReferences();
		IResource resource = res;
		if (resource == null) {
			return null;
		}

		IWorkbenchPage page = getPart().getSite().getPage();
		for (int i = 0; i < references.length; i++) {
			IEditorReference reference = references[i];
			try {
				if (resource.equals(reference.getEditorInput()
								.getAdapter(IFile.class))) {
					IEditorPart editor = reference.getEditor(false);
					if (editor instanceof ITextEditorExtension4) {
						return (ITextEditorExtension4) editor;
					}
					// editor opened is not a text editor - reopen file using
					// the
					// defualt text editor
					IEditorPart part = page
							.openEditor(new FileEditorInput((IFile) resource),
									DEFAULT_TEXT_EDITOR_ID, true,
									IWorkbenchPage.MATCH_NONE);
					if (part instanceof ITextEditorExtension4) {
						return (ITextEditorExtension4) part;
					}
				}
			} catch (PartInitException e) {
				MercurialEclipsePlugin.logError(e);
			}
		}

		// no existing editor references found, try to open a new editor for the
		// file

		try {
			IEditorDescriptor descrptr = IDE.getEditorDescriptor((IFile) resource);
			// try to open the associated editor only if its an internal
			// editor
			if (descrptr.isInternal()) {
				IEditorPart part = ResourceUtils.openEditor(page, (IFile) resource);
				if (part instanceof ITextEditorExtension4) {
					return (ITextEditorExtension4) part;
				}
				// editor opened is not a text editor - close it
				// NB: editor should not be closed as it could be opened by user before
				// page.closeEditor(part, false);
			}
			// open file in default text editor
			IEditorPart part = IDE.openEditor(page,	(IFile) resource, DEFAULT_TEXT_EDITOR_ID);
			if (part instanceof ITextEditorExtension4) {
				return (ITextEditorExtension4) part;
			}

		} catch (PartInitException e) {
			MercurialEclipsePlugin.logError(e);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private RevisionInformation createRevisionInformation(
			final AnnotateBlocks annotateBlocks, IProgressMonitor monitor)
			throws HgException {
		Map<Integer, ChangeSet> logEntriesByRevision = new HashMap<Integer, ChangeSet>();
		LocalChangesetCache.getInstance().refreshAllLocalRevisions(
				res, true);
		Iterable<ChangeSet> revisions = LocalChangesetCache.getInstance()
				.getOrFetchChangeSets(res);
		for (ChangeSet changeSet : revisions) {
			logEntriesByRevision.put(Integer.valueOf(changeSet.getRevision()
					.getRevision()),
					changeSet);
		}

		RevisionInformation info = new RevisionInformation();

		try {
			Class infoClass = info.getClass();
			Class[] paramTypes = { IInformationControlCreator.class };
			Method method1 = infoClass.getMethod("setHoverControlCreator", //$NON-NLS-1$
					paramTypes);
			Method method2 = infoClass.getMethod(
					"setInformationPresenterControlCreator", paramTypes); //$NON-NLS-1$

			final class AnnotationControlCreator implements
					IInformationControlCreator {
				private final String statusFieldText;

				public AnnotationControlCreator(String statusFieldText) {
					this.statusFieldText = statusFieldText;
				}

				public IInformationControl createInformationControl(Shell parent) {
					return new SourceViewerInformationControl(parent, SWT.TOOL,
							SWT.NONE, JFaceResources.DEFAULT_FONT,
							statusFieldText);
				}
			}

			method1.invoke(info, new Object[] { new AnnotationControlCreator(
					Messages.getString("ShowAnnotationOperation.pressF2ForFocus")) }); //$NON-NLS-1$
			method2.invoke(info, new Object[] { new AnnotationControlCreator(
					null) });

		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
		}

		final CommitterColors colors = CommitterColors.getDefault();

		HashMap<String, Revision> sets = new HashMap<String, Revision>();

		for (final AnnotateBlock block : annotateBlocks.getAnnotateBlocks()) {
			final String revisionString = block.getRevision().toString();
			final ChangeSet logEntry = logEntriesByRevision.get(
					Integer.valueOf(block.getRevision().getRevision()));

			Revision revision = sets.get(revisionString);
			if (revision == null) {
				revision = new MercurialRevision(colors, logEntry,
						revisionString, block);
				sets.put(revisionString, revision);
				info.addRevision(revision);
			}
			revision.addRange(new LineRange(block.getStartLine(), block
					.getEndLine()
					- block.getStartLine() + 1));
		}
		return info;
	}
}
