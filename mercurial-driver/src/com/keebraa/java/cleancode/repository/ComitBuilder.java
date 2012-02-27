package com.keebraa.java.cleancode.repository;

import java.util.Date;

import com.keebraa.java.cleancode.core.model.Comit;
import com.vectrace.MercurialEclipse.history.MercurialRevision;

public class ComitBuilder
{
   private String createForeignNumber(MercurialRevision revision)
   {
	return ""+revision.getRevision()+":"+revision.getContentIdentifier();
   }
   
   public Comit build(MercurialRevision revision)
   {
	Date date = revision.getChangeSet().getRealDate();
	return new Comit(null, createForeignNumber(revision), revision.getComment(), date, revision.getRevision());
   }
}
