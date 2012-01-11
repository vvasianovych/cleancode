package com.keebraa.java.cleancode.core.extensionpoints;

import com.keebraa.java.cleancode.core.actions.CreateReviewAction;
import com.keebraa.java.cleancode.core.exceptions.CommitRepositoryNotFoundException;

/**
 * This class provides realizations of commitRepositories from
 * "com.keebraa.java.cleancode.core.commitRepository" extension point.
 * 
 * @see CreateReviewAction
 * @author taqi
 * 
 */
public class CommitRepositoryProvider
{
    private static final String COMMITREPOSITORY_POINTNAME = "com.keebraa.java.cleancode.core.commitRepository";
    private static final String REPOSITORY_ATTRIBUTE = "repository";

    public CommitRepository getCommitRepository() throws CommitRepositoryNotFoundException
    {
	CommitRepository factory = ExtensionPointsUtil.getUniqueExtensionPointRealization(COMMITREPOSITORY_POINTNAME,
		REPOSITORY_ATTRIBUTE, CommitRepository.class);
	return factory;
    }
}
