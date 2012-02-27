package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.keebraa.java.cleancode.core.exceptions.CommitRepositoryNotFoundException;
import com.keebraa.java.cleancode.core.extensionpoints.ComitRepository;
import com.keebraa.java.cleancode.core.model.CodeReview;
import com.keebraa.java.cleancode.core.model.builders.CodeReviewBuilder;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.CodeReviewCreationWizardPage;

/**
 * Controller is responsible for creation of the {@link CodeReview}.
 * 
 * @author taqi
 * 
 */

public class ReviewCreationController
{
   private IProject project;

   private ComitRepository factory;

   public ReviewCreationController(IProject project, ComitRepository factory)
   {
	this.project = project;
	this.factory = factory;
   }

   /**
    * The nice thing in the Wizard dialog is that method open() locks thread
    * until you finish your wizard.
    * 
    * @return
    * @throws CommitRepositoryNotFoundException 
    */
   public CodeReview performReviewCreationWizard() throws CommitRepositoryNotFoundException
   {
	CodeReviewCreationWizardBuilder wizardBuilder = new CodeReviewCreationWizardBuilder(project, factory);
	CodeReviewCreationWizard wizard = wizardBuilder.build();
	Shell currShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	WizardDialog dialog = new WizardDialog(currShell, wizard);
	dialog.open();
	CodeReviewBuilder codeReviewBuilder = new CodeReviewBuilder();
	for(IWizardPage page : wizard.getPages())
	{
	   CodeReviewCreationWizardPage wizardPage = (CodeReviewCreationWizardPage) page;
	   wizardPage.fillCodeReviewBuilder(codeReviewBuilder);
	}
	return codeReviewBuilder.build();
   }
}