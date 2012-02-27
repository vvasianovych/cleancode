package com.keebraa.java.cleancode.core.model;

import java.util.ArrayList;
import java.util.List;

public class ComitFile
{    
    private Status status;
    
    private String name;
    
    private List<Change> changes;
    
    public ComitFile()
    {
        this(Status.NULL, new ArrayList<Change>());
    }
    
    public ComitFile(Status status)
    {
        this(status, new ArrayList<Change>());
    }
    
    public ComitFile(Status status, List<Change> changes)
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
