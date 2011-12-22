/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		bastian					  - implementation
 * 		Andrei Loskutov           - bugfixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgParentClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.IHgRepositoryLocation;
import com.vectrace.MercurialEclipse.team.MercurialRevisionStorage;
import com.vectrace.MercurialEclipse.team.NullRevision;
import com.vectrace.MercurialEclipse.team.cache.OutgoingChangesetCache;
import com.vectrace.MercurialEclipse.utils.CompareUtils;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

/**
 * @author bastian
 */
public class OutgoingPage extends IncomingPage {
	private boolean svn;
	private class GetOutgoingOperation extends HgOperation {

		public GetOutgoingOperation(IRunnableContext context) {
			super(context);
		}

		@Override
		protected String getActionDescription() {
			return Messages.getString("OutgoingPage.getOutgoingOperation.description"); //$NON-NLS-1$
		}

		/**
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			monitor.beginTask(Messages.getString("OutgoingPage.getOutgoingOperation.beginTask"), 1); //$NON-NLS-1$
			monitor.subTask(Messages.getString("OutgoingPage.getOutgoingOperation.call")); //$NON-NLS-1$
			setChangesets(getOutgoingInternal());
			monitor.worked(1);
			monitor.done();
		}

		private SortedSet<ChangeSet> getOutgoingInternal() {
			if (isSvn()) {
				return new TreeSet<ChangeSet>();
			}
			IHgRepositoryLocation remote = getLocation();
			try {
				Set<ChangeSet> changesets = OutgoingChangesetCache.getInstance().getChangeSets(
						getHgRoot(), remote, null, isForce());
				SortedSet<ChangeSet> revertedSet = new TreeSet<ChangeSet>(Collections.reverseOrder());
				revertedSet.addAll(changesets);
				return revertedSet;
			} catch (HgException e) {
				MercurialEclipsePlugin.showError(e);
				return new TreeSet<ChangeSet>();
			}
		}

	}

	protected class OutgoingDoubleClickListener implements IDoubleClickListener {
		public void doubleClick(DoubleClickEvent event) {
			ChangeSet cs = getSelectedChangeSet();
			IStructuredSelection sel = (IStructuredSelection) event.getSelection();
			FileStatus clickedFileStatus = (FileStatus) sel.getFirstElement();

			if (cs == null || clickedFileStatus == null) {
				return;
			}


			IPath hgRoot = new Path(cs.getHgRoot().getPath());
			IPath fileRelPath = clickedFileStatus.getRootRelativePath();
			IPath fileAbsPath = hgRoot.append(fileRelPath);
			IFile file = ResourceUtils.getFileHandle(fileAbsPath);

			if (file != null) {
				// See issue #10249: Push/Pull diff problem on outgoing/incoming stage
				// This doesn't work here (seems to work only for incoming? or should be fixed there too?)
				// CompareUtils.openEditor(file, cs, true, true);
				MercurialRevisionStorage thisRev = new MercurialRevisionStorage(file, cs.getChangeset());
				MercurialRevisionStorage parentRev;
				String[] parents = cs.getParents();
				if(parents.length == 0){
					// TODO for some reason, we do not always have right parent info in the changesets
					// if we are on the different branch then the changeset. So simply enforce the parents resolving
					try {
						parents = HgParentClient.getParentNodeIds(file, cs);
					} catch (HgException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}

				if(cs.getRevision().getRevision() == 0 || parents.length == 0){
					parentRev = new NullRevision(file, cs);

				} else if(clickedFileStatus.isCopied()){
					IPath fileCopySrcPath = cs.getHgRoot().toAbsolute(clickedFileStatus.getRootRelativeCopySourcePath());
					IFile copySrc = ResourceUtils.getFileHandle(fileCopySrcPath);
					parentRev = new MercurialRevisionStorage(copySrc, parents[0]);

				}else{
					parentRev = new MercurialRevisionStorage(file, parents[0]);
				}
				CompareUtils.openEditor(thisRev, parentRev, true);
			} else {
				// It is possible that file has been removed or part of the
				// repository but not the project (and has incoming changes)
				MercurialEclipsePlugin.showError(new FileNotFoundException(Messages.getString("IncomingPage.compare.file.missing")));
			}

		}
	}

	protected OutgoingPage(String pageName) {
		super(pageName);
		setTitle(Messages.getString("OutgoingPage.title")); //$NON-NLS-1$
		setDescription(Messages.getString("OutgoingPage.description1")); //$NON-NLS-1$
	}

	@Override
	public void setChangesets(SortedSet<ChangeSet> outgoingInternal) {
		super.setChangesets(outgoingInternal);
	}

	@Override
	public SortedSet<ChangeSet> getChangesets() {
		return super.getChangesets();
	}

	@Override
	protected void getInputForPage() throws InvocationTargetException,
			InterruptedException {
		getContainer().run(true, false, new GetOutgoingOperation(getContainer()));
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		revisionCheckBox.setText(Messages.getString("OutgoingPage.option.pushUpTo")); //$NON-NLS-1$
	}

	@Override
	public boolean isSvn() {
		return svn;
	}

	@Override
	public void setSvn(boolean svn) {
		this.svn = svn;
	}

	@Override
	protected IDoubleClickListener getDoubleClickListener() {
		return new OutgoingDoubleClickListener();
	}
}
