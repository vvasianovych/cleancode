package com.keebraa.java.cleancode.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.keebraa.java.cleancode.core.reviewcreation.wizard.ReviewCreationWizard;

public class Action1 implements IActionDelegate
{
    private IProject project;
    
    public Action1()
    {
    }

    @Override
    public void run(IAction action)
    {
	ReviewCreationWizard wizard = new ReviewCreationWizard();
	Shell sh = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        WizardDialog dialog = new WizardDialog(sh, wizard);
        dialog.open();    
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        project = (IProject) structuredSelection.getFirstElement();
    }
}
