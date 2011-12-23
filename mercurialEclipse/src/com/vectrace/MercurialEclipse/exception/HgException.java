/*******************************************************************************
 * Copyright (c) 2006-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Software Balm Consulting Inc (Peter Hunnisett <peter_hge at softwarebalm dot com>) - implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.exception;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.TeamException;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

public class HgException extends TeamException {
	private static final long serialVersionUID = 1L; // Get rid of warning

	public static final int OPERATION_FAILED = -100;
	public static final int OPERATION_CANCELLED = -200;
	public static final String OPERATION_FAILED_STRING = Messages.getString("HgException.operationFailed"); //$NON-NLS-1$

	/**
	 * @see #getConciseMessage()
	 */
	private static final Pattern CONCISE_MESSAGE_PATTERN = Pattern.compile("^(?:remote:\\s*)?abort:\\s*(.+)", Pattern.MULTILINE);

	private static final Pattern LINE_PATTERN = Pattern.compile(".+");

	/**
	 * May be null. Raw hg output.
	 */
	private String output;

	public HgException(IStatus status) {
		super(status);
	}

	public HgException(String message) {
		super(new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
				OPERATION_FAILED, message, new Exception(message)));
	}

	public HgException(CoreException e) {
		super(e);
	}

	public HgException(String message, Throwable e) {
		super(new Status(IStatus.ERROR, MercurialEclipsePlugin.ID,
				OPERATION_FAILED, message, e));
	}

	public HgException(int code, String message, Throwable e) {
		super(new Status(IStatus.ERROR, MercurialEclipsePlugin.ID, code, message, e));
	}

	/**
	 * Create an exception indicating an error that occurred invoking hg.
	 */
	public HgException(int code, String message, String jobName, Throwable e) {
		this(code, message + ". Command line: " + jobName, e);

		this.output = message;
	}

	@Override
	public String getMessage() {
		IStatus status = getStatus();
		StringBuilder sb = new StringBuilder(status.getMessage());
		switch (status.getCode()) {
		case OPERATION_CANCELLED:
			break;
		case OPERATION_FAILED:
			break;
		default:
			sb.append(", error code: ").append(status.getCode());
			break;
		}
		return sb.toString();
	}

	@Override
	public String getLocalizedMessage() {
		return getMessage();
	}

	@Override
	public String toString() {
		// never null
		return getMessage();
	}

	private String getOutput() {
		return this.output != null ? this.output : getStatus().getMessage();
	}

	/**
	 * Parses the message looking for "abort:" and if present returns the remainder of the line
	 *
	 * @return A more concise error message if possible. Otherwise the entire error message
	 */
	public String getConciseMessage() {
		String message = getOutput();
		Matcher matcher = CONCISE_MESSAGE_PATTERN.matcher(message);

		if (matcher.find()) {
			return matcher.group(1);
		}

		return message;
	}

	/**
	 * @return Whether the message is multiple lines
	 */
	public boolean isMultiLine() {
		Matcher m = LINE_PATTERN.matcher(getOutput());

		m.find();
		return m.find();
	}
}
