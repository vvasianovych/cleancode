package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.IWizardPage;

import com.keebraa.java.cleancode.core.extensionpoints.CommitRepository;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.commitpage.CommitSelectionWizardPageBuilder;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.commitpage.CommitTableBuilder;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.reviewerspage.ReviewersSelectionWizardPage;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.reviewerspage.ReviewersTableBuilder;

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

    public CodeReviewCreationWizardBuilder(IProject project,
            CommitRepository repository)
    {
        this.project = project;
        this.repository = repository;
    }

    private IWizardPage createCommitSelectionPage()
    {
        CommitTableBuilder commitTableBuilder = new CommitTableBuilder(
                repository, project);
        commitTableBuilder.createSelectionListener();
        CommitSelectionWizardPageBuilder pageBuilder = new CommitSelectionWizardPageBuilder();
        pageBuilder.setCommitTableBuilder(commitTableBuilder);
        return pageBuilder.build();
    }

    private CodeReviewCreationWizard createWizard()
    {
        return new CodeReviewCreationWizard();
    }

    private IWizardPage createReviewerSelectionPage()
    {
        ReviewersTableBuilder builder = new ReviewersTableBuilder();
        builder.createSelectionListener();
        ReviewersSelectionWizardPage page = new ReviewersSelectionWizardPage(builder);
        return page;
    }

    public CodeReviewCreationWizard build()
    {
        CodeReviewCreationWizard wizard = createWizard();
        wizard.addPage(createCommitSelectionPage());
        wizard.addPage(createReviewerSelectionPage());
        return wizard;
    }
}
