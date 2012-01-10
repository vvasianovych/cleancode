package com.keebraa.java.cleancode.core.exceptions;

import org.eclipse.core.runtime.CoreException;

public class ChangeSetFactoryNotFoundException extends Throwable
{
    private static final long serialVersionUID = 4843223252320325798L;
    
    public ChangeSetFactoryNotFoundException()
    {
	super("ChangeSetFactory realization has not been found");
    }
    
    public ChangeSetFactoryNotFoundException(CoreException exception)
    {
	super("ChangeSetFactory realization has not been found", exception);
    }
}
