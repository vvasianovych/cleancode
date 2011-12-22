/*******************************************************************************
 * Copyright (c) 2005-2010 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * 		Andrei Loskutov			- implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.properties;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.team.ui.history.IHistoryView;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.vectrace.MercurialEclipse.history.MercurialHistoryPage;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.ChangeSet;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.model.FileStatus;
import com.vectrace.MercurialEclipse.model.Tag;
import com.vectrace.MercurialEclipse.synchronize.cs.ChangesetGroup;

/**
 * Factory adapting our model for Property page
 *
 * @author andrei
 */
public class PropertyPageAdapterFactory implements IAdapterFactory {

	/* Disabled tabbed property view support - right now this is overkill.
	 * see also plugin.xml
	private static class MercurialPropertySheetPage extends TabbedPropertySheetPage {

		static ITabbedPropertySheetPageContributor contributor = new ITabbedPropertySheetPageContributor() {
			public String getContributorId() {
				return MercurialHistoryPage.class.getName();
			}
		};

		public MercurialPropertySheetPage() {
			super(contributor);
		}

	}
	*/

	public Object getAdapter(Object adaptable, Class adapterType) {
		if (adapterType == IPropertySource.class) {
			if (adaptable instanceof MercurialRevision
					|| adaptable instanceof FileStatus
					|| adaptable instanceof FileFromChangeSet
					|| adaptable instanceof ChangeSet
					|| adaptable instanceof ChangesetGroup
					|| adaptable instanceof Tag) {
				return new GenericPropertySource(adaptable);
			}
		}
		if (adapterType == IPropertySheetPage.class) {
			if (adaptable instanceof IHistoryView
					&& ((IHistoryView) adaptable).getHistoryPage() instanceof MercurialHistoryPage) {
				return new /*Mercurial*/PropertySheetPage();
			}
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] { IPropertySource.class, IPropertySheetPage.class };
	}

}
