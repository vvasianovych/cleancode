package com.keebraa.java.cleancode.repository;

import org.eclipse.core.resources.IProject;

import com.keebraa.java.cleancode.core.extensionpoints.ComitRepository;
import com.keebraa.java.cleancode.core.extensionpoints.ComitRepositoryFactory;

public class MercurialComitRepositoryFactory implements ComitRepositoryFactory
{
   @Override
   public ComitRepository createRepository(IProject project)
   {
	MercurialHistoryBuilder builder = new MercurialHistoryBuilder();
	builder.setProject(project);
	ComitRepository repository = new MercurialComitRepository(builder);
	return repository;
   }
}
