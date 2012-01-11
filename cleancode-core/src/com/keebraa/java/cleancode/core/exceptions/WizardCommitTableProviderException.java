package com.keebraa.java.cleancode.core.exceptions;

public class WizardCommitTableProviderException extends RuntimeException
{
    private static final long serialVersionUID = 6556217953367324517L;
    public static final String CAST_EXCEPTION = "element in the collection is not the commit object!";

    public WizardCommitTableProviderException(String message)
    {
	super(message);
    }
}
