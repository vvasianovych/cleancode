package com.vectrace.MercurialEclipse.search;

import org.eclipse.jface.viewers.IContentProvider;

public interface IMercurialTextSearchContentProvider extends IContentProvider {

	void elementsChanged(Object[] updatedElements);

	void clear();

}