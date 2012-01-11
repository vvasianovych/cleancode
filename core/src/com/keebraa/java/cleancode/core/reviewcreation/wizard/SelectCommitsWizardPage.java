package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import com.keebraa.java.cleancode.core.extensionpoints.ChangeSetFactory;

public class SelectCommitsWizardPage extends WizardPage
{
    private final String PAGE_TITLE = "Select your commits";

    private final String PAGE_DESCRIPTION = "Please, select changes for code review";

    private ChangeSetFactory factory;

    public SelectCommitsWizardPage(String pageName, ChangeSetFactory factory)
    {
	super(pageName);
	this.factory = factory;
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
    
    private TableViewer createCommitTable(Composite parent)
    {
	CommitTableBuilder builder = new CommitTableBuilder();
	builder.setChangeSetFacoty(factory);
	builder.createTable(parent);
	builder.addCheckBoxColumn();
	builder.addForeignNumberColumn();
	builder.addCommentColumn();
	return builder.build();
    }
}
