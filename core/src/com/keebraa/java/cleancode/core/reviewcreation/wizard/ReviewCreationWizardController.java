package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.core.resources.IProject;

import com.keebraa.java.cleancode.core.extensionpoints.ChangeSetFactory;
import com.keebraa.java.cleancode.core.model.CodeReview;

/**
 * Controller is responsible for creation of the {@link CodeReview}.
 * @author taqi
 *
 */
public class ReviewCreationWizardController
{
    private IProject project;
    
    private ChangeSetFactory factory;
    
    public ReviewCreationWizardController(IProject project, ChangeSetFactory factory)
    {
	this.project = project;
	this.factory = factory;
    }

    public CodeReview createCodeReview()
    {
	return null;
    }
}
