package app;

import javax.swing.SwingUtilities;

import asset.Init;

public class Main {
	public static void main(String[] args) {
		Init.loadAssets();
		SwingUtilities.invokeLater(JAVA_Archive::new);
	}
}
