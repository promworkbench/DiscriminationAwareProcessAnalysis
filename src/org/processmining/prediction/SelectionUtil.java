package org.processmining.prediction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.scalableview.ScalableViewPanel;
import org.processmining.framework.util.ui.scalableview.interaction.PIPInteractionPanel;
import org.processmining.framework.util.ui.scalableview.interaction.ZoomInteractionPanel;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.AttributeMap;
import org.processmining.models.graphbased.ViewSpecificAttributeMap;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Arc;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.jgraph.ProMGraphModel;
import org.processmining.models.jgraph.ProMJGraph;
import org.processmining.models.jgraph.elements.ProMGraphCell;
import org.processmining.models.jgraph.visualization.ProMJGraphPanel;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.hierarchical.JGraphHierarchicalLayout;

public class SelectionUtil {
	private int serial = 0;
	private final Color involvedMoveOnLogColor = new Color(255, 0, 0, 200);
	private final Color transparentColor = new Color(255, 255, 255, 0);
	private ProMJGraph graph = null;
	private PetrinetGraph net;
	private GraphLayoutConnection layoutConnection = null;
	private Set<DirectedGraphNode> selection, selected;
	private JButton ok;
	private PluginContext context;

	public SelectionUtil(PluginContext context, PetrinetGraph net) throws Exception {
		this.net = net;
		this.context = context;
		Collection<GraphLayoutConnection> layouts = context.getConnectionManager().getConnections(
				GraphLayoutConnection.class, context, net);
		if (layouts != null) layoutConnection = layouts.iterator().next();
		
	}
	
	Set<DirectedGraphNode> getChoice(String title, final boolean isChoice) {//, Set<DirectedGraphNode> previousSelection) {

		boolean newConnection = false;
//		if (previousSelection == null) {
			selection = new HashSet<DirectedGraphNode>();
			selected = new HashSet<DirectedGraphNode>();
//		} else {
//			selection = new HashSet<DirectedGraphNode>(previousSelection);
//			selected = previousSelection;
//		}
		final JDialog dialog = new JDialog();
		dialog.setTitle(title);
		dialog.setModalityType(ModalityType.APPLICATION_MODAL);

		// Make new content panes
		final Container contentPane = new JPanel(new BorderLayout(0,0));
		contentPane.setBackground(Color.white);

//		ScalableComponent scalable = GraphBuilder.buildJGraph(net, layoutConnection);
//		ProMJGraph graph = (ProMJGraph) scalable;

		ViewSpecificAttributeMap map = new ViewSpecificAttributeMap();

		if (layoutConnection == null) {
			layoutConnection = new GraphLayoutConnection(net);
			newConnection = true;
		}

		if (!layoutConnection.isLayedOut()) {
			// shown for the first time.
			layoutConnection.expandAll();
		}

		ProMGraphModel model = new ProMGraphModel(net);
		graph = new ProMJGraph(model, map, layoutConnection);

		JGraphHierarchicalLayout layout = new JGraphHierarchicalLayout();
		layout.setDeterministic(false);
		layout.setCompactLayout(false);
		layout.setFineTuning(true);
		layout.setParallelEdgeSpacing(15);
		layout.setFixRoots(false);

		layout.setOrientation(map.get(net, AttributeMap.PREF_ORIENTATION, SwingConstants.SOUTH));

		if (!layoutConnection.isLayedOut()) {

			JGraphFacade facade = new JGraphFacade(graph);

			facade.setOrdered(false);
			facade.setEdgePromotion(true);
			facade.setIgnoresCellsInGroups(false);
			facade.setIgnoresHiddenCells(false);
			facade.setIgnoresUnconnectedCells(false);
			facade.setDirected(true);
			facade.resetControlPoints();

			facade.run(layout, true);

			java.util.Map<?, ?> nested = facade.createNestedMap(true, true);
			graph.getGraphLayoutCache().edit(nested);

		}

		graph.setUpdateLayout(layout);

		graph.addGraphSelectionListener(new GraphSelectionListener() {
			public void valueChanged(GraphSelectionEvent e) {
/*				System.out.println("Event #"+(serial++));
				System.out.println("Cell:"+e.getCell());
				System.out.println(e.getCells().length+" Cells added:");
				for (Object obj: e.getCells())
					System.out.println(obj);
				System.out.println("isAdded:"+e.isAddedCell());*/
				// selection of a transition would change the stats
				graph.getModel().beginUpdate();
				if (e.getCell() instanceof ProMGraphCell) {
					DirectedGraphNode cell = ((ProMGraphCell) e.getCell()).getNode();
					if (cell instanceof Transition) {
					} else if (cell instanceof Place) {
					}
					selection.add(cell);
					graph.getViewSpecificAttributes().putViewSpecific(cell, AttributeMap.FILLCOLOR,
							involvedMoveOnLogColor);
				}
				for (int i = 1; i < e.getCells().length; i++) {
					if (e.getCells()[i] instanceof ProMGraphCell) {
						DirectedGraphNode cell = ((ProMGraphCell) e.getCells()[i]).getNode();
						selection.remove(cell);
						graph.getViewSpecificAttributes().putViewSpecific(cell,
								AttributeMap.FILLCOLOR, transparentColor);
					}
				}
				boolean enabled=false;
				if (isChoice) {
					enabled = true;
					if (selection.size() >= 1) {
						for (Object obj: selection.toArray()) {
							if (!(obj instanceof Place) || !isChoicePlace((Place) obj))
								enabled = false;
						} 	
					}
				} else if (selection.size() > 0) {
					enabled = true;
					for (Object obj: selection.toArray()) {
						if (obj instanceof Place || obj instanceof Arc)
							enabled = false;
					}
				}
				ok.setEnabled(enabled);
				graph.getModel().endUpdate();
				graph.refresh();
			}
		});

		ProMJGraphPanel panel = new ProMJGraphPanel(graph);

		panel.addViewInteractionPanel(new PIPInteractionPanel(panel), SwingConstants.NORTH);
		panel.addViewInteractionPanel(new ZoomInteractionPanel(panel, ScalableViewPanel.MAX_ZOOM), SwingConstants.WEST);
//		panel.addViewInteractionPanel(new ExportInteractionPanel(panel), SwingConstants.SOUTH);

		layoutConnection.updated();

		if (newConnection) {
			context.getConnectionManager().addConnection(layoutConnection);
		}

//		for (Place p : marking) {
//			String label = "" + marking.occurrences(p);
//			graph.getViewSpecificAttributes().putViewSpecific(p, AttributeMap.LABEL, label);
//			graph.getViewSpecificAttributes().putViewSpecific(p, AttributeMap.SHOWLABEL, !label.equals(""));
//		}
		for (Object obj: selection.toArray()) {
			if (obj instanceof DirectedGraphNode) {
				DirectedGraphNode cell = (DirectedGraphNode) obj;
				graph.getViewSpecificAttributes().putViewSpecific(cell, AttributeMap.FILLCOLOR,
						involvedMoveOnLogColor);
			}
		}

//		JScrollPane scroll = new JScrollPane(((ScalableComponent) graph).getComponent());
//		SlickerDecorator.instance().decorate(scroll, Color.WHITE, Color.GRAY, Color.DARK_GRAY);
//		setLayout(new BorderLayout());
//		add(scroll);
		
		contentPane.add(panel, BorderLayout.CENTER);

		JPanel wrapper = new JPanel(new FlowLayout());
				
		// Add button to bottom
		JPanel bottomPanel = new JPanel(new BorderLayout(0,0));
		contentPane.add(bottomPanel, BorderLayout.SOUTH);

		ok = new JButton("OK");
		ok.addActionListener(
			new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					selected = selection;
					dialog.setVisible(false);
					dialog.dispose();
				}
			}
		);
		ok.setEnabled(false);
		wrapper.add(ok);

		bottomPanel.add(wrapper, BorderLayout.SOUTH);

		dialog.setContentPane(contentPane);

		// make viz
		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		dialog.setPreferredSize(new Dimension(1000, 700));
		dialog.pack();
		
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
		return selected;
	}
	
	public boolean isChoicePlace(Place place) {
		Set<Place> allPlaces = new HashSet<Place>();
		allPlaces.addAll(net.getPlaces());
		if (!allPlaces.contains(place)) {
			return false;
		}
		Set<Transition> allTransitions = new HashSet<Transition>();
		allTransitions.addAll(net.getTransitions());
		
		int num = 0;
		
		for (Transition transition : allTransitions) {
			Arc arc = net.getArc(place, transition);
			if (arc != null) {
				num++;
			} 
		}
		if (num > 1) {
			return true;
		}
		return false;
	}

}
