package com.vectrace.MercurialEclipse.ui;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.vectrace.MercurialEclipse.dialogs.CommitResource;


public class CommittableFilesFilter extends ViewerFilter {
	public CommittableFilesFilter() {
		super();
	}

	/**
	 * Filter out un commitable files (i.e. ! -> deleted but still tracked)
	 */
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof CommitResource) {
			return true;
		}
		return true;
	}
}
