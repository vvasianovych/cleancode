package com.keebraa.java.cleancode.repository;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

import com.keebraa.java.cleancode.core.extensionpoints.ComitRepository;
import com.keebraa.java.cleancode.core.model.Comit;
import com.vectrace.MercurialEclipse.history.MercurialHistory;
import com.vectrace.MercurialEclipse.history.MercurialRevision;

public class MercurialComitRepository implements ComitRepository
{
   private MercurialHistoryBuilder historyBuilder;
   
   private ComitBuilder comitBuilder;

   public MercurialComitRepository(MercurialHistoryBuilder historyBuilder)
   {
	this.historyBuilder = historyBuilder;
	this.comitBuilder = new ComitBuilder();
   }

   @Override
   public boolean canHandle(RepositoryProvider provider)
   {
	return true;
   }

   @Override
   public List<Comit> getAllCommits()
   {
	MercurialHistory history = getHistory(Integer.MAX_VALUE);
	List<Comit> commits = new ArrayList<Comit>(history.getRevisions().size());
	for (MercurialRevision revision : history.getRevisions())
	{
	   Comit commit = comitBuilder.build(revision);
	   commits.add(commit);
	}
	return commits;
   }

   @Override
   public String getRealizationName()
   {
	return "MERCURIAL REALIZATION";
   }

   private MercurialHistory getHistory(int from)
   {
	MercurialHistory history = historyBuilder.build();
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

   //TODO: refactor this ugly code. Avoid such decrements, and so on... 
   @Override
   public Comit getBefore(Comit comit)
   {
	Comit result = null;
	int revisionNumber = comit.getRevision();
	MercurialHistory history = getHistory(revisionNumber);
	List<MercurialRevision> revisions = history.getRevisions(); 
	if(revisions.size() > 0 && revisions.contains(comit))
	{
	   int index = revisions.indexOf(comit);
	   index--;
	   MercurialRevision revision = revisions.get(index);
	   result = comitBuilder.build(revision);
	}
	return result;
   }
}
