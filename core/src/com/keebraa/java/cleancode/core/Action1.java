package com.keebraa.java.cleancode.core;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IActionDelegate;

public class Action1 implements IActionDelegate{

	public Action1() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(IAction action) {
		System.out.println("FUCK UP!");
		
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
		
	}
}
