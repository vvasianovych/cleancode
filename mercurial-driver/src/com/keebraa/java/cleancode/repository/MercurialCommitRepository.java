package com.keebraa.java.cleancode.repository;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

import com.keebraa.java.cleancode.core.extensionpoints.CommitRepository;
import com.keebraa.java.cleancode.core.model.Commit;
import com.vectrace.MercurialEclipse.history.MercurialHistory;
import com.vectrace.MercurialEclipse.history.MercurialRevision;

public class MercurialCommitRepository implements CommitRepository
{

    public MercurialCommitRepository()
    {
    }

    @Override
    public boolean canHandle(RepositoryProvider provider)
    {
        return true;
    }

    @Override
    public List<Commit> getAllCommits(IProject project)
    {
        MercurialHistory history = getHistory(project);
        CommitBuilder builder = new CommitBuilder();
        List<Commit> commits = new ArrayList<Commit>(history.getRevisions()
                .size());
        for (MercurialRevision revision : history.getRevisions())
        {
            Commit commit = builder.build(revision);
            commits.add(commit);
        }
        return commits;
    }

    @Override
    public String getRealizationName()
    {
        return "MERCURIAL REALIZATION";
    }

    private MercurialHistory getHistory(IProject project)
    {
        MercurialHistory history = new MercurialHistory(project);
        try
        {
            history.refresh(null, Integer.MAX_VALUE);
        }
        catch (CoreException e)
        {
            e.printStackTrace();
        }
        return history;
    }
}
