package com.keebraa.java.cleancode.mercurialFactory;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.team.core.RepositoryProvider;

import com.keebraa.java.cleancode.core.extensionpoints.ChangeSetFactory;
import com.keebraa.java.cleancode.core.model.Commit;
import com.keebraa.java.cleancode.core.model.CommitFile;

public class MercurialChangeSetFactory implements ChangeSetFactory
{

    public MercurialChangeSetFactory()
    {
    }

    @Override
    public boolean canHandle(RepositoryProvider provider)
    {
	return true;
    }

    @Override
    public List<Commit> getAllCommits()
    {
	List<Commit> commits = new ArrayList<Commit>();
	commits.add(new Commit(new ArrayList<CommitFile>(), "111111", "aaaaaa"));
	return commits;
    }

    @Override
    public String getRealizationName()
    {
	return "MERCURIAL REALIZATION";
    }

}