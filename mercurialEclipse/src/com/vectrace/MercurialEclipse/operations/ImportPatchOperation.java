/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * VecTrace (Zingo Andersen) - implementation
 * Stefan Groschupf          - logError
 * Stefan C                  - Code cleanup
 * Andrei Loskutov           - bug fixes
 * John Peberdy	             - Move from ImportPatchWizard
 *******************************************************************************/
package com.vectrace.MercurialEclipse.operations;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.commands.HgPatchClient;
import com.vectrace.MercurialEclipse.exception.HgException;
import com.vectrace.MercurialEclipse.model.HgRoot;
import com.vectrace.MercurialEclipse.team.cache.RefreshRootJob;
import com.vectrace.MercurialEclipse.team.cache.RefreshWorkspaceStatusJob;
import com.vectrace.MercurialEclipse.ui.LocationChooser.Location;
import com.vectrace.MercurialEclipse.ui.LocationChooser.LocationType;
import com.vectrace.MercurialEclipse.utils.ClipboardUtils;

public class ImportPatchOperation extends HgOperation {

	private final Location location;
	private final HgRoot hgRoot;
	private final ArrayList<String> options;

	/**
	 * Whether the operation resulted in a conflict
	 */
	private boolean conflict = false;

	public ImportPatchOperation(IRunnableContext context, HgRoot hgRoot, Location location, ArrayList<String> options) {
		super(context);

		this.hgRoot = hgRoot;
		this.location = location;
		this.options = options;
	}

	@Override
	protected String getActionDescription() {
		return Messages.getString("ImportOperation.importing");
	}

	/**
	 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask(getActionDescription(), 1);

		try {
			result = performOperation();
		} catch (HgException e) {
			if (HgPatchClient.isPatchImportConflict(e)) {
				conflict = true;
				result = e.getLocalizedMessage();
			} else {
				throw new InvocationTargetException(e, e.getLocalizedMessage());
			}
		} finally {
			int refreshFlags = RefreshRootJob.LOCAL_AND_OUTGOING;
			if (options != null && options.contains("--no-commit")) {
				refreshFlags = RefreshRootJob.LOCAL;
			}

			new RefreshWorkspaceStatusJob(hgRoot, refreshFlags).schedule();
			monitor.done();
		}
	}

	protected String performOperation() throws HgException {
		if (location.getLocationType() == LocationType.Clipboard) {
			File file = null;
			try {
				file = ClipboardUtils.clipboardToTempFile("mercurial_", //$NON-NLS-1$
						".patch");
				if (file != null) {
					return HgPatchClient.importPatch(hgRoot, file, options);
				}
			} finally {
				if (file != null && file.exists()) {
					boolean deleted = file.delete();
					if (!deleted) {
						MercurialEclipsePlugin.logError("Failed to delete clipboard content file: "
								+ file, null);
					}
				}
			}
		} else {
			return HgPatchClient.importPatch(hgRoot, location.getFile(), options);
		}

		// fail
		return null;
	}

	/**
	 * @return Whether the operation resulted in a conflict
	 */
	public boolean isConflict() {
		return conflict ;
	}
}