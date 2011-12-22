/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * zk	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

import junit.framework.TestCase;

/**
 * @author Zsolt Koppany <zsolt.koppany@intland.com>
 */
public class StringUtilsTests extends TestCase {
	public void testRemoveLineBreaks() {
		assertEquals("", StringUtils.removeLineBreaks("\r\n"));
		assertEquals("", StringUtils.removeLineBreaks("  \r\n  "));
		assertEquals("", StringUtils.removeLineBreaks("\r\n  "));
		assertEquals("", StringUtils.removeLineBreaks("  \r\n"));

		assertEquals("abc def", StringUtils.removeLineBreaks("abc\r\ndef"));
		assertEquals("abc def", StringUtils.removeLineBreaks("abc\r\n  def"));
		assertEquals("abc def", StringUtils.removeLineBreaks("abc \r\n  def"));
		assertEquals("abc def", StringUtils.removeLineBreaks("abc\t \r\n  def"));
		assertEquals("hello abcdef", StringUtils.removeLineBreaks("hello\rabcdef"));
		assertEquals("hello abcdef", StringUtils.removeLineBreaks("hello\nabcdef"));
	}
}
