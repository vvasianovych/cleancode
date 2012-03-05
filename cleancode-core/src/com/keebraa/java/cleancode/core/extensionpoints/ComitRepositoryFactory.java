package com.keebraa.java.cleancode.core.extensionpoints;

import org.eclipse.core.resources.IProject;

public interface ComitRepositoryFactory
{
   public ComitRepository createRepository(IProject project);
}
