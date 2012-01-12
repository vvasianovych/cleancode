package com.keebraa.java.cleancode.repository;

import com.keebraa.java.cleancode.core.model.Commit;
import com.vectrace.MercurialEclipse.history.MercurialRevision;

public class CommitBuilder
{
   private String createForeignNumber(MercurialRevision revision)
   {
	return ""+revision.getRevision()+":"+revision.getContentIdentifier();
   }
   
   public Commit build(MercurialRevision revision)
   {
	return new Commit(null, createForeignNumber(revision), revision.getComment());
   }
}
