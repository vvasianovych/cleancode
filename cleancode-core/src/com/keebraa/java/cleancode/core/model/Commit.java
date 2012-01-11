package com.keebraa.java.cleancode.core.model;

import java.util.List;
import java.util.UUID;

/**
 * Class that represents one ChangeSet (commit) to the repository. It contains
 * Files (not real, but CommitFile. @see CommitFile. )
 * 
 * @author taqi
 * 
 */
public class Commit
{
    private List<CommitFile> files;
    
    private String foreignNumber;
    
    private String description;
    
    private UUID id;
    
    public Commit(List<CommitFile> files, String foreignNumber, String description)
    {
        this.files = files;
        this.foreignNumber = foreignNumber;
        this.description = description;
        id = UUID.randomUUID();
    }
    
    public List<CommitFile> getFiles()
    {
        return files;
    }
    
    public String getForeignNumber()
    {
        return foreignNumber;
    }
    
    public UUID getId()
    {
        return id;
    }
    
    public String getDescription()
    {
	return description;
    }
    
    @Override
    public int hashCode()
    {
        return id.hashCode();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        Commit other = (Commit) obj;
        if (id == null)
        {
            if (other.id != null)
            {
                return false;
            }
        }
        else if (!id.equals(other.id))
        {
            return false;
        }
        return true;
    }
}
