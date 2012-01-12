package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.keebraa.java.cleancode.core.extensionpoints.CommitRepository;
import com.keebraa.java.cleancode.core.model.CodeReview;

/**
 * Controller is responsible for creation of the {@link CodeReview}.
 * 
 * @author taqi
 * 
 */
public class ReviewCreationWizardController
{
    private static final String SELECT_COMMITS_PAGE = "select_commits_page";
    
    private IProject project;

    private CommitRepository factory;

    public ReviewCreationWizardController(IProject project, CommitRepository factory)
    {
	this.project = project;
	this.factory = factory;
    }

    public CodeReview createCodeReview()
    {
	Wizard wizard = createCodeReviewCreationWizard();
	wizard.addPage(createCommitsSelectionPage());
	return showWizard(wizard);
    }

    private Wizard createCodeReviewCreationWizard()
    {
	Wizard reviewCreationWizard = new ReviewCreationWizard();
	return reviewCreationWizard;
    }

    private CodeReview showWizard(Wizard wizard)
    {
	Shell currShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	WizardDialog dialog = new WizardDialog(currShell, wizard);
	dialog.open();
	return null;
    }
    
    private WizardPage createCommitsSelectionPage()
    {
	SelectCommitsWizardPage page = new SelectCommitsWizardPage(SELECT_COMMITS_PAGE, factory, project);
	return page;
    }
}
