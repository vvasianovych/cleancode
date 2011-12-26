/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrei Loskutov - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.utils;

public final class Bits {
	private Bits(){

	}

	/**
	 * @return true if at least one of given bits is set in the source
	 */
	public static boolean contains(int source, int bits) {
		return (source & bits) != 0;
	}

	/**
	 * @return true if ALL given bits are set in the source
	 */
	public static boolean matchesAll(int source, int bits) {
		return (source & bits) == bits;
	}

	public static int clear(int source, int bit) {
		return source & ~bit;
	}


	public static int highestBit(int source) {
		return Integer.highestOneBit(source);
	}

	public static int cardinality(int source) {
		return Integer.bitCount(source);
	}

}