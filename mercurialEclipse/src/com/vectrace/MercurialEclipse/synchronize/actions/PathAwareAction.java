/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * John Peberdy	implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.synchronize.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelAction;

import com.vectrace.MercurialEclipse.model.PathFromChangeSet;

/**
 * An action that can operate on folder nodes in "compressed tree" or "tree" mode in the synchronize
 * view.
 *
 * @see PathFromChangeSet
 */
public abstract class PathAwareAction extends SynchronizeModelAction {

	public PathAwareAction(String text, ISynchronizePageConfiguration configuration,
			ISelectionProvider selectionProvider) {
		super(text, configuration, selectionProvider);
	}

	// operations

	/**
	 * @see org.eclipse.ui.actions.BaseSelectionListenerAction#getStructuredSelection()
	 * @deprecated Use {@link #getNormalizedSelection()}
	 */
	@Override
	@Deprecated
	public IStructuredSelection getStructuredSelection() {
		return super.getStructuredSelection();
	}

	/**
	 * @return Selected items with tree nodes replaced with their children.
	 */
	protected Object[] getNormalizedSelection() {
		return normalize(super.getStructuredSelection().toArray());
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		// TODO: why do we calculate this only if parent says it should be disabled?
		if (!super.updateSelection(selection)) {
			Object[] array = normalize(selection.toArray());
			for (Object object : array) {
				if (!isSupported(object)) {
					return false;
				}
			}
			return true;
		}
		return true;
	}

	/**
	 * Whether the action should be enabled for this object.
	 *
	 * @param object
	 *            The object to check. Note: Will never be a PathFromChangeSet.
	 * @return False to disable the action
	 */
	protected abstract boolean isSupported(Object object);

	/**
	 * Depending on the synchronize presentation mode there may be {@link PathFromChangeSet}s
	 * present in selection, but actions need not operate on them. This method replaces such path
	 * nodes with their children.
	 *
	 * @param selection
	 *            The selection to normalize. May be null.
	 * @return Normalized copy of selection. Not null.
	 */
	public static Object[] normalize(Object[] selection) {
		if (selection != null) {
			return normalize(selection, new ArrayList<Object>(selection.length)).toArray();
		}

		return new Object[0];
	}

	/**
	 * @param selection
	 *            Selection to normalize
	 * @param l
	 *            (Output) All the elements of selection are added to this, replacing path nodes
	 *            with their children
	 * @return l
	 */
	private static List<Object> normalize(Object[] selection, List<Object> l) {
		for (int i = 0; i < selection.length; i++) {
			if (selection[i] instanceof PathFromChangeSet) {
				normalize(((PathFromChangeSet) selection[i]).getChildren(), l);
			} else {
				l.add(selection[i]);
			}
		}

		return l;
	}
}
