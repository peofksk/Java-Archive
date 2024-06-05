package ver_1;

import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.ImageIcon;

public class Note extends Thread {
	private Image note = new ImageIcon(Main.class.getResource("../Asset/note.png")).getImage();
	private int x, y = 436 - (1000 / Main.SLEEP_TIME * Main.NOTE_SPEED) * Main.REACH_TIME;
	private String noteType;
	private boolean proceeded = true;

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
		}else if (noteType.equals("F")) {
			x = 144;
		} else if (noteType.equals("J")) {
			x = 234;
		} else if (noteType.equals("K")) {
			x = 324;
		}
		this.noteType = noteType; // reset note type
	}

	public void screenDraw(Graphics2D g) {
		g.drawImage(note, x, y, null);
	}

	public void drop() {
		y += Main.NOTE_SPEED;
		if (y > 556) { // if note goes beyond the judge bar
			// System.out.println("Miss");
			close();
		}
	}

	@Override
	public void run() { // run the thread
		try {
			while (true) {
				drop();
				if (proceeded) {
					Thread.sleep(Main.SLEEP_TIME);// sleep is based on 0.001 seconds; for us sleep time is set to 10;
													// while statement runs 100 times in 1 sec; in 1 sec, the note drops
													// 700 px per second
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
		if (y >= 469) {
			// System.out.println("Late");
			close();
			return "Late";
		}
		else if (y >= 456) {
			// System.out.println("Good");
			close();
			return "Good";
		}
		else if (y >= 443) {
			// System.out.println("Great");
			close();
			return "Great";
		}
		else if (y >= 429) {
			// System.out.println("Perfect");
			close();
			return "Perfect";
		}
		else if (y >= 421) {
			// System.out.println("Great");
			close();
			return "Great";
		}
		else if (y >= 406) {
			// System.out.println("Good");
			close();
			return "Good";
		}
		else if (y >= 391) {
			// System.out.println("Early");
			close();
			return "Early";
		}
		return "Miss";
	}

	public int getY() {
		return y;
	}
}
