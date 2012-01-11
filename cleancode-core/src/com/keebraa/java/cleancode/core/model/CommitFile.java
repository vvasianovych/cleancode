package com.keebraa.java.cleancode.core.model;

import java.util.ArrayList;
import java.util.List;

public class CommitFile
{    
    private Status status;
    
    private String name;
    
    private List<Change> changes;
    
    public CommitFile()
    {
        this(Status.NULL, new ArrayList<Change>());
    }
    
    public CommitFile(Status status)
    {
        this(status, new ArrayList<Change>());
    }
    
    public CommitFile(Status status, List<Change> changes)
    {
        this.status = status;
        this.changes = changes;
    }
    
    public void addChange(Change change)
    {
        changes.add(change);
    }
    
    public List<Change> getChanges()
    {
        return changes;
    }
    
    public Status getStatus()
    {
        return status;
    }
    
    public String getName()
    {
        return name;
    }
}
