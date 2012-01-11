package com.keebraa.java.cleancode.core.exceptions;

import org.eclipse.core.runtime.CoreException;

public class CommitRepositoryNotFoundException extends Throwable
{
    private static final long serialVersionUID = 4843223252320325798L;
    
    public CommitRepositoryNotFoundException()
    {
	super("CommitRepository realization has not been found");
    }
    
    public CommitRepositoryNotFoundException(CoreException exception)
    {
	super("CommitRepository realization has not been found", exception);
    }
}
