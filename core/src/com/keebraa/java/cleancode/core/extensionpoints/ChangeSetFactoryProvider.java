package com.keebraa.java.cleancode.core.extensionpoints;

import com.keebraa.java.cleancode.core.actions.CreateReviewAction;
import com.keebraa.java.cleancode.core.exceptions.ChangeSetFactoryNotFoundException;

/**
 * This class provides realizations of changeSetFactories from
 * "com.keebraa.java.cleancode.core.changeSetFactory" extension point.
 * 
 * @see CreateReviewAction
 * @author taqi
 * 
 */
public class ChangeSetFactoryProvider
{
    private static final String CHANGESETFACTORY_POINTNAME = "com.keebraa.java.cleancode.core.changeSetFactory";
    private static final String FACTORY_ATTRIBUTE = "factory";
    
    public ChangeSetFactory getChangeSetFactory() throws ChangeSetFactoryNotFoundException
    {
	ChangeSetFactory factory = ExtensionPointsUtil.getUniqueExtensionPointRealization(CHANGESETFACTORY_POINTNAME, FACTORY_ATTRIBUTE, ChangeSetFactory.class);
	return factory;
    }
}
