/*******************************************************************************
 * Copyright (c) 2005-2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * bastian	implementation
 *     Andrei Loskutov - bug fixes
 *******************************************************************************/
package com.vectrace.MercurialEclipse.team.cache;

import org.eclipse.osgi.util.NLS;

/**
 * @author bastian
 *
 */
public final class Messages extends NLS {
	private static final String BUNDLE_NAME = "com.vectrace.MercurialEclipse.team.cache.messages"; //$NON-NLS-1$

	private Messages() {
	}

	public static String localChangesetCache_LogLimitLessThanZero;
	public static String localChangesetCache_LogLimitNotCorrectlyConfigured;
	public static String mercurialStatusCache_autoshare;
	public static String mercurialStatusCache_BatchSizeForStatusCommandNotCorrect;
	public static String mercurialStatusCache_FailedToRefreshMergeStatus;
	public static String mercurialStatusCache_Refreshing;
	public static String mercurialStatusCache_RefreshingProject;
	public static String mercurialStatusCache_RefreshingResources;
	public static String mercurialStatusCache_RefreshStatus;
	public static String mercurialStatusCache_UnknownStatus;
	public static String refreshJob_LoadingIncomingRevisions;
	public static String refreshJob_LoadingLocalRevisions;
	public static String refreshJob_LoadingOutgoingRevisionsFor;
	public static String refreshJob_UpdatingStatusAndVersionCache;
	public static String refreshStatusJob_OptainingMercurialStatusInformation;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

}
