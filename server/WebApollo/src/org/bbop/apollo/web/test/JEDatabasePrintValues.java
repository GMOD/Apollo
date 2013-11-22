package org.bbop.apollo.web.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;

public class JEDatabasePrintValues extends JFrame {

	public JEDatabasePrintValues() {
		init();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		pack();
		setLocationRelativeTo(null);
	}
	
	private void init() {
		boolean valid = false;
		String path = null;
		while (!valid) {
			path = JOptionPane.showInputDialog("Enter JE database path");
			if (path == null || path.length() == 0) {
				JOptionPane.showMessageDialog(null, "Provide a JE database path", "Error", JOptionPane.ERROR_MESSAGE);
				continue;
			}
			File f = new File(path);
			if (!f.exists()) {
				JOptionPane.showMessageDialog(null, "JE database path does not exist", "Error", JOptionPane.ERROR_MESSAGE);
				continue;
			}
			valid = true;
		}
		setTitle("Contents of " + path);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add(panel);
		
		JEDatabase db = new JEDatabase(path, true);

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(path);
		List<Feature> features = new ArrayList<Feature>();
		db.readFeatures(features);
		processFeatures(root, features, "Features");
		List<Feature> sequenceAlterations = new ArrayList<Feature>();
		db.readSequenceAlterations(sequenceAlterations);
		processFeatures(root, sequenceAlterations, "Sequence alterations");
		
		JTree tree = new JTree(root);
		JScrollPane scrollPane = new JScrollPane(tree);
		panel.add(scrollPane);

	}
	
	private void processFeatures(DefaultMutableTreeNode node, List<Feature> features, String label) {
		DefaultMutableTreeNode featuresNode = new DefaultMutableTreeNode(label);
		node.add(featuresNode);
		for (Feature feature : features) {
			processFeature(featuresNode, feature);
		}
	}
	
	private void processFeature(DefaultMutableTreeNode parentNode, Feature feature) {
		FeatureLocation loc = feature.getFeatureLocations().iterator().next();
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(String.format("%s (%d, %d, %d) [%s]", feature.getUniqueName(), loc.getFmin(), loc.getFmax(), loc.getStrand(), feature.getType().toString()));
		parentNode.add(node);
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			processFeature(node, fr.getSubjectFeature());
		}
	}

	public static void main(String [] args) {
		new JEDatabasePrintValues();
	}
	
}
