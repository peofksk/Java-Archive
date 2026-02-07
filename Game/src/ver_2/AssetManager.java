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

    private AssetManager() {}

    public static AssetManager getInstance() {
        return instance;
    }

    void loadImage(String key, String path) {
        Image img = new ImageIcon(getClass().getResource(path)).getImage();
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
            System.err.println("텍스트 로드 실패: " + path);
            e.printStackTrace();
        }

        texts.put(key, lines);
    }


    public Image getImage(String key) {
        return images.get(key);
    }

    public ArrayList<String> getText(String key) {
        return texts.get(key);
    }
}
