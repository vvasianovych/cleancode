package com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.reviewerspage;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.keebraa.java.cleancode.core.model.Reviewer;

public class ReviewersTableBuilder
{
    private Table reviewersTable;

    private WizardPage page;

    private SelectionListener listener;

    private Composite parent;

    public void setParent(Composite parent)
    {
         this.parent = parent;
    }

    public void createSelectionListener()
    {
         this.listener = new ReviewersSelectionListener();
    }

    public void setPage(WizardPage page)
    {
         this.page = page;
    }

    public Table build()
    {
         reviewersTable = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI);
         reviewersTable.setHeaderVisible(true);
         createCheckBoxColumn();
         fillTable();
         reviewersTable.addSelectionListener(listener);
         reviewersTable.setData(page);
         return reviewersTable;
    }

    private void createCheckBoxColumn()
    {
         TableColumn column = new TableColumn(reviewersTable, SWT.NONE);
         column.setText("Reviewer");
         column.setWidth(100);
    }

    private void fillTable()
    {
         for (int i = 0; i < 5; i++)
         {
            TableItem item = new TableItem(reviewersTable, SWT.NONE);
            Reviewer reviewer = new Reviewer();
            reviewer.setSign("aaa");
            item.setText("aaaa");
            item.setData(reviewer);
         }
    }
}