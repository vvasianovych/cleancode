/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Philip Graf   implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.model.ChangeSet;

/**
 * Tests for {@link ChangeSetUtils}.
 *
 * @author Philip Graf
 */
public class ChangeSetUtilsTest extends TestCase {

	/**
	 * Tests for {@link ChangeSetUtils#getPrintableTagsString(ChangeSet)}.
	 */
	public void testGetPrintableTagsString() {
		assertEquals("no changeset", "", ChangeSetUtils.getPrintableTagsString(null));

		ChangeSet changeSet = new ChangeSet.Builder(0, "a", "b", "c", "d", null).build();
		assertEquals("changeset without tags", "", ChangeSetUtils.getPrintableTagsString(changeSet));

		changeSet = new ChangeSet.Builder(0, "a", "b", "c", "d", null).tags("tag1").build();
		assertEquals("changeset with one tag", "tag1", ChangeSetUtils.getPrintableTagsString(changeSet));

		changeSet = new ChangeSet.Builder(0, "a", "b", "c", "d", null).tags("tag1_,_tag2").build();
		assertEquals("changeset with two tags", "tag1, tag2", ChangeSetUtils.getPrintableTagsString(changeSet));

		changeSet = new ChangeSet.Builder(0, "a", "b", "c", "d", null).tags("tag1_,_tag2_,_tag3").build();
		assertEquals("changeset with three tags", "tag1, tag2, tag3", ChangeSetUtils.getPrintableTagsString(changeSet));
	}

	/**
	 * Tests for {@link ChangeSetUtils#getPrintableRevisionShort(ChangeSet)}.
	 */
	public void testGetPrintableRevisionShort() {
		assertEquals("no changeset", "", ChangeSetUtils.getPrintableRevisionShort(null));

		ChangeSet changeSet = new ChangeSet.Builder(1, "a", "b", "c", "d", null).build();
		assertEquals("changeset without short revision", "1", ChangeSetUtils.getPrintableRevisionShort(changeSet));

		changeSet = new ChangeSet.Builder(1, "a", "b", "c", "d", null).nodeShort("cafebeef").build();
		assertEquals("changeset with short revision", "1:cafebeef", ChangeSetUtils.getPrintableRevisionShort(changeSet));
	}
}
