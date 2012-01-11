package com.keebraa.java.cleancode.core.exceptions;

public class CodeReviewCreationException extends Throwable
{
    private static final long serialVersionUID = 3656488349850780201L;

    public CodeReviewCreationException()
    {
	super("There are some problems during codeReview creation");
    }
}
