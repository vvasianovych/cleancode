package com.keebraa.java.cleancode.core.storage;

import com.keebraa.java.cleancode.core.exceptions.CodeReviewSavingException;
import com.keebraa.java.cleancode.core.model.CodeReview;

/**
 * This interface should be used as saving mechanism for the new
 * {@link CodeReview}.
 * 
 * @see ReviewCreationWizardController, CleanCodeEngine
 * @author taqi
 * 
 */
public interface CodeReviewStorage
{
    public void storeCodeReview(CodeReview codeReview) throws CodeReviewSavingException;
}
