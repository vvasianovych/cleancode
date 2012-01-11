package com.keebraa.java.cleancode.core.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

import com.keebraa.java.cleancode.core.CleanCodeEngine;

public class CreateReviewAction implements IActionDelegate
{
    private IProject project;

    @Override
    public void run(IAction action)
    {
	if(project != null)
	    CleanCodeEngine.createCodeReview(project);
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
	IStructuredSelection structuredSelection = (IStructuredSelection) selection;
	project = (IProject) structuredSelection.getFirstElement();
    }
}
