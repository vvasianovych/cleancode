package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.keebraa.java.cleancode.core.extensionpoints.ChangeSetFactory;
import com.keebraa.java.cleancode.core.model.Commit;

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
	addCommitTable(parent);
	setControl(parent);
	setPageComplete(false);
    }

    private void addCommitTable(Composite parent)
    {
	TableViewer tableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
	CellEditor[] editors = new CellEditor[1];
	
	TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.NONE);
	column.getColumn().setText("aaa");
	column.getColumn().setWidth(100);
	editors[0] = new CheckboxCellEditor(tableViewer.getTable());
	tableViewer.setCellEditors(editors);
	tableViewer.setUseHashlookup(true);
	tableViewer.getTable().setHeaderVisible(true);
	tableViewer.setColumnProperties(new String[]{"aaaa"});
	
//	Table commitTable = new Table(parent, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
//	tableViewer.setCellEditors(editors)
//	TableColumn checkBoxColumn = new TableColumn(commitTable, SWT.LEFT);
//	checkBoxColumn.setText(CHECKBOX_COLUMN_TITLE);
//	checkBoxColumn.setWidth(CHECKBOX_COLUMN_WIDTH);
//	checkBoxColumn.setResizable(false);
//
//	TableColumn numberCommitColumn = new TableColumn(commitTable, SWT.CENTER | SWT.CHECK);
//	numberCommitColumn.setText(NUMBER_COLUMN_TITLE);
//	numberCommitColumn.setWidth(NUMBER_COLUMN_WIDTH);
//	numberCommitColumn.setResizable(false);
//
//	TableColumn commentColumn = new TableColumn(commitTable, SWT.CENTER);
//	commentColumn.setText(COMMENT_COLUMN_TITLE);
//	commentColumn.setWidth(COMMENT_COLUMN_WIDTH);
//	commentColumn.setResizable(false);
//	
//	commitTable.setHeaderVisible(true);
//	return commitTable;
    }

    private void fillCommitsTable(Table table)
    {
	for (Commit commit : factory.getAllCommits())
	{
	    TableItem item = new TableItem(table, SWT.NONE);
	    item.setText(1, commit.getForeignNumber());
	    item.setText(2, commit.getDescription());
	}
    }
}
