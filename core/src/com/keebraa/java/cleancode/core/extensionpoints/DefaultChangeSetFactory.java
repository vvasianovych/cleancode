package com.keebraa.java.cleancode.core.extensionpoints;

import java.util.List;

import org.eclipse.team.core.RepositoryProvider;

import com.keebraa.java.cleancode.core.model.Commit;

public class DefaultChangeSetFactory implements ChangeSetFactory
{
    @Override
    public List<Commit> getAllCommits()
    {
	return null;
    }

    @Override
    public boolean canHandle(RepositoryProvider provider)
    {
	return false;
    }

    @Override
    public String getRealizationName()
    {
	return "DEFAULT";
    }
}
