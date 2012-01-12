package com.keebraa.java.tipstricks.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class KTable extends Composite
{
   public KTable(Composite parent, int style)
   {
	super(parent, checkStyle(style));
   }
   
   private static int checkStyle(int style)
   {
   /* GTK is always FULL_SELECTION */
	style |= SWT.FULL_SELECTION;
	return style;
   }
}
