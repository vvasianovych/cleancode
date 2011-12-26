/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bastian	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.search;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.core.text.TextSearchScope;

/**
 * @author Bastian
 *
 */
public class MercurialTextSearchEngine extends TextSearchEngine {
	public MercurialTextSearchEngine() {
	}

	@Override
	public IStatus search(TextSearchScope scope, TextSearchRequestor requestor,
			Pattern searchPattern, IProgressMonitor monitor) {
		// do all the work in the visitor
		return new MercurialTextSearchVisitor(requestor, searchPattern).search(
				(MercurialTextSearchScope) scope, monitor);
	}

	@Override
	public IStatus search(IFile[] scope, TextSearchRequestor requestor,
			Pattern searchPattern, IProgressMonitor monitor) {
		// do all the work in the visitor
		return new MercurialTextSearchVisitor(requestor, searchPattern).search(
				scope, monitor);
	}

}
