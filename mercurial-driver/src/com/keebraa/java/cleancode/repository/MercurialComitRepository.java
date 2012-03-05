package com.keebraa.java.cleancode.repository;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

import com.keebraa.java.cleancode.core.extensionpoints.ComitRepository;
import com.keebraa.java.cleancode.core.model.Comit;
import com.vectrace.MercurialEclipse.history.MercurialHistory;
import com.vectrace.MercurialEclipse.history.MercurialRevision;

public class MercurialComitRepository implements ComitRepository
{
   private IProject project;
   
   private MercurialHistoryBuilder historyBuilder;

   public MercurialComitRepository(IProject project, MercurialHistoryBuilder historyBuilder)
   {
	this.project = project;
	this.historyBuilder = historyBuilder;
   }

   @Override
   public boolean canHandle(RepositoryProvider provider)
   {
	return true;
   }

   @Override
   public List<Comit> getAllCommits()
   {
	MercurialHistory history = getHistory(project, Integer.MAX_VALUE);
	ComitBuilder builder = new ComitBuilder();
	List<Comit> commits = new ArrayList<Comit>(history.getRevisions().size());
	for (MercurialRevision revision : history.getRevisions())
	{
	   Comit commit = builder.build(revision);
	   commits.add(commit);
	}
	return commits;
   }

   @Override
   public String getRealizationName()
   {
	return "MERCURIAL REALIZATION";
   }

   private MercurialHistory getHistory(IProject project, int from)
   {
	MercurialHistory history = new MercurialHistory(project);
	try
	{
	   history.refresh(null, from);
	}
	catch (CoreException e)
	{
	   e.printStackTrace();
	}
	return history;
   }

   @Override
   public Comit getBefore(Comit comit)
   {
	int revisionNumber = comit.getRevision();
	MercurialHistory history = getHistory(project, revisionNumber);
	for (MercurialRevision revision : history.getRevisions())
	{
	}
	return null;
   }
}
