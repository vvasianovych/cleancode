package com.keebraa.java.cleancode.core;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionDelegate;

import com.vectrace.MercurialEclipse.commands.HgLogClient;
import com.vectrace.MercurialEclipse.history.MercurialHistory;
import com.vectrace.MercurialEclipse.history.MercurialRevision;
import com.vectrace.MercurialEclipse.model.FileFromChangeSet;
import com.vectrace.MercurialEclipse.team.MercurialTeamProvider;

public class Action1 implements IActionDelegate
{
    private IProject project;
    
    public Action1()
    {
    }

    @Override
    public void run(IAction action)
    {
	MercurialHistory history = new MercurialHistory(project);
	try
	{
	    history.refresh(null, Integer.MAX_VALUE);
	    List<MercurialRevision> revisions = history.getRevisions();
	    
	    for(MercurialRevision revision : revisions)
	    {
		System.out.println("=========revision : "+revision.getChangeSet().getChangeset()+"============");
		System.out.println(HgLogClient.getLogWithBranchInfo(revision, history, null).getChangesetFiles().length);
		for(FileFromChangeSet file : HgLogClient.getLogWithBranchInfo(revision, history, null).getChangesetFiles())
		{
		    System.out.println(file.getFile().getName());
		}
		System.out.println("===============================================================");
	    }
	}
	catch (CoreException e)
	{
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
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
