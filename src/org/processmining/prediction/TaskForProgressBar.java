package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;

class ProgressBarDialog extends JFrame
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Container content;
	private JProgressBar progressBar;
	private int max;
	private boolean progressBarVisible;

	public ProgressBarDialog(String message,String note,int min, int max)
	{
		//setModal(true);
		super("Please wait...");
	//	setAlwaysOnTop(true);
		//setModalityType(ModalityType.APPLICATION_MODAL);
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		content=this.getContentPane();
		content.setLayout(new BorderLayout());
		 progressBar = new JProgressBar();
		 this.max=max;
		progressBar.setMinimum(min);
		progressBar.setMaximum(max);
		progressBar.setStringPainted(true);
		this.setResizable(false);
		Border border = BorderFactory.createTitledBorder(message);
		progressBar.setBorder(border);
		if("".equals(note))
			content.add(new JLabel(note),BorderLayout.NORTH);
		content.add(progressBar, BorderLayout.CENTER);
		setSize(300, 100);
		progressBarVisible=true;
		setUndecorated(true);
		setBackground(Color.YELLOW);
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		final int x = (screenSize.width - getWidth()) / 2;
		final int y = (screenSize.height - getHeight()) / 2;
		setLocation(x, y);
		
	}
	
	public void setValue(final int n){
		SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				if (n>=0)
				{
					if (progressBarVisible)
						progressBar.setValue(n);
					else
					{
						content.add(progressBar, BorderLayout.CENTER);
						progressBar.setValue(n);
						progressBarVisible=true;
					}
				}
				else
				{
					content.add(new JLabel("Progress Bar not available..."),BorderLayout.CENTER);
				}
			}
		});
		
		if (n==max)
		{
			this.dispose();
		}
		
	}
}

public abstract class TaskForProgressBar extends SwingWorker<Void,Void> implements PropertyChangeListener
{

	private int progress;
	private ProgressBarDialog pm;
	protected Component component;

	public TaskForProgressBar(String message,String note,int min, int max)
	{
		this(null,message,note,min,max);
	}
	
	public TaskForProgressBar(Component component,String message,String note,int min, int max)
	{
		super();
		this.component=component;
		pm=new ProgressBarDialog(message,note,min,max);
		this.addPropertyChangeListener(this);
	}

	public void myProgress(int progress)
	{
		
		if (this.progress!=progress)
		{
			setProgress(progress);
			this.progress=progress;			
		}
	}
	
	
	public void propertyChange(PropertyChangeEvent arg0) {
		pm.setVisible(true);
		pm.setValue(this.getProgress());
	}
	
}
