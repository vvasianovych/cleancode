package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.keebraa.java.cleancode.core.extensionpoints.CommitRepository;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.committable.CommitTableBuilder;

public class SelectCommitsWizardPage extends WizardPage
{
    private final String PAGE_TITLE = "Select your commits";

    private final String PAGE_DESCRIPTION = "Please, select changes for code review";

    private CommitRepository repository;

    public SelectCommitsWizardPage(String pageName, CommitRepository repository)
    {
	super(pageName);
	this.repository = repository;
    }

    @Override
    public void createControl(Composite parent)
    {
	setTitle(PAGE_TITLE);
	setDescription(PAGE_DESCRIPTION);
	createCommitTable(parent);
	setControl(parent);
	setPageComplete(false);
    }
    
    private Table createCommitTable(Composite parent)
    {
	CommitTableBuilder builder = new CommitTableBuilder(repository);
	builder.createTable(parent);
	return builder.build();
    }
}
