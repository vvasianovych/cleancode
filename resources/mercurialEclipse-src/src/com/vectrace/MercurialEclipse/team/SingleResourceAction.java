package com.vectrace.MercurialEclipse.team;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;

import com.vectrace.MercurialEclipse.MercurialEclipsePlugin;

/**
 *
 * @author Jerome Negre <jerome+hg@jnegre.org>
 *
 */
public abstract class SingleResourceAction implements IActionDelegate {

	private IResource selection;

	public SingleResourceAction() {
		super();
	}

	public void selectionChanged(IAction action, ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			//the xml enables this action only for a selection of a single resource
			this.selection = (IResource) ((IStructuredSelection) sel).getFirstElement();
		}
	}

	protected Shell getShell() {
		return MercurialEclipsePlugin.getActiveShell();
	}

	protected IResource getSelectedResource() {
		return selection;
	}

	public void run(IAction action) {
		try {
			run(getSelectedResource());
		} catch (Exception e) {
			MercurialEclipsePlugin.logError(e);
			MessageDialog.openError(getShell(), Messages.getString("SingleResourceAction.hgSays"), e.getMessage()+Messages.getString("SingleResourceAction.seeErrorLog")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected abstract void run(IResource resource) throws Exception;
}