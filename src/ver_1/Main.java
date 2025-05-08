package ver_1;

public class Main {
	
	public static final int SCREEN_WIDTH = 1024;
	public static final int SCREEN_HEIGHT = 576;
	public static final int NOTE_SPEED = 6; //the speed at which the note drops
	public static final int SLEEP_TIME = 10; //in order to drop notes in evenly spanned time
	public static final int REACH_TIME = 2; //time needed for note to reach the judge bar
	public static final int CORRECTION = 70; // synchronize the note to music, 노트 빨리 내려오면 올리고 늦게 내려오면 내리세요
	public static void main(String[] args) {
		
		new JAVA_Archive();
	}
}
