package com.keebraa.java.cleancode.core.exceptions;

import org.eclipse.core.runtime.CoreException;

public class CommitRepositoryFactoryNotFoundException extends Throwable
{
    private static final long serialVersionUID = 4843223252320325798L;
    
    public CommitRepositoryFactoryNotFoundException()
    {
	super("CommitRepositoryFactory realization has not been found");
    }
    
    public CommitRepositoryFactoryNotFoundException(CoreException exception)
    {
	super("CommitRepositoryFactory realization has not been found", exception);
    }
}
