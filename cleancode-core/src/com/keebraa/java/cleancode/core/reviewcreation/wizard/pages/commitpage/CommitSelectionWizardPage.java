package com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.commitpage;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.keebraa.java.cleancode.core.model.Commit;
import com.keebraa.java.cleancode.core.model.builders.CodeReviewBuilder;
import com.keebraa.java.cleancode.core.reviewcreation.wizard.pages.CodeReviewCreationWizardPage;

public class CommitSelectionWizardPage extends WizardPage implements CodeReviewCreationWizardPage
{
   private final static String PAGENAME = "commit_selection_wizard_page";
   
   private final String PAGE_TITLE = "Select your commits";

   private final String PAGE_DESCRIPTION = "Please, select changes for code review";

   private Table commitTable;
   
   private CommitTableBuilder commitTableBuilder;
   
   public CommitSelectionWizardPage(CommitTableBuilder commitTableBuilder)
   {
	super(PAGENAME);
	this.commitTableBuilder = commitTableBuilder;
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
	   Commit commit = (Commit)item.getData();
	   selectedCommits.add(commit);
	}
	return selectedCommits;
   }

   private Table createCommitTable(Composite parent)
   {
	commitTableBuilder.setParent(parent);
	commitTableBuilder.setPage(this);
	return commitTableBuilder.build();
   }

   @Override
   public void fillCodeReviewBuilder(CodeReviewBuilder builder)
   {
   }
}
