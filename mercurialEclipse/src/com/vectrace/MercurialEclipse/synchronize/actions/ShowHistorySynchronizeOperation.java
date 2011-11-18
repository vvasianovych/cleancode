/*******************************************************************************
 * Copyright (c) 2008 MercurialEclipse project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bastian Doetsch				- implementation
 *     Subclipse                    - original impl.o
 *     Andrei Loskutov              - bug fixes
 ******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.ui.PartInitException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.model.HgRoot;

public class ShowHistorySynchronizeOperation extends SynchronizeModelOperation {
	private final Object input;

	public ShowHistorySynchronizeOperation(
			ISynchronizePageConfiguration configuration,
			IDiffElement[] elements, Object input) {
		super(configuration, elements);
		this.input = input;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		monitor.beginTask("Opening History View...", 1);
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (input instanceof HgRoot) {
					TeamUI.getHistoryView().showHistoryFor(input);
				} else {
					try {
						IHistoryView view = (IHistoryView) getPart().getSite().getPage()
						.showView(IHistoryView.VIEW_ID);
						if (view != null) {
							view.showHistoryFor(input);
						}
					} catch (PartInitException e) {
						MercurialEclipsePlugin.logError(e);
					}
				}
			}
		});
		monitor.done();
	}
}
