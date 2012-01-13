package com.keebraa.java.cleancode.core.reviewcreation.wizard.commitpage;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.keebraa.java.cleancode.core.extensionpoints.CommitRepository;
import com.keebraa.java.cleancode.core.model.Commit;
import com.keebraa.java.cleancode.core.model.builders.CodeReviewBuilder;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.CodeReviewCreationWizardPage;

public class CommitSelectionWizardPage extends WizardPage implements CodeReviewCreationWizardPage
{
   private final String PAGE_TITLE = "Select your commits";

   private final String PAGE_DESCRIPTION = "Please, select changes for code review";

   private CommitRepository repository;

   private IProject project;

   private Table commitTable;

   public CommitSelectionWizardPage(String pageName, CommitRepository repository, IProject project)
   {
	super(pageName);
	this.repository = repository;
	this.project = project;
   }

   @Override
   public void createControl(Composite parent)
   {
	setTitle(PAGE_TITLE);
	setDescription(PAGE_DESCRIPTION);
	commitTable = createCommitTable(parent);
	setControl(parent);
	setPageComplete(false);
   }

   public List<Commit> getSelectedCommits()
   {
	List<Commit> selectedCommits = new ArrayList<Commit>();
	for (TableItem item : commitTable.getItems())
	{
	   if (!item.getChecked())
	   {
		continue;
	   }
	   item.getData();
	}
	return selectedCommits;
   }

   private Table createCommitTable(Composite parent)
   {
	CommitSelectListener listener = new CommitSelectListener(this);
	CommitTableBuilder builder = new CommitTableBuilder(repository, project);
	builder.createTable(parent);
	builder.setTableSelectionListener(listener);
	return builder.build();
   }

   @Override
   public void fillCodeReviewBuilder(CodeReviewBuilder builder)
   {
   }
}
