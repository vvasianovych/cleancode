package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class SelectCommitsWizardPage extends WizardPage
{
    private final String PAGE_TITLE = "Select your commits";

    private final String PAGE_DESCRIPTION = "Please, select changes for code review";

    private final String CHECKBOX_COLUMN_TITLE = "";

    private final int CHECKBOX_COLUMN_WIDTH = 22;

    private final String NUMBER_COLUMN_TITLE = "symbolic number of changesets";

    private final int NUMBER_COLUMN_WIDTH = 250;

    private final String COMMENT_COLUMN_TITLE = "Comment";

    private final int COMMENT_COLUMN_WIDTH = 370;

    private Table commitTable;

    protected SelectCommitsWizardPage(String pageName)
    {
	super(pageName);
    }

    @Override
    public void createControl(Composite parent)
    {
	setTitle(PAGE_TITLE);
	setDescription(PAGE_DESCRIPTION);
	FormData formData = new FormData();
	formData.left = new FormAttachment(0, 0);
	formData.top = new FormAttachment(0, 0);
	formData.right = new FormAttachment(100, 0);
	formData.bottom = new FormAttachment(100, 0);
	getCommitTable(parent, formData);
	setControl(parent);
	setPageComplete(false);
    }

    private Composite getCommitTable(Composite parent, Object layoutData)
    {
	if (commitTable == null)
	{
	    commitTable = new Table(parent, SWT.CHECK | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);

	    TableColumn checkBoxColumn = new TableColumn(commitTable, SWT.LEFT);
	    checkBoxColumn.setText(CHECKBOX_COLUMN_TITLE);
	    checkBoxColumn.setWidth(CHECKBOX_COLUMN_WIDTH);
	    checkBoxColumn.setResizable(false);
	    
	    TableColumn numberCommitColumn = new TableColumn(commitTable, SWT.CENTER);
	    numberCommitColumn.setText(NUMBER_COLUMN_TITLE);
	    numberCommitColumn.setWidth(NUMBER_COLUMN_WIDTH);
	    numberCommitColumn.setResizable(false);
	    
	    TableColumn commentColumn = new TableColumn(commitTable, SWT.CENTER);
	    commentColumn.setText(COMMENT_COLUMN_TITLE);
	    commentColumn.setWidth(COMMENT_COLUMN_WIDTH);
	    commentColumn.setResizable(false);
	    commitTable.setHeaderVisible(true);
	}
	return commitTable;
    }
}
