/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package com.vectrace.MercurialEclipse.commands;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import com.vectrace.MercurialEclipse.HgRevision;
import com.vectrace.MercurialEclipse.model.Tag;

/**
 * @author <a href="mailto:zsolt.koppany@intland.com">Zsolt Koppany</a>
 * @version $Id$
 */
public class HgTagClientTests extends TestCase {
	public void testGetTags1() throws Exception {
		String[] lines = {
			"cb-3-6-RC1  7144:3e28ba45c18e",
			"end_of_metrics_branch  7117:db9d060ed00f",
			"cb-3-5-RC   6472:37e681b9c47b",
			"CB-3-5-RC   6472:37e681b9c47b",
			"original   5999:fc532a302004",
			"tip    23323:e374027944fb",
			"Root_eclipse-2-1   5601:8de90d654b96",
		};

		Collection<Tag> tags = HgTagClient.getTags(null, lines);
		Iterator<Tag> it = tags.iterator();

		Tag tag = it.next();
		assertEquals(HgRevision.TIP.getChangeset(), tag.getName());

		tag = it.next();
		assertEquals("CB-3-5-RC", tag.getName());

		tag = it.next();
		assertEquals("cb-3-5-RC", tag.getName());

		tag = it.next();
		assertEquals("cb-3-6-RC1", tag.getName());

		tag = it.next();
		assertEquals("end_of_metrics_branch", tag.getName());

		tag = it.next();
		assertEquals("original", tag.getName());

		tag = it.next();
		assertEquals("Root_eclipse-2-1", tag.getName());

		assertEquals(lines.length, tags.size());
	}

	public void testGetTags2() throws Exception {
		String[] lines = {
			"tip                             1377:288718b86ade",
			"release_1.2                      941:c3db62d68609",
			"RELEASE_1.2                      941:c3db62d68609",
			"RELEASE_1.4                     1280:0fedfb9cf182",
			"RELEASE_1.3                     1019:758c1089d4f0",
			"RELEASE_1.1                      867:5dbd10536a7d",
			"RELEASE_1.0                      741:e97205e404e7",
			"RELEASE_0.2                      239:4b5fcdf8128f",
			"RELEASE_0.1                       40:dde832884afc"
		};

		Collection<Tag> tags = HgTagClient.getTags(null, lines);
		Iterator<Tag> it = tags.iterator();

		Tag tag = it.next();
		assertEquals(HgRevision.TIP.getChangeset(), tag.getName());

		tag = it.next();
		assertEquals("RELEASE_0.1", tag.getName());

		tag = it.next();
		assertEquals("RELEASE_0.2", tag.getName());

		tag = it.next();
		assertEquals("RELEASE_1.0", tag.getName());

		tag = it.next();
		assertEquals("RELEASE_1.1", tag.getName());

		tag = it.next();
		assertEquals("RELEASE_1.2", tag.getName());

		tag = it.next();
		assertEquals("release_1.2", tag.getName());

		tag = it.next();
		assertEquals("RELEASE_1.3", tag.getName());
	}
}
