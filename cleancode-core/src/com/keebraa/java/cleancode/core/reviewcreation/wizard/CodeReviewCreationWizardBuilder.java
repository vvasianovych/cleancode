package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.keebraa.java.cleancode.core.extensionpoints.CommitRepository;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.commitpage.CommitSelectionWizardPage;

/**
 * This builder encapsulates the whole logic for Wizard creation.
 * 
 * @author taqi
 * 
 */
public class CodeReviewCreationWizardBuilder
{
   private static final String SELECT_COMMITS_PAGE = "select_commits_page";
   
   private IProject project;

   private CommitRepository repository;
   
   public CodeReviewCreationWizardBuilder(IProject project, CommitRepository repository)
   {
	this.project = project;
	this.repository = repository;
   }
   
   private IWizardPage createCommitSelectionPage()
   {
	CommitSelectionWizardPage page = 
		new CommitSelectionWizardPage(SELECT_COMMITS_PAGE, repository, project);
	return page;
   }
   
   private CodeReviewCreationWizard createWizard()
   {
	return new CodeReviewCreationWizard();
   }
   
   public CodeReviewCreationWizard build()
   {
	CodeReviewCreationWizard wizard = createWizard();
	wizard.addPage(createCommitSelectionPage());
	return wizard;
   }
}
