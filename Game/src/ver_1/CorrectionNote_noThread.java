package ver_1;

import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.ImageIcon;

public class CorrectionNote_noThread {
	
	private int x, y, perfectY, error;
	private double minBeat = 1.0 / (112 / 60.0) / 4.0 * 1000.0; // 16분음표(4분의 4박자 기준)
	
	private Image note = new ImageIcon(Main.class.getResource("../Asset/noteLong.png")).getImage();
	private Music correctionMusic = new Music("correctionMusic.mp3", true);
	
	public CorrectionNote_noThread() {
		this.x = 54;
		this.y = 303;
		this.minBeat = 1.0 / (112.0 / 60.0) / 4.0 * 1000.0; // 112 = correctionMusic.mp3 bpm
	}	
	
	public void screenDraw(Graphics2D g) {
		g.drawImage(note, x, y, null);
		g.drawString(String.valueOf(error), 600, 100);
	}
	
	public void drop() {
		while (true) {
			System.out.println("오 이제 실행 되는뎁쇼?");
			y = (int)(-84 * Math.sin(Math.PI / (4 * minBeat) * (correctionMusic.getTime() - 2000)) + 303);
			try {
				Thread.sleep(Main.SLEEP_TIME);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}
	
	public String judge() {
		if (y >= perfectY + 42) {
			return "Late";
		} else if (y >= perfectY + 33) {
			return "Good";
		} else if (y >= perfectY + 15) {
			return "Great";
		} else if (y >= perfectY - 15) {
			return "Perfect";
		} else if (y >= perfectY - 33) {
			return "Great";
		} else if (y >= perfectY - 42) {
			return "Good";
		} else if (y >= perfectY - 48) {
			return "Early";
		}
		error = y - perfectY;
		System.out.println(error);
		return "Miss";
	}

	public int getY() {
		return y;
	}

	public int getError() {
		return error;
	}
}
