package com.keebraa.java.cleancode.core.reviewcreation.wizard;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.keebraa.java.cleancode.core.extensionpoints.ChangeSetFactory;
import com.keebraa.java.cleancode.core.model.Commit;

/**
 * This class is Builder (GoF pattern), but without abstract superClass. Just
 * build table for wizard.
 * 
 * @author taqi
 * 
 */
public class CommitTableBuilder
{
    private final String FOREIGN_NUMBER_COLUMN_TITLE = "Foreign number";
    private final String DESCRIPTION_COLUMN_TITLE = "Description";
    private TableViewer tableViewer;
    private List<CellEditor> editors;
    private ChangeSetFactory factory;

    /**
     * This method is necessary only for checking in methods if our table is not
     * null
     * 
     * @return current tableViewer
     * @throws RuntimeException
     *             if tableViewer now is null
     */
    private TableViewer getTableViewer()
    {
	if (tableViewer == null)
	{
	    throw new RuntimeException("CommitTableBuilder: tableViewer is null. Sorry ( ");
	}
	return tableViewer;
    }

    private List<CellEditor> getCellEditors()
    {
	if (editors == null)
	{
	    editors = new ArrayList<CellEditor>(5);
	}
	return editors;
    }

    public void createTable(Composite parent)
    {
	if (tableViewer != null)
	    return;
	tableViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
	tableViewer.setUseHashlookup(true);
	tableViewer.getTable().setHeaderVisible(true);
    }
    
    public void setChangeSetFacoty(ChangeSetFactory factory)
    {
	this.factory = factory;
    }

    public void addCheckBoxColumn()
    {
	TableViewerColumn column = new TableViewerColumn(getTableViewer(), SWT.NONE);
	column.getColumn().setWidth(20);
	CellEditor editor = new CheckboxCellEditor(getTableViewer().getTable());
	getCellEditors().add(editor);
    }

    /**
     * this column is for inner number of your Version Control system. Some ID
     * text. This column will be without editor;
     */
    public void addForeignNumberColumn()
    {
	TableViewerColumn column = new TableViewerColumn(getTableViewer(), SWT.NONE);
	column.getColumn().setText(FOREIGN_NUMBER_COLUMN_TITLE);
	column.getColumn().setWidth(200);
	getCellEditors().add(null);
    }

    public void addCommentColumn()
    {
	TableViewerColumn column = new TableViewerColumn(getTableViewer(), SWT.NONE);
	column.getColumn().setText(DESCRIPTION_COLUMN_TITLE);
	column.getColumn().setWidth(200);
	getCellEditors().add(null);
    }

    private void addEditorsToTable()
    {
	CellEditor[] editors = new CellEditor[getCellEditors().size()];
	for (int i = 0; i < editors.length; i++)
	{
	    editors[i] = getCellEditors().get(i);
	}
	getTableViewer().setCellEditors(editors);
    }

    private IContentProvider createContentProvider()
    {
	CommitsTableProvider provider = new CommitsTableProvider();
	return provider;
    }
    
    private void fillTable(TableViewer tableViewer)
    {
	for(Commit commit : factory.getAllCommits())
	{
	    tableViewer.add(commit);
	}
    }
    /**
     * The main method of the Builder mechanism. Create table related to the
     * previous called methods (addColumn...)
     * 
     * @return created TableViewer
     */
    public TableViewer build()
    {
	addEditorsToTable();
	getTableViewer().setContentProvider(createContentProvider());
	fillTable(getTableViewer());
	return getTableViewer();
    }
}
