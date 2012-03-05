package com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.commitpage;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.keebraa.java.cleancode.core.exceptions.CommitTableBuilderException;
import com.keebraa.java.cleancode.core.extensionpoints.ComitRepository;
import com.keebraa.java.cleancode.core.model.Comit;

public class CommitTableBuilder
{
    private final String FOREIGNCOLUMN_TITLE = "foreign number (ID)";

    private final String COMMENT_TITLE = "comment";

    private Table commitTable;

    private ComitRepository repository;

    private WizardPage page;

    private SelectionListener listener;

    private IProject project;

    private Composite parent;

    public CommitTableBuilder(ComitRepository repository, IProject project)
    {
        if (repository == null)
        {
            throw new CommitTableBuilderException(
                    "CommitRepository can't be null");
        }
        this.project = project;
        this.repository = repository;
    }

    public void setParent(Composite parent)
    {
        this.parent = parent;
    }

    public void setCommitRepository(ComitRepository repository)
    {
        this.repository = repository;
    }

    public void createSelectionListener()
    {
        this.listener = new CommitSelectionListener();
    }

    public void setPage(WizardPage page)
    {
        this.page = page;
    }

    public Table build()
    {
        commitTable = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI);
        commitTable.setHeaderVisible(true);
        createCheckBoxColumn();
        createForeignNumberColumn();
        createCommentColumn();
        fillTable();
        commitTable.addSelectionListener(listener);
        commitTable.setData(page);
        return commitTable;
    }

    private void createCheckBoxColumn()
    {
        TableColumn column = new TableColumn(commitTable, SWT.NONE);
        column.setText("");
        column.setWidth(20);
    }

    private void createForeignNumberColumn()
    {
        TableColumn column = new TableColumn(commitTable, SWT.NONE);
        column.setText(FOREIGNCOLUMN_TITLE);
        column.setWidth(200);
    }

    private void createCommentColumn()
    {
        TableColumn column = new TableColumn(commitTable, SWT.NONE);
        column.setText(COMMENT_TITLE);
        column.setWidth(300);
    }

    private void fillTable()
    {
        for (Comit commit : repository.getAllCommits())
        {
            TableItem item = new TableItem(commitTable, SWT.NONE);
            item.setText(new String[] { "", commit.getForeignNumber(),
                    commit.getDescription() });
            item.setData(commit);
        }
    }
}
