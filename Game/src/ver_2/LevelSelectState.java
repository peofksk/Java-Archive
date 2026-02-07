package ver_2;

import java.awt.Graphics2D;
import java.awt.Image;

public class LevelSelectState implements GameState {
	private final JAVA_Archive game;
	private Image background, arrowLeft, arrowRight, titleUnwelcome, titleAfter, titleComing;
	private Music selectMusic, sampleUnwelcome, sampleAfter, sampleComing;
	private String[] music_arr = { "unwelcomeSchool", "afterSchoolDessert", "test" };
	
	public LevelSelectState(JAVA_Archive game) {
		this.game = game;
	}
	
	@Override
	public void enter() {
		background = AssetManager.getInstance().getImage("selecton_bg");
		arrowLeft = AssetManager.getInstance().getImage("arrow_left");
		arrowRight = AssetManager.getInstance().getImage("arrow_right");
		titleUnwelcome = AssetManager.getInstance().getImage("title_unwelcome");
		titleAfter = AssetManager.getInstance().getImage("title_after");
		titleComing = AssetManager.getInstance().getImage("title_Coming");
		selectMusic = new Music("sample_unwelcomeSchool.mp3", true);
		selectMusic.start();
	}
	
	@Override
    public void update() {	    	
	    
	}
	
	@Override
	public void render(Graphics2D g) {
		g.drawImage(background, 0, 0, null);
		g.drawImage(arrowLeft, 200, 400, null);
		g.drawImage(arrowRight, 800, 400, null);
	}

	@Override
	public void exit() {

	}

	

}
