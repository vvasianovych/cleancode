/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * stefanc	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.InputStream;

import junit.framework.TestCase;

/**
 * @author stefanc
 *
 */
public class HgIdentClientTest extends TestCase {

	public void testGetCurrentChangesetId() throws Exception {
		String expected = "9b417223cbb2f53e54872bfbd559db35645a6afb";
		InputStream dirstate = Thread.currentThread().getContextClassLoader()
			.getResourceAsStream("dirstate");
		String id = HgIdentClient.getCurrentChangesetId(dirstate);
		assertEquals(expected, id);
	}

}
