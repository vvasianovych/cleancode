package com.keebraa.java.cleancode.core.model;

import java.util.List;

public class CodeReview
{
   private List<Reviewer> reviewers;
   
   private List<Comit> commits;
   
   private Comit basicState;
   
   public CodeReview(List<Reviewer> reviewers, List<Comit> commits, Comit basicState)
   {
	this.reviewers = reviewers;
	this.commits = commits;
	this.basicState = basicState;
   }
}
