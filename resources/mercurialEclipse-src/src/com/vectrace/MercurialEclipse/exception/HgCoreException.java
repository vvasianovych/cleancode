/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.exception;

/**
 * @author bastian
 *
 */
public class HgCoreException extends RuntimeException {

	public HgCoreException() {
		super();
	}

	public HgCoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public HgCoreException(String message) {
		super(message);
	}

	public HgCoreException(Throwable cause) {
		super(cause);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -582465463467902805L;

}
