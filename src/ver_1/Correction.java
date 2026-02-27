package ver_1;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import javax.swing.ImageIcon;

public class Correction extends Thread {

	private Music correctionMusic = new Music("correctionMusic.mp3", true);
	private JAVA_Archive javaArchive;
	private String lastJudge = "";
	
	private CorrectionNote_noThread correctionNote_noThread = new CorrectionNote_noThread();

	public Correction() {
		correctionMusic.setOnCompletionListener(new Music.OnCompletionListener() {
			@Override
			public void onCompletion() {
				close();
			}
		});
		correctionMusic.start();
	}

	public void screenDraw(Graphics2D g) {
		Graphics2D g2d = (Graphics2D) g.create();
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g2d.drawImage(new ImageIcon(Main.class.getResource("../Asset/gameBar.png")).getImage(), 0, 516, null);
		g2d.drawImage(new ImageIcon(Main.class.getResource("../Asset/correctionJudgementLine.png")).getImage(), 54, 286,
				null);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
		g2d.drawImage(new ImageIcon(Main.class.getResource("../Asset/noteLineEnd.png")).getImage(), 54, 30, null);
		g2d.drawImage(new ImageIcon(Main.class.getResource("../Asset/noteLineEnd.png")).getImage(), 134, 30, null);
		g2d.drawImage(new ImageIcon(Main.class.getResource("../Asset/noteLineEnd.png")).getImage(), 214, 30, null);
		g2d.drawImage(new ImageIcon(Main.class.getResource("../Asset/noteLineEnd.png")).getImage(), 294, 30, null);
		g2d.dispose();
		g.setFont(new Font("Arial", Font.BOLD, 30));
		g.setColor(Color.WHITE);
		g.drawString(String.valueOf(lastJudge), 180, 100);
		correctionNote_noThread.screenDraw(g);
	}

	public void pressSpace() {
		judge("Space");
	}

	public void releaseSpace() {
	}

	@Override
	public void run() {
		dropCorrectionNote();
	}

	public void close() {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		javaArchive.musicSelection();
		this.interrupt();
	}

	public void dropCorrectionNote() {
		Beat[] beats = null;

		int gap = Main.CORRECTION - Main.REACH_TIME;
		beats = new Beat[1];
		beats[0] = new Beat(gap, "Space");

		new CorrectionNote_noThread();
	}

	public void judge(String input) {
		CorrectionNote_noThread correctionNote_noThread = new CorrectionNote_noThread();
		judgeEvent(correctionNote_noThread.judge());
	}

	public void judgeEvent(String judge) {
		if (judge.equals("Miss")) {
			lastJudge = "Miss";
		} else if (judge.equals("Late")) {
			lastJudge = "Late";
		} else if (judge.equals("Early")) {
			lastJudge = "Early";
		} else if (judge.equals("Good")) {
			lastJudge = "Good";
		} else if (judge.equals("Great")) {
			lastJudge = "Great";
		} else if (judge.equals("Perfect")) {
			lastJudge = "Perfect";
		}
	}

	public Music musicTime() {
		return correctionMusic;
	}
	
	public int getMusicTime() {
		return correctionMusic.getTime();
	}
}