/*******************************************************************************
 * Copyright (c) 2005-2009 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.history;

import org.eclipse.osgi.util.NLS;

/**
 * @author Bastian
 *
 */
public final class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.vectrace.MercurialEclipse.history.messages"; //$NON-NLS-1$
	public static String BisectAbstractAction_bisecting;
	public static String BisectAbstractAction_BisectionResult;
	public static String BisectAbstractAction_showBisectionResult;
	public static String BisectAbstractAction_successString;
	public static String BisectMarkBadAction_description1;
	public static String BisectMarkBadAction_description2;
	public static String BisectMarkBadAction_name;
	public static String BisectMarkGoodAction_markSelectionAsGood;
	public static String BisectMarkGoodAction_markSelectionDescription;
	public static String BisectMarkGoodAction_markSelectionDescription2;
	public static String BisectResetAction_description;
	public static String BisectResetAction_description2;
	public static String BisectResetAction_name;
	public static String ChangePathsTableProvider_retrievingAffectedPaths;
	public static String CompareRevisionAction_retrievingDiffData;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
