package ver_1;

import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.ImageIcon;

public class Note extends Thread {
	
	private int perfectY = 400;
	private int x, y = perfectY - (1000 / Main.SLEEP_TIME * Main.NOTE_SPEED) * Main.REACH_TIME;
	private String noteType;
	private boolean proceeded = true;

	private Image note = new ImageIcon(Main.class.getResource("../Asset/note_" + JAVA_Archive.note_state + ".png"))
			.getImage();

	public String getNoteType() {
		return noteType;
	}

	public boolean isProceeded() {
		return proceeded;
	}

	public void close() {
		proceeded = false;
	}

	public Note(String noteType) {
		if (noteType.equals("D")) {
			x = 54;
		} else if (noteType.equals("F")) {
			x = 144;
		} else if (noteType.equals("J")) {
			x = 234;
		} else if (noteType.equals("K")) {
			x = 324;
		}
		this.noteType = noteType;
	}

	public void screenDraw(Graphics2D g) {
		g.drawImage(note, x, y, null);
	}

	public void drop() {
		y += Main.NOTE_SPEED;
		if (y > 484) {
			close();
		}
	}

	@Override
	public void run() {
		try {
			while (true) {
				drop();
				if (proceeded) {
					Thread.sleep(Main.SLEEP_TIME);
				} else {
					interrupt();
					break;
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public String judge() {
		if (y >= perfectY + 42) {
			close();
			return "Late";
		} else if (y >= perfectY + 33) {
			close();
			return "Good";
		} else if (y >= perfectY + 15) {
			close();
			return "Great";
		} else if (y >= perfectY - 15) {
			close();
			return "Perfect";
		} else if (y >= perfectY - 33) {
			close();
			return "Great";
		} else if (y >= perfectY - 42) {
			close();
			return "Good";
		} else if (y >= perfectY - 48) {
			close();
			return "Early";
		}
		return "Miss";
	}

	public int getY() {
		return y;
	}
}
