/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * stefanc	implementation
 * Adam Berkes (Intland) - restructure, special character tests
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.vectrace.MercurialEclipse.model.HgRoot;

/**
 * @author stefanc
 *
 */
public class HgCommitClientTest extends AbstractCommandTest {

	private static final String UE = "\u00FC";
	private static final String OE = "\u00F6";
	private static final String AE = "\u00E4";
	private static final String SS = "\u00DF";

	/* (non-Javadoc)
	 * @see com.vectrace.MercurialEclipse.commands.AbstractCommandTest#testCreateRepo()
	 */
	public void testCommitSimpleMessage() throws Exception {
		doCommit("Simple", "the message");
	}
	/* (non-Javadoc)
	 * @see com.vectrace.MercurialEclipse.commands.AbstractCommandTest#testCreateRepo()
	 */
	public void testCommitMessageWithQuote() throws Exception {
		doCommit("Trasan 'O Banarne", "is this message \" really escaped?");
	}

	public void testCommitMessageWithSpecialChars1() throws Exception {
		String special1 = "árvíztűrő tükörfúrógép";
		String special2 = "Qualit"+AE+"ts Ger"+AE+"te Pl"+AE+"ne Liebesgr"+UE+""+SS+"e Grundgeb"+UE+"hr Kocht"+OE+"pfe";
		doCommit("Simple", special1);
		doCommit("Simple",
				special2,
				"dummy2.txt");
	}

	private void doCommit(String user, String message) throws Exception {
		doCommit(user, message, "dummy.txt");
	}

	private void doCommit(String user, String message, String commitFileName) throws Exception {
		File root = getRepository();
		File newFile = new File(root.getAbsolutePath(), commitFileName);
		assertTrue("Unable to create file to commit", newFile.createNewFile());

		addToRepository(newFile);

		HgRoot hgroot = new HgRoot(root.getAbsolutePath());
		List<File> files = new ArrayList<File>();
		files.add(newFile);
		HgCommitClient.commit(hgroot, files, user, message, false);
	}
}
