/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * zk	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.model.Branch;

/**
* @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 *
 */
public class HgBranchClientTest extends TestCase {
	public void testParseValidBranches() throws Exception {
		String[] lines = {
			"  default                    27860:6be4e6914439",
			"cb-5.4                     27832:33ab0c49de97",
			"cb-5.5.0                   27769:3c679028db6e",
			"cmr-5.4                    25057:13b6d1a4e4ce",
			"  cb-tracker-baselining      24953:3d176bf4dc2d",
			"cb-5.3                     24126:cd008bd788e9",
			"createreferringissue-feature 27521:3615d5cc7a6f (inactive)",
			"ehcache-conversion-branch  27378:fdb7771ddc89 (inactive)",
			"gravatar-feature           27273:347b8185f056     (inactive)",
			"milestone-feature          26734:b3332c14b4a4 (inactive)",
			"fop-1.0-experiment         26374:2bb9b137ae89 (inactive)",
			"cb-5.4.2-AUO               24622:a294bf6d8ce4 (inactive)",
			"mr-access-feature          23693:a2f11c74e8b8 (inactive)   ",
			"rebase keepbranches         2139:a73da2849655 (inactive)"
		};

		for (String line : lines) {
			Branch branch = HgBranchClient.parseBranch(line);

			assertTrue(branch.getName().trim().length() > 0);
			assertEquals(branch.getName().trim(), branch.getName());
			assertTrue(branch.getGlobalId().trim().length() > 0);
			assertEquals(branch.getGlobalId().trim(), branch.getGlobalId());
		}
	}

	public void testParseBranch() throws Exception {
		Branch branch = HgBranchClient.parseBranch("   a test                         3:066ee3f79d2a");

		assertEquals("a test", branch.getName());
		assertEquals(3, branch.getRevision());
		assertEquals("066ee3f79d2a", branch.getGlobalId());
		assertTrue(branch.isActive());

		branch = HgBranchClient.parseBranch("*                              2:5a953790aa12 (inactive)");

		assertEquals("*", branch.getName());
		assertEquals(2, branch.getRevision());
		assertEquals("5a953790aa12", branch.getGlobalId());
		assertFalse(branch.isActive());

		branch = HgBranchClient.parseBranch("default                        0:fd83cc49d230 (inactive)");

		assertEquals("default", branch.getName());
		assertEquals(0, branch.getRevision());
		assertEquals("fd83cc49d230", branch.getGlobalId());
		assertFalse(branch.isActive());

		branch = HgBranchClient.parseBranch("rebase keepbranches         2139:a73da2849655 (inactive)   ");

		assertEquals("rebase keepbranches", branch.getName());
		assertEquals(2139, branch.getRevision());
		assertEquals("a73da2849655", branch.getGlobalId());
		assertFalse(branch.isActive());

		branch = HgBranchClient.parseBranch("rebase keepbranches");
		assertNull(branch);

		branch = HgBranchClient.parseBranch("2139:a73da2849655");
		assertNull(branch);
	}
}
