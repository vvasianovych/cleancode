/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.annotations;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import junit.framework.TestCase;

public class AnnotateCommandTest extends TestCase {
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH);
	static {
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	private List<AnnotateBlock> createFromStdOut(String name) {
		return AnnotateCommand.createFromStdOut(
				getClass().getResourceAsStream(name + ".out")).getAnnotateBlocks();
	}

	public void test1() {
		List<AnnotateBlock> blocks = createFromStdOut("annotate1");
		assertEquals(48, blocks.size());

		checkBlock(blocks, 0, "zingo", "146:16cd70529433", "Tue Feb 05 20:17:52 2008 +0000", 0, 0);
		checkBlock(blocks, 1, "zingo", "151:893d61d581c6", "Thu Mar 27 21:47:06 2008 +0000", 1, 5);
	}

	private void checkBlock(List<AnnotateBlock> blocks, int i, String user, String rev, String date, int startLine, int endLine){
		AnnotateBlock block = blocks.get(i);
		assertEquals(user, block.getUser());
		assertEquals(rev, block.getRevision().toString());
		assertEquals(startLine, block.getStartLine());
		assertEquals(endLine, block.getEndLine());
		assertEquals(date, DATE_FORMAT.format(block.getDate()));
	}
}
