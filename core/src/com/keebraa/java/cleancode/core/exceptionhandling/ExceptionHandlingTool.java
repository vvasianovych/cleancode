package com.keebraa.java.cleancode.core.exceptionhandling;

import java.util.HashMap;
import java.util.Map;

/**
 * This class will handle all exceptions in the system, that are not handled in
 * the other code.
 * 
 * @author taqi
 * 
 */
public class ExceptionHandlingTool
{
    private static ExceptionHandlingTool singleton;

    private Map<Throwable, ExceptionHandler> handlers;
    
    private ExceptionHandlingTool()
    {
	handlers = new HashMap<Throwable, ExceptionHandler>();
    }

    public static ExceptionHandlingTool getInstance()
    {
	if (singleton == null)
	{
	    singleton = new ExceptionHandlingTool();
	}
	return singleton;
    }
    
    public void handleException(Throwable exception)
    {
	ExceptionHandler handler = handlers.get(exception);
	if(handler == null)
	{
	    throw new RuntimeException(this.getClass().getName()+":: there is no handler for exception: "+exception.getClass().getCanonicalName());
	}
	handler.handle(exception);
    }
}
