/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.actions.HgOperation;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * Like {@link HgWizard} but finishes with an operation that may throw exceptions. These exceptions
 * are shown in a consistent way. When an exception occurs the finish is aborted.
 */
public abstract class HgOperationWizard extends HgWizard {

	public HgOperationWizard(String windowTitle) {
		super(windowTitle);
	}

	/**
	 * @see com.vectrace.MercurialEclipse.wizards.HgWizard#performFinish()
	 */
	@Override
	public final boolean performFinish() {
		setErrorMessage(null);

		try {
			HgOperation operation = initOperation();

			if (operation == null) {
				return false;
			}
			try {
				getContainer().run(true, false, operation);
			} finally {
				operationFinished();
			}
			return operationSucceeded(operation);
		} catch (Exception e) {
			return operationFailed(e);
		}
	}

	/**
	 * Create the operation to finish the wizard. May return null if operation is not ready. In this
	 * case subclasses must show error.
	 *
	 * @return The operation to use
	 */
	protected abstract HgOperation initOperation() throws HgException;

	/**
	 * The operation finished with no exception
	 *
	 * @return Whether the wizard should close
	 */
	@SuppressWarnings("unused")
	protected boolean operationSucceeded(HgOperation operation) throws HgException {
		return true;
	}

	/**
	 * Template method called before {@link #operationFailed(Exception)} or
	 * {@link #operationSucceeded(HgOperation)}.
	 */
	protected void operationFinished() {
	}

	/**
	 * Called when an exception is thrown by the job. Shows the error to the user.
	 *
	 * @param e
	 *            The exception that occurred
	 * @return Whether the wizard should close
	 */
	protected boolean operationFailed(Throwable e) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}

		MercurialEclipsePlugin.logError(e);

		if (e instanceof HgException) {
			HgException he = (HgException) e;

			setErrorMessage(he.getConciseMessage());

			if (he.isMultiLine()) {
				MercurialEclipsePlugin.showError(e);
			}
		} else if (e instanceof InterruptedException) {
			MercurialEclipsePlugin.logError(e);
		} else {
			setErrorMessage(e.getLocalizedMessage());
			MercurialEclipsePlugin.showError(e);
		}
		return false;
	}

	protected final void setErrorMessage(String message) {
		for (IWizardPage curPage : getPages()) {
			if (curPage instanceof WizardPage) {
				((WizardPage) curPage).setErrorMessage(message);
			}
		}
	}

}
