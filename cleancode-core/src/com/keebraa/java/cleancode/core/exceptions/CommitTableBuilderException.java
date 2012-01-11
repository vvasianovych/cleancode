package com.keebraa.java.cleancode.core.exceptions;

public class CommitTableBuilderException extends RuntimeException
{
    private static final long serialVersionUID = -7665844621652443115L;

    public CommitTableBuilderException(String message)
    {
	super(message);
    }
}
