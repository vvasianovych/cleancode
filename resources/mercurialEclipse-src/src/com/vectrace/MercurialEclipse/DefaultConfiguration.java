/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan                    - implementation
 *     Andrei Loskutov           - bug fixes
 *     Philip Graf               - use default timeout from preferences
 *******************************************************************************/
package com.vectrace.MercurialEclipse;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vectrace.MercurialEclipse.commands.IConfiguration;
import com.vectrace.MercurialEclipse.commands.IConsole;
import com.vectrace.MercurialEclipse.commands.IErrorHandler;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;
import com.vectrace.MercurialEclipse.preferences.TimeoutPreferencePage;
import com.vectrace.MercurialEclipse.team.MercurialUtilities;
import com.vectrace.MercurialEclipse.views.console.HgConsoleHolder;

/**
 * @author Stefan
 */
public class DefaultConfiguration implements IConsole, IErrorHandler, IConfiguration {

	public DefaultConfiguration() {
	}

	public String getExecutable() {
		if (!MercurialEclipsePlugin.getDefault().isHgUsable()) {
			MercurialUtilities.configureHgExecutable();
			MercurialEclipsePlugin.getDefault().checkHgInstallation();
		}

		return MercurialEclipsePlugin.getDefault().getPreferenceStore()
				.getString(MercurialPreferenceConstants.MERCURIAL_EXECUTABLE);
	}

	public String getPreference(String preferenceConstant,
			String defaultIfNotSet) {
		return MercurialUtilities.getPreference(preferenceConstant,
				defaultIfNotSet);
	}

	public void logError(Throwable e) {
		MercurialEclipsePlugin.logError(e);
	}

	public void logWarning(String message, Throwable e) {
		MercurialEclipsePlugin.logWarning(message, e);

	}

	public int getTimeOut(String commandId) {
		int timeout = TimeoutPreferencePage.DEFAULT_TIMEOUT;
		String pref = getPreference(commandId, String.valueOf(timeout));
		try {
			timeout = Integer.parseInt(pref);
			if (timeout < 0) {
				throw new NumberFormatException(Messages.getString("DefaultConfiguration.timoutLessThanEqual")); //$NON-NLS-1$
			}
		} catch (NumberFormatException e) {
			logWarning(Messages.getString("DefaultConfiguration.timeoutForCommand") + commandId //$NON-NLS-1$
					+ Messages.getString("DefaultConfiguration.notCorrectlyConfigured"), e); //$NON-NLS-1$
		}
		return timeout;
	}

	/*
	 * ======================================================
	 *
	 * IConsole methods below
	 *
	 * ======================================================
	 */
	public void commandCompleted(final int exitCode, final long timeInMillis, final String message, final Throwable error) {
		int severity = IStatus.OK;
		switch (exitCode) {
		case 0:
			severity = IStatus.OK;
			break;
		case 1:
			severity = IStatus.OK;
			break;
		default:
			severity = IStatus.ERROR;
		}
		HgConsoleHolder
				.getInstance()
				.getConsole()
				.commandCompleted(timeInMillis,
						new Status(severity, MercurialEclipsePlugin.ID, message), error);
	}

	public void commandInvoked(final String command) {
		HgConsoleHolder.getInstance().getConsole().commandInvoked(command);
	}

	public void printError(final String message, final Throwable root) {
		HgConsoleHolder.getInstance().getConsole().errorLineReceived(root.getMessage());
	}

	public void printMessage(final String message, final Throwable root) {
		HgConsoleHolder.getInstance().getConsole().messageLineReceived(message);
	}

}
