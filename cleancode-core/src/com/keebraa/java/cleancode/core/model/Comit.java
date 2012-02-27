package com.keebraa.java.cleancode.core.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Class that represents one ChangeSet (commit) to the repository. It contains
 * Files (not real, but CommitFile. @see CommitFile. )
 * 
 * @author taqi
 * 
 */
public class Comit
{
    private List<ComitFile> files;
    
    private String foreignNumber;
    
    private String description;
    
    private UUID id;
    
    private Date comittedAt;
    
    private int revision;
    
    public Comit(List<ComitFile> files, String foreignNumber, String description, Date comittedAt, int revision)
    {
        this.files = files;
        this.foreignNumber = foreignNumber;
        this.description = description;
        id = UUID.randomUUID();
        this.comittedAt = comittedAt;
        this.revision = revision;
    }
    
    public List<ComitFile> getFiles()
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
    
    public Date getComittedAt()
    {
	 return comittedAt;
    }
    
    public int getRevision()
    {
	 return revision;
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
        Comit other = (Comit) obj;
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
