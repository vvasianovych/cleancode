package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.events.SelectionListener;

import com.keebraa.java.cleancode.core.extensionpoints.CommitRepository;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.commitpage.CommitSelectionListener;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.commitpage.CommitSelectionWizardPageBuilder;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.commitpage.CommitTableBuilder;

/**
 * This builder encapsulates the whole logic for Wizard creation.
 * 
 * @author taqi
 * 
 */
public class CodeReviewCreationWizardBuilder
{
   private IProject project;

   private CommitRepository repository;
   
   public CodeReviewCreationWizardBuilder(IProject project, CommitRepository repository)
   {
	this.project = project;
	this.repository = repository;
   }
   
   private IWizardPage createCommitSelectionPage()
   {
	CommitTableBuilder commitTableBuilder = new CommitTableBuilder(repository, project);
	SelectionListener listener = new CommitSelectionListener();
	commitTableBuilder.setSelectionListener(listener);
	CommitSelectionWizardPageBuilder pageBuilder = new CommitSelectionWizardPageBuilder();
	pageBuilder.setCommitTableBuilder(commitTableBuilder);
	return pageBuilder.build();
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
