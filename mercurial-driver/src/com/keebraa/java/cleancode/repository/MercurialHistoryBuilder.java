package com.keebraa.java.cleancode.repository;

import org.eclipse.core.resources.IProject;

import com.vectrace.MercurialEclipse.history.MercurialHistory;

public class MercurialHistoryBuilder
{
   private IProject project;
   
   public void setProject(IProject project)
   {
	this.project = project;
   }
   public MercurialHistory build()
   {
	return new MercurialHistory(project);
   }
}
