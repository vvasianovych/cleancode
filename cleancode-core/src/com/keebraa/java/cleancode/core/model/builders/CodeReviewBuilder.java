package com.keebraa.java.cleancode.core.model.builders;

import java.util.ArrayList;
import java.util.List;

import com.keebraa.java.cleancode.core.model.CodeReview;
import com.keebraa.java.cleancode.core.model.Commit;
import com.keebraa.java.cleancode.core.model.Reviewer;

/**
 * This builder creates correct Code Review document. it is necessary for the
 * Code Review creation Wizard.
 * 
 * @author taqi
 * 
 */
public class CodeReviewBuilder
{
   private List<Commit> commits;
   private List<Reviewer> reviewers;
   
   public void addCommits(List<Commit> commits)
   {
	getCommits().addAll(commits);
   }
   
   public void addReviewes(List<Reviewer> reviewers)
   {
	getReviewers().addAll(reviewers);
   }
   
   public void addReviewer(Reviewer reviewer)
   {
	getReviewers().add(reviewer);
   }
   
   public void addCommit(Commit commit)
   {
	getCommits().add(commit);
   }
   
   private List<Commit> getCommits()
   {
	if(commits == null)
	{
	   commits = new ArrayList<Commit>();
	}
	return commits;
   }
   
   private List<Reviewer> getReviewers()
   {
	if(reviewers == null)
	{
	   reviewers = new ArrayList<Reviewer>();
	}
	return reviewers;
   }
   
   public CodeReview build()
   {
	CodeReview codeReview = new CodeReview();
	return codeReview;
   }
}
