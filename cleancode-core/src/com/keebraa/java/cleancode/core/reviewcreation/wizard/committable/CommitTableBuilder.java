package com.keebraa.java.cleancode.core.reviewcreation.wizard.committable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.keebraa.java.cleancode.core.exceptions.CommitTableBuilderException;
import com.keebraa.java.cleancode.core.extensionpoints.CommitRepository;
import com.keebraa.java.cleancode.core.model.Commit;

public class CommitTableBuilder
{
    private final String FOREIGNCOLUMN_TITLE = "foreign number (ID)";
    private final String COMMENT_TITLE = "comment";
    private Table commitTable;
    private CommitRepository repository;

    public CommitTableBuilder(CommitRepository repository)
    {
	if(repository == null)
	{
	    throw new CommitTableBuilderException("CommitRepository can't be null");
	}
	this.repository = repository;
    }
    
    public void createTable(Composite parent)
    {
	commitTable = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI);
	commitTable.setHeaderVisible(true);
    }

    private void createCheckBoxColumn()
    {
	TableColumn column = new TableColumn(getTable(), SWT.NONE);
	column.setText("");
	column.setWidth(20);
    }
    private void createForeignNumberColumn()
    {
	TableColumn column = new TableColumn(getTable(), SWT.NONE);
	column.setText(FOREIGNCOLUMN_TITLE);
	column.setWidth(200);
    }
    
    private void createCommentColumn()
    {
	TableColumn column = new TableColumn(getTable(), SWT.NONE);
	column.setText(COMMENT_TITLE);
	column.setWidth(300);
    }
    
    private Table getTable()
    {
	if (commitTable == null)
	{
	    throw new RuntimeException("CommitTableBuilder: table is null. Call createTable first.");
	}
	return commitTable;
    }

    private void fillTable()
    {
	for(Commit commit : repository.getAllCommits())
	{
	    TableItem item = new TableItem(getTable(), SWT.NONE);
	    item.setText(new String[]{"", commit.getForeignNumber(), commit.getDescription()});
	}
    }
    public void setCommitRepository(CommitRepository repository)
    {
	this.repository = repository;
    }
    
    public Table build()
    {
	createCheckBoxColumn();	
	createForeignNumberColumn();
	createCommentColumn();
	fillTable();
	return getTable();
    }
}
