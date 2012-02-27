package com.keebraa.java.cleancode.core.model.builders;

import java.util.ArrayList;
import java.util.List;

import com.keebraa.java.cleancode.core.exceptions.CommitRepositoryNotFoundException;
import com.keebraa.java.cleancode.core.extensionpoints.ComitRepository;
import com.keebraa.java.cleancode.core.extensionpoints.ComitRepositoryProvider;
import com.keebraa.java.cleancode.core.model.CodeReview;
import com.keebraa.java.cleancode.core.model.Comit;
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
   private List<Comit> comits;
   private List<Reviewer> reviewers;
   
   public void setComits(List<Comit> comits)
   {
	this.comits = comits; 
   }
   
   public void setReviewes(List<Reviewer> reviewers)
   {
	this.reviewers = reviewers;
   }
   
   public void addReviewer(Reviewer reviewer)
   {
	getReviewers().add(reviewer);
   }
   
   public void addComit(Comit comit)
   {
	getComits().add(comit);
   }
   
   private List<Comit> getComits()
   {
	if(comits == null)
	{
	   comits = new ArrayList<Comit>();
	}
	return comits;
   }
   
   private List<Reviewer> getReviewers()
   {
	if(reviewers == null)
	{
	   reviewers = new ArrayList<Reviewer>();
	}
	return reviewers;
   }

   private Comit calculateBasicState() throws CommitRepositoryNotFoundException
   {
	Comit older = comits.get(0);
	for(Comit comit : comits)
	{
	   if(older.getComittedAt().after(comit.getComittedAt()))
	   {
		older = comit;
	   }
	}
	ComitRepository repository = ComitRepositoryProvider.getCommitRepository();
	Comit result = repository.getBefore(older);
	return result;
   }
   
   public CodeReview build() throws CommitRepositoryNotFoundException
   {
	Comit basicState = calculateBasicState();
	CodeReview codeReview = new CodeReview(getReviewers(), getComits(), basicState);
	return codeReview;
   }
}
