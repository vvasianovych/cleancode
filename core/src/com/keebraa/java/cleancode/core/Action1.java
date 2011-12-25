package com.keebraa.java.cleancode.core;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.IActionDelegate;

import com.vectrace.MercurialEclipse.history.MercurialHistory;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;
import com.vectrace.MercurialEclipse.utils.ResourceUtils;

public class Action1 implements IActionDelegate
{
    private IProject project;
    
    public Action1()
    {
    }

    @Override
    public void run(IAction action)
    {
//        RepositoryProvider provider = RepositoryProvider.getProvider(project);
//        MercurialTeamProvider mProvider = (MercurialTeamProvider) provider;
//        MercurialHistory history = (MercurialHistory)mProvider.getFileHistoryProvider().getFileHistoryFor(project, IFileHistoryProvider.NONE, null);
//        List<MercurialRevision> revisions = history.getRevisions();
//        IFileHistoryProvider historyProvider = provider.getFileHistoryProvider();
//        System.out.println("========================================revisions=============================================");
//        System.out.println("========================================end revisions=============================================");
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection)
    {
        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        project = (IProject) structuredSelection.getFirstElement();
    }
}
