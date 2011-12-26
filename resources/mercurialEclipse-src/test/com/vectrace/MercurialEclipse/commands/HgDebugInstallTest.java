/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import com.vectrace.MercurialEclipse.AbstractMercurialTestCase;
import com.vectrace.MercurialEclipse.exception.HgException;

/**
 * @author bastian
 *
 */
public class HgDebugInstallTest extends AbstractMercurialTestCase {

	/*
	 * (non-Javadoc)
	 *
	 * @see com.vectrace.MercurialEclipse.AbstractMercurialTestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.vectrace.MercurialEclipse.AbstractMercurialTestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test method for
	 * {@link com.vectrace.MercurialEclipse.commands.HgDebugInstallClient#debugInstall()}.
	 * @throws HgException
	 */
	public void testDebugInstall() throws HgException {
		String result;
		result = HgDebugInstallClient.debugInstall();
		assertTrue(result != null);
	}

}
