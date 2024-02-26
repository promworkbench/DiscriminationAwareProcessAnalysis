package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.processmining.framework.util.ui.scalableview.ScalableComponent;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ViewInteractionPanel;

import com.fluxicon.slickerbox.factory.SlickerFactory;

public class Summary extends JPanel implements ViewInteractionPanel{


	public Summary(EvaluationNDC evaluation) {
		final StringBuffer sb = new StringBuffer();
		try {
			
			sb.append(evaluation.toStringDiscrimination());
			sb.append(evaluation.toStringAccuracy());	
	
			} catch (Exception e) {
				
				if (!e.getMessage().contains("per class statistics possible"))
					e.printStackTrace(); 
			}
			JTextArea textArea = new JTextArea(sb.toString());
			textArea.setEditable(false);
			textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
			JScrollPane scrollPane = new JScrollPane(textArea);
			this.setLayout(new BorderLayout());
			add(scrollPane, BorderLayout.CENTER);
			JPanel panel=new JPanel();
			JButton copyClipboard=SlickerFactory.instance().createButton("Copy to the clipboard");
			panel.add(copyClipboard);
			copyClipboard.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e) {
					java.awt.datatransfer.Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
					clpbrd.setContents(new StringSelection(sb.toString()), null);
				}
			});
			add(panel,BorderLayout.SOUTH);
	}

	public JComponent getComponent() {
		return this;
	}

	public double getHeightInView() {
		return this.getPreferredSize().getHeight();
	}

	public String getPanelName() {
		return "Summary";
	}

	public double getWidthInView() {
		return this.getPreferredSize().getWidth();
	}

	public void setParent(ScalableViewPanel viewPanel) {
	
	}

	public void setScalableComponent(ScalableComponent scalable) {
		
	}

	public void willChangeVisibility(boolean to) {
		// TODO Auto-generated method stub
		
	}

	public void updated() {
		// TODO Auto-generated method stub
		
	}
}
