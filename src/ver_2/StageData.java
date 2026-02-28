package ver_2;

import java.util.HashMap;
import java.util.Map;


public class StageData {

    private String title;
    private String musicPath;
    private String backgroundPath;
    
    private Map<Difficulty, String> noteFileMap;

    public StageData(String title,
                     String musicPath,
                     String backgroundPath) {

        this.title = title;
        this.musicPath = musicPath;
        this.backgroundPath = backgroundPath;

        noteFileMap = new HashMap<>();
    }

    public void addNoteFile(Difficulty difficulty, String path) {
        noteFileMap.put(difficulty, path);
    }

    public String getNoteFile(Difficulty difficulty) {
        return noteFileMap.get(difficulty);
    }

    public String getTitle() {
        return title;
    }
}