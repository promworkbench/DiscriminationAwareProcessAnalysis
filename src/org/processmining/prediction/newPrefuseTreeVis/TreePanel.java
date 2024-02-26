package org.processmining.prediction.newPrefuseTreeVis;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;

import javax.swing.AbstractAction;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.ItemAction;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.LocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.filter.FisheyeTreeFilter;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Node;
import prefuse.data.Tree;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.sort.TreeDepthItemSorter;

public class TreePanel extends Display {

	/** for serialization. */
	private static final long serialVersionUID = 8262123080545898882L;

	public class OrientAction extends AbstractAction {
		private int orientation;

		public OrientAction(int orientation) {
			this.orientation = orientation;
		}

		public void actionPerformed(ActionEvent evt) {
			setOrientation(orientation);
			getVisualization().cancel("orient");
			getVisualization().run("treeLayout");
			getVisualization().run("orient");
		}
	}

	public class AutoPanAction extends Action {
		private Point2D m_start = new Point2D.Double();
		private Point2D m_end = new Point2D.Double();
		private Point2D m_cur = new Point2D.Double();
		private int m_bias = 150;

		public void run(double frac) {
			TupleSet ts = m_vis.getFocusGroup(Visualization.FOCUS_ITEMS);
			if (ts.getTupleCount() == 0)
				return;

			if (frac == 0.0) {
				int xbias = 0, ybias = 0;
				switch (m_orientation) {
					case Constants.ORIENT_LEFT_RIGHT :
						xbias = m_bias;
						break;
					case Constants.ORIENT_RIGHT_LEFT :
						xbias = -m_bias;
						break;
					case Constants.ORIENT_TOP_BOTTOM :
						ybias = m_bias;
						break;
					case Constants.ORIENT_BOTTOM_TOP :
						ybias = -m_bias;
						break;
				}

				VisualItem vi = (VisualItem) ts.tuples().next();
				m_cur.setLocation(getWidth() / 2, getHeight() / 2);
				getAbsoluteCoordinate(m_cur, m_start);
				m_end.setLocation(vi.getX() + xbias, vi.getY() + ybias);
			} else {
				m_cur.setLocation(m_start.getX() + frac * (m_end.getX() - m_start.getX()), m_start.getY() + frac
						* (m_end.getY() - m_start.getY()));
				panToAbs(m_cur);
			}
		}
	}

	public static class NodeColorAction extends ColorAction {
		Map<String, Color> variableColor;
		BigDecimal maxValue;
		BigDecimal minValue;
		Color[] palette;

		public NodeColorAction(String group, Map<String, Color> variableColor, BigDecimal minValue,
				BigDecimal maxValue, Color[] colorScale) {
			super(group, VisualItem.FILLCOLOR, ColorLib.rgba(255, 0, 0, 0));
			this.variableColor = variableColor;
			this.maxValue = maxValue;
			this.minValue = minValue;
			this.palette = colorScale;
		}

		public int getColor(VisualItem item) {
			//If color is given by classifier color the nodes according to perspective
			Color color = variableColor.get(item.getString("name"));
			if (color != null) {
				return color.getRGB();
			} else {
				//If color is not given color the nodes according to the values of the nodes and maxValue(mainly needed for leaves)
				if (item instanceof NodeItem) {
					NodeItem nitem = (NodeItem) item;
					// maxvalue == -1 means it is a string and it dont need to be recolored
					if (nitem.getChildCount() == 0) {
						if (maxValue.compareTo(minValue) >  0 && minValue.compareTo(BigDecimal.ZERO) >  0 && item.getString("name").contains("[")) {
							BigDecimal itemValue = parseLeaf(nitem);
							BigDecimal scale = maxValue.subtract(minValue);
							BigDecimal itemValueScale = (itemValue.subtract(minValue));
							BigDecimal colorValue = BigDecimal.valueOf(-1);
							try{
								colorValue = itemValueScale.divide(scale);
								
							} catch(Exception e){
								colorValue = itemValueScale.divide(scale, BigDecimal.ROUND_HALF_EVEN);
							}
							//System.out.println(itemValue.doubleValue() + " " + itemValueScale.doubleValue()+" "+colorValue.doubleValue());
							
							return ColorLib.interp(palette[0].getRGB(), palette[1].getRGB(), colorValue.doubleValue());
						} else {
							return ColorLib.interp(palette[0].getRGB(), palette[1].getRGB(), 1);
						}
					}
				}
				return Color.gray.getRGB();
			}
		}
	}

	public static class TextColorAction extends ColorAction {
		Map<String, Color> variableColor;

		public TextColorAction(String group, Map<String, Color> variableColor) {
			super(group, VisualItem.TEXTCOLOR, ColorLib.rgba(255, 0, 0, 0));
			this.variableColor = variableColor;
		}

		public int getColor(VisualItem item) {
			Color color = variableColor.get(item.getString("name"));
			if (color != null) {
				double y = 0.2126 * Math.pow(color.getRed() / 255F, 2.2) + 0.7151
						* Math.pow(color.getGreen() / 255F, 2.2) + 0.0721 * Math.pow(color.getBlue() / 255F, 2.2);
				if (y < 0.18) {
					return ColorLib.rgb(255, 255, 255);
				} else {
					return ColorLib.rgb(0, 0, 0);
				}
			} else {
				return ColorLib.rgb(0, 0, 0);
			}
		}
	}

	private static final String tree = "tree";
	private static final String treeNodes = "tree.nodes";
	private static final String treeEdges = "tree.edges";

	private LabelRenderer m_nodeRenderer;
	private LabelRenderer m_valueRenderer;
	private NewEdgeRenderer m_edgeRenderer;

	private String m_label = "name";

	//One could switch between left-right to top bottom orientation
	private int m_orientation = Constants.ORIENT_LEFT_RIGHT;

	//private int m_orientation = Constants.ORIENT_TOP_BOTTOM;

	/**
	 * Initializes the panel.
	 * 
	 * @param tree2
	 *            the tree to visualize
	 * @param config
	 */
	public TreePanel(Tree tree2, Map<String, Color> variableColor, VisConfigurables config) {
		super(new Visualization());
		m_vis.add(tree, tree2);
		m_nodeRenderer = new LabelRenderer(m_label);
		m_nodeRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
		m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
		m_nodeRenderer.setRoundedCorner(5, 5);

		m_valueRenderer = new LabelRenderer("edge");
		m_valueRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
		m_valueRenderer.setHorizontalAlignment(Constants.CENTER);
		m_valueRenderer.setRoundedCorner(5, 5);

		m_edgeRenderer = new NewEdgeRenderer(Constants.EDGE_TYPE_CURVE);
		//m_edgeRenderer = new EdgeRenderer(Constants.EDGE_TYPE_CURVE);

		//DefaultRendererFactory rf = new DefaultRendererFactory(m_nodeRenderer, m_edgeRenderer);
		DefaultRendererFactory rf = new DefaultRendererFactory();
		rf.add(new InGroupPredicate(treeNodes), m_nodeRenderer);
		rf.add(new InGroupPredicate(treeEdges), m_edgeRenderer);
		m_vis.setRendererFactory(rf);

		//find maxValue so that leaves can be colored accordingly
		BigDecimal maxValue = maxValue(tree2.getRoot(), BigDecimal.ZERO);
		BigDecimal minValue = minValue(tree2.getRoot(), maxValue);
		//System.out.println("MINMAX = " + minValue + " " + maxValue);
		// colors
		ItemAction nodeColor = new NodeColorAction(treeNodes, variableColor, minValue, maxValue, config.colorScale);
		ItemAction textColor = new TextColorAction(treeNodes, variableColor);
		ColorAction edgeColor = new ColorAction(treeEdges, VisualItem.STROKECOLOR, ColorLib.gray(200));

		//Paint textcolor
		m_vis.putAction("textColor", textColor);

		// quick repaint
		ActionList repaint = new ActionList();
		repaint.add(nodeColor);
		repaint.add(new RepaintAction());
		m_vis.putAction("repaint", repaint);

		// full paint
		ActionList fullPaint = new ActionList();
		fullPaint.add(nodeColor);
		m_vis.putAction("fullPaint", fullPaint);

		// animate paint change
		ActionList animatePaint = new ActionList(400);
		animatePaint.add(new ColorAnimator(treeNodes));
		animatePaint.add(new RepaintAction());
		m_vis.putAction("animatePaint", animatePaint);

		// create the tree layout action
		NodeLinkTreeLayout treeLayout = new NodeLinkTreeLayout(tree, m_orientation, 10, 10, 10);
		treeLayout.setLayoutAnchor(new Point2D.Double(100, 300));
		m_vis.putAction("treeLayout", treeLayout);

		CollapsedSubtreeLayout subLayout = new CollapsedSubtreeLayout(tree, m_orientation);
		m_vis.putAction("subLayout", subLayout);

		

		// create the filtering and layout
		ActionList filter = new ActionList();
		filter.add(new FisheyeTreeFilter(tree, 2));
		filter.add(new FontAction(treeNodes, FontLib.getFont("Tahoma", config.fontSize)));
		filter.add(treeLayout);
		filter.add(subLayout);
		filter.add(textColor);
		filter.add(nodeColor);
		filter.add(edgeColor);
		m_vis.putAction("filter", filter);

		// animated transition
		ActionList animate = new ActionList(2000);
		animate.setPacingFunction(new SlowInSlowOutPacer());

		animate.add(new QualityControlAnimator());
		animate.add(new VisibilityAnimator(tree));
		animate.add(new LocationAnimator(treeNodes));
		animate.add(new ColorAnimator(treeNodes));
		animate.add(new RepaintAction());
		m_vis.putAction("animate", animate);
		m_vis.alwaysRunAfter("filter", "animate");

		// create animator for orientation changes
		ActionList orient = new ActionList(2000);
		orient.setPacingFunction(new SlowInSlowOutPacer());

		orient.add(new QualityControlAnimator());
		orient.add(new LocationAnimator(treeNodes));
		orient.add(new RepaintAction());
		m_vis.putAction("orient", orient);

		// ------------------------------------------------
		// initialize the display
		setSize(1920, 1080);
		setItemSorter(new TreeDepthItemSorter());
		addControlListener(new ZoomToFitControl());
		addControlListener(new ZoomControl());
		addControlListener(new WheelZoomControl());
		addControlListener(new PanControl());
		addControlListener(new NewFocusControl(1,"filter"));
		addControlListener(new NewFocusControl(2,"filter"));
		// ------------------------------------------------

		// filter graph and perform layout
		setOrientation(m_orientation);
		m_vis.run("filter");
		m_vis.run("repaint");
		m_vis.run("fullPaint");
	};

	//Parses a leaf to integerer. When the leaf is a interval it returns the average value. Otherwise it returns the label itself.
	private static BigDecimal parseLeaf(Node leaf) {

		String string = leaf.getString("name")/*.replace(']', '[')*/;
		BigDecimal result = BigDecimal.valueOf(-1);
		String[] split = string.split(",");
		try {
			if (split.length > 1) {
				split[0] = split[0].replace("[", "").trim();
				split[1] = split[1].replace("[", "").trim();

				String[] split1 = split[0].split(" ");
				String[] split2 = split[1].split(" ");
				
				BigDecimal bg0 = new BigDecimal(split1[0]);
				BigDecimal bg1 = new BigDecimal(split2[0]);
				BigDecimal resultbg = bg0.add(bg1).divide(BigDecimal.valueOf(2));

				return resultbg;
			} else {
				String[] split1 = split[0].split(" ");
				result = new BigDecimal(split1[0]);
			}
		} catch (Exception e) {

		}
		return result;
	}

	//Returns the maximum value of the tree leaves
	private BigDecimal maxValue(Node root, BigDecimal max) {
		BigDecimal result = max;
		if (root.getChildCount() == 0) {
			BigDecimal value = parseLeaf(root);
			if (value.compareTo(max) > 0) {
				result = value;
			}
		} else {
			Iterator children = root.children();
			while (children.hasNext()) {
				Node child = (Node) children.next();
				max = maxValue(child, max);
			}
			result = max;
		}
		return result;
	}

	//returns minimum value of the tree leaves
	private BigDecimal minValue(Node root, BigDecimal min) {
		BigDecimal result = min;
		if (root.getChildCount() == 0) {
			BigDecimal value = parseLeaf(root);
			if (value.compareTo(min) < 0) {
				result = value;
			}
		} else {
			Iterator children = root.children();
			while (children.hasNext()) {
				Node child = (Node) children.next();
				min = minValue(child, min);
			}
			result = min;
		}
		return result;
	}

	public void setOrientation(int orientation) {
		NodeLinkTreeLayout rtl = (NodeLinkTreeLayout) m_vis.getAction("treeLayout");
		CollapsedSubtreeLayout stl = (CollapsedSubtreeLayout) m_vis.getAction("subLayout");
		switch (orientation) {
			case Constants.ORIENT_LEFT_RIGHT :
				m_nodeRenderer.setHorizontalAlignment(Constants.LEFT);
				m_edgeRenderer.setHorizontalAlignment1(Constants.RIGHT);
				m_edgeRenderer.setHorizontalAlignment2(Constants.LEFT);
				m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
				m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
				break;
			case Constants.ORIENT_RIGHT_LEFT :
				m_nodeRenderer.setHorizontalAlignment(Constants.RIGHT);
				m_edgeRenderer.setHorizontalAlignment1(Constants.LEFT);
				m_edgeRenderer.setHorizontalAlignment2(Constants.RIGHT);
				m_edgeRenderer.setVerticalAlignment1(Constants.CENTER);
				m_edgeRenderer.setVerticalAlignment2(Constants.CENTER);
				break;
			case Constants.ORIENT_TOP_BOTTOM :
				m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
				m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
				m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
				m_edgeRenderer.setVerticalAlignment1(Constants.BOTTOM);
				m_edgeRenderer.setVerticalAlignment2(Constants.TOP);
				break;
			case Constants.ORIENT_BOTTOM_TOP :
				m_nodeRenderer.setHorizontalAlignment(Constants.CENTER);
				m_edgeRenderer.setHorizontalAlignment1(Constants.CENTER);
				m_edgeRenderer.setHorizontalAlignment2(Constants.CENTER);
				m_edgeRenderer.setVerticalAlignment1(Constants.TOP);
				m_edgeRenderer.setVerticalAlignment2(Constants.BOTTOM);
				break;
			default :
				throw new IllegalArgumentException("Unrecognized orientation value: " + orientation);
		}
		m_orientation = orientation;
		rtl.setOrientation(orientation);
		stl.setOrientation(orientation);
	}

	public int getOrientation() {
		return m_orientation;
	}
}