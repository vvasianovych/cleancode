package com.keebraa.java.cleancode.core;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;


public class HelloTeamAction implements IActionDelegate
{

    @Override
    public void run(IAction action)
    {
        System.out.println("HELLO WORLD");
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
        
    }
}
