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
package com.vectrace.MercurialEclipse.synchronize;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;
import com.vectrace.MercurialEclipse.preferences.MercurialPreferenceConstants;

/**
 * @author johnpeb
 */
public enum PresentationMode {
	FLAT(Messages.getString("PresentationMode.Flat")), //
	TREE(Messages.getString("PresentationMode.Tree")), //
	COMPRESSED_TREE(Messages.getString("PresentationMode.CompressedTree"));

	public static final String PREFERENCE_KEY = MercurialPreferenceConstants.PREF_SYNC_PRESENTATION_MODE;

	private final String localized;

	PresentationMode(String localized) {
		if (localized == null) {
			throw new IllegalStateException();
		}
		this.localized = localized;
	}

	@Override
	public String toString() {
		return localized;
	}

	public static PresentationMode get() {
		String name = MercurialEclipsePlugin.getDefault().getPreferenceStore().getString(PREFERENCE_KEY);

		if (name != null) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException t) {
			}
		}

		return FLAT;
	}

	public void set() {
		MercurialEclipsePlugin.getDefault().getPreferenceStore().setValue(PREFERENCE_KEY, name());
	}

	public boolean isSet() {
		return get() == this;
	}
}
