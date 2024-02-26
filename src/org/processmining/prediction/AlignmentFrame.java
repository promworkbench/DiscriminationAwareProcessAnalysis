package org.processmining.prediction;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;

import org.processmining.framework.util.Pair;
import org.processmining.plugins.DataConformance.ResultReplay;
import org.processmining.prediction.Augmentation.AlignmentMove;
import org.processmining.prediction.Augmentation.Augmentation;

import com.fluxicon.slickerbox.factory.SlickerFactory;

class PairButton extends JToggleButton
{
	public static final Dimension buttonPreferredSize=new Dimension(65,30);

	private Pair<String, String> pair;

	public PairButton(String activityName,String stepType,int number)
	{
		super(number+"");
		pair=new Pair<String, String>(activityName, stepType);		
		this.setPreferredSize(buttonPreferredSize);

	}
	
	public Pair<String,String> getPair()
	{
		return pair;
	}
	
	
}

class ColumnSelector implements ActionListener
{

	private int index;
	private PairButton[][] buttonList;

	public ColumnSelector(int i, PairButton[][] buttonList) {
		index=i;
		this.buttonList=buttonList;
	}

	public void actionPerformed(ActionEvent arg0) {
		buttonList[0][index].setSelected(!buttonList[0][index].isSelected());
		for(int j=1;j<buttonList.length;j++)
		{
			buttonList[j][index].setSelected(buttonList[0][index].isSelected());
		}
	}
	
}

class RowSelector implements ActionListener
{

	private int index;
	private PairButton[][] buttonList;

	public RowSelector(int i, PairButton[][] buttonList) {
		index=i;
		this.buttonList=buttonList;
	}

	public void actionPerformed(ActionEvent arg0) {
		buttonList[index][0].setSelected(!buttonList[index][0].isSelected());
		for(int j=1;j<buttonList[index].length;j++)
		{
			buttonList[index][j].setSelected(buttonList[index][0].isSelected());
		}
	}
	
}

public class AlignmentFrame extends JFrame {

	private PairButton[][] buttonList;
	private ResultReplay resReplay;
	private String[] activityList;

	public AlignmentFrame(Collection<String> activitySet, ResultReplay resReplay) {
		activityList=activitySet.toArray(new String[0]);
		Arrays.sort(activityList);
		buttonList=new PairButton[activityList.length][4];
		JPanel innerButtonPanel=new JPanel();
		GridBagConstraints c = new GridBagConstraints();
		innerButtonPanel.setLayout(new GridBagLayout());
		JButton aButton;
		aButton=SlickerFactory.instance().createButton("Activity");
		c.gridwidth=2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx=0;
		c.gridy=0;
		c.weightx=1;
		c.weighty=1;
		innerButtonPanel.add(aButton,c);
		aButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				buttonList[0][0].setSelected(!buttonList[0][0].isSelected());
				for(int i=0;i<buttonList.length;i++)
					for(int j=0;j<buttonList[i].length;j++)
				{
					buttonList[i][j].setSelected(buttonList[0][0].isSelected());
				}
				
			}
		});
		aButton=SlickerFactory.instance().createButton("Log");
		c.gridwidth=1;
		c.gridx=2;
		Dimension preferredSize=PairButton.buttonPreferredSize;
		aButton.setPreferredSize(preferredSize);
		innerButtonPanel.add(aButton,c); 
		aButton.addActionListener(new ColumnSelector(0,buttonList));
		aButton=SlickerFactory.instance().createButton("Sync");
		aButton.setPreferredSize(preferredSize);
		c.gridx=3;
		innerButtonPanel.add(aButton,c); 
		aButton.addActionListener(new ColumnSelector(1,buttonList));
		aButton=SlickerFactory.instance().createButton("Sync No Good");
		aButton.setPreferredSize(preferredSize);
		c.gridx=4;
		innerButtonPanel.add(aButton,c); 
		aButton.addActionListener(new ColumnSelector(2,buttonList));
		aButton=SlickerFactory.instance().createButton("Model");
		aButton.setPreferredSize(preferredSize);
		c.gridx=5;
		innerButtonPanel.add(aButton,c); 
		aButton.addActionListener(new ColumnSelector(3,buttonList));


				
		this.resReplay=resReplay;
		int j=0;
		for(int i=0;i<activityList.length;i++)
		{
			c.gridy++;
			String activity=activityList[i];
			if (activity.equals(Predictor.CASE_ACTIVITY))
				continue;
			float actArray[]=resReplay.actArray.get(activity);
			if (actArray==null)
				actArray=new float[]{0,0,0,0,0};
			buttonList[j][0]=new PairButton(activity,AlignmentMove.MOVE_LOG,(int) actArray[2]);
			buttonList[j][1]=new PairButton(activity,AlignmentMove.MOVE_BOTH_OK,(int) actArray[0]);
			buttonList[j][2]=new PairButton(activity,AlignmentMove.MOVE_BOTH_NOK,(int) actArray[1]);
			buttonList[j][3]=new PairButton(activity,AlignmentMove.MOVE_MODEL,(int) actArray[3]);

			aButton=SlickerFactory.instance().createButton(activity);
			c.gridwidth=2;
			c.gridx=0;
			innerButtonPanel.add(aButton,c);
			aButton.addActionListener(new RowSelector(j,buttonList));
			c.gridwidth=1;
			c.gridx=2;
			innerButtonPanel.add(buttonList[j][0],c);
			c.gridx=3;
			innerButtonPanel.add(buttonList[j][1],c);
			c.gridx=4;
			innerButtonPanel.add(buttonList[j][2],c);
			c.gridx=5;
			innerButtonPanel.add(buttonList[j][3],c);
			j++;
		}
		JScrollPane jsp=new JScrollPane(innerButtonPanel);
		setContentPane(jsp);
		pack();
		Dimension screenDim=Toolkit.getDefaultToolkit().getScreenSize();
		Dimension windowDim=getSize();
		int height=0;
		int width=0;
		if (screenDim.getHeight()<windowDim.getHeight());
			height=(int) screenDim.getHeight();
		if (screenDim.getWidth()<windowDim.getWidth());
			width=(int) screenDim.getWidth();
		if(height>0 || width > 0)
			setSize(width,height);
	}

	public Collection<Augmentation> createAugmentation() {
		ArrayList<Augmentation> considered=new ArrayList<Augmentation>();
		for(int i=0;i<buttonList.length;i++)
			for(int j=0;j<buttonList[i].length;j++)
			{
				if (buttonList[i][j].isSelected())
					considered.add(new AlignmentMove(resReplay,buttonList[i][j].getPair().getFirst(),buttonList[i][j].getPair().getSecond()));
			}
		return considered;
	}

}
