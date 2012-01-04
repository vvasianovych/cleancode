package com.keebraa.java.cleancode.core.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;

import com.keebraa.java.cleancode.core.extensionpoints.ChangeSetFactory;

public class CreateReviewAction implements IActionDelegate
{
    private static final String CHANGESETFACTORY_POINTNAME = "com.keebraa.java.cleancode.core.changeSetFactory";

    @Override
    public void run(IAction action)
    {
	IConfigurationElement[] configElements = Platform.getExtensionRegistry()
		.getConfigurationElementsFor(CHANGESETFACTORY_POINTNAME);
	for (IConfigurationElement element : configElements)
	{
	    try
	    {
		final Object o = element.createExecutableExtension("factory");
		if (o instanceof ChangeSetFactory)
		    {
			ChangeSetFactory factory = (ChangeSetFactory) o;
			System.out.println(factory.getRealizationName());
		    }
	    }
	    catch (CoreException e)
	    {
		e.printStackTrace();
	    }
	}
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {

    }
}
