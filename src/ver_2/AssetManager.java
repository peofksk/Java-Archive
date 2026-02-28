package ver_2;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.ArrayList;
import javax.swing.ImageIcon;

public class AssetManager {

	private static final AssetManager instance = new AssetManager();

	private HashMap<String, Image> images = new HashMap<>();
	private HashMap<String, ArrayList<String>> texts = new HashMap<>();

	private AssetManager() {
	}

	public static AssetManager getInstance() {
		return instance;
	}

	void loadImage(String key, String path) {
		java.net.URL url = getClass().getResource(path);

		if (url == null) {
			System.err.println("[AssetManager] Image resource not found: " + path);
			return;
		}

		Image img = new ImageIcon(url).getImage();
		images.put(key, img);

	}

	void loadText(String key, String path) {
		ArrayList<String> lines = new ArrayList<>();

		try {
			InputStream in = getClass().getResourceAsStream(path);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			br.close();
		} catch (Exception e) {
			System.err.println("[AssetManager] Text load failed: " + path);
			e.printStackTrace();
		}

		texts.put(key, lines);
	}

	public Image getImage(String key) {
		Image img = images.get(key);
		if (img == null) {
			System.err.println("[AssetManager] Image not found: " + key);
		}
		return img;
	}

	public ArrayList<String> getText(String key) {
		ArrayList<String> text = texts.get(key);
		if (text == null)
			System.err.println("[AssetManager] Text not found: " + key);
		return text;
	}
}
