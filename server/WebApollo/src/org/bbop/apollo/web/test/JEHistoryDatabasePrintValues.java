package org.bbop.apollo.web.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.bbop.apollo.web.datastore.JEDatabase;
import org.bbop.apollo.web.datastore.history.JEHistoryDatabase;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.simpleObject.Feature;

public class JEHistoryDatabasePrintValues extends JFrame {

	public JEHistoryDatabasePrintValues() {
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
			path = JOptionPane.showInputDialog("Enter JE history database path");
			if (path == null || path.length() == 0) {
				JOptionPane.showMessageDialog(null, "Provide a JE history database path", "Error", JOptionPane.ERROR_MESSAGE);
				continue;
			}
			File f = new File(path);
			if (!f.exists()) {
				JOptionPane.showMessageDialog(null, "JE history database path does not exist", "Error", JOptionPane.ERROR_MESSAGE);
				continue;
			}
			valid = true;
		}
		setTitle("Contents of " + path);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		add(panel);
		
		JEHistoryDatabase db = new JEHistoryDatabase(path, true, 0);
		Collection<String> keys = db.getKeys();
		panel.add(new JLabel("Keys"));
		JList keyList = new JList(keys.toArray());
		panel.add(keyList);
	}

	public static void main(String [] args) {
		new JEHistoryDatabasePrintValues();
	}
	
}
