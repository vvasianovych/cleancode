package com.keebraa.java.cleancode.core.extensionpoints;

import org.eclipse.core.resources.IProject;

import com.keebraa.java.cleancode.core.actions.CreateReviewAction;
import com.keebraa.java.cleancode.core.exceptions.CommitRepositoryFactoryNotFoundException;

/**
 * This class provides realizations of commitRepositories from
 * "com.keebraa.java.cleancode.core.commitRepository" extension point.
 * 
 * @see CreateReviewAction
 * @author taqi
 * 
 */
public class ComitRepositoryProvider
{
    private static final String COMMITREPOSITORY_POINTNAME = "com.keebraa.java.cleancode.core.comitRepositoryFactory";
    private static final String REPOSITORY_ATTRIBUTE = "factory";

    public static ComitRepository getCommitRepository(IProject project) throws CommitRepositoryFactoryNotFoundException
    {
	ComitRepositoryFactory factory = ExtensionPointsUtil.getUniqueExtensionPointRealization(COMMITREPOSITORY_POINTNAME,
		REPOSITORY_ATTRIBUTE, ComitRepositoryFactory.class);
	ComitRepository repository = factory.createRepository(project);
	return repository;
    }
}
