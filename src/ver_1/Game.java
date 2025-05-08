package ver_1;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;

public class Game extends Thread {

	private Music gameMusic;
	private JAVA_Archive javaArchive;

	ArrayList<Note> noteList = new ArrayList<Note>();

	private String titleName;
	private String lastJudge = "";
	private String difficulty;
	private int bpm;
	private int noteNum = 877; // unwelcomeSchool 기준. 후에 각 노래마다 변경 필요.

	float alpha = 0.0f;
	int combo = 0;
	public int maxCombo = 0;
	public int score = 0;
	public int missCount = 0;
	public int lateCount = 0;
	public int earlyCount = 0;
	public int goodCount = 0;
	public int greatCount = 0;
	public int perfectCount = 0;
	public double accuracy = 100.0;

	private Image gameBar = new ImageIcon(Main.class.getResource("../Asset/gameBar.png")).getImage();
	private Image judgementLine = new ImageIcon(Main.class.getResource("../Asset/judgementLine.png")).getImage();
	private Image noteLinePressed = new ImageIcon(Main.class.getResource("../Asset/noteLinePressed.png")).getImage();
	private Image noteLineEndPressed = new ImageIcon(Main.class.getResource("../Asset/noteLineEndPressed.png"))
			.getImage();
	private Image noteLineReleased = new ImageIcon(Main.class.getResource("../Asset/noteLine.png")).getImage();
	private Image noteLineEndReleased = new ImageIcon(Main.class.getResource("../Asset/noteLineEnd.png")).getImage();
	private Image noteLineD = noteLineReleased;
	private Image noteLineF = noteLineReleased;
	private Image noteLineJ = noteLineReleased;
	private Image noteLineK = noteLineEndReleased;

	public Game(Music gameMusic, String titleName, String difficulty, int bpm, JAVA_Archive javaArchive) {
		this.gameMusic = gameMusic;
		this.titleName = titleName;
		this.difficulty = difficulty;
		this.bpm = bpm;
		this.javaArchive = javaArchive;
		gameMusic.setOnCompletionListener(new Music.OnCompletionListener() {
			@Override
			public void onCompletion() {
				close();
			}
		});
		gameMusic.start();
	}

	public void screenDraw(Graphics2D g) {
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g.drawImage(gameBar, 0, 516, null);
		g.drawImage(judgementLine, 54, 383, null);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
		g.drawImage(noteLineD, 54, 30, null);
		g.drawImage(noteLineF, 144, 30, null);
		g.drawImage(noteLineJ, 234, 30, null);
		g.drawImage(noteLineK, 324, 30, null);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		
		for (int i = 0; i < noteList.size(); i++) {
			Note note = noteList.get(i);

			if (note.getY() > 484) {
				score -= 10;
				combo = 0;
				accuracy -= 100.0 / noteNum;
				lastJudge = "Miss";
			}

			if (!note.isProceeded()) {
				noteList.remove(i);
				i--;
			} else {
				note.screenDraw(g);
			}
		}

		if (maxCombo < combo) {
			maxCombo = combo;
		}
		
		g.setFont(new Font("Arial", Font.BOLD, 24));
		g.setColor(Color.CYAN);
		g.drawString("SCORE : ", 600, 250);
		g.drawString(String.valueOf(score), 740, 250);
		g.drawString("COMBO : ", 600, 280);
		g.drawString("MAX COMBO : ", 570, 310);
		g.drawString(String.valueOf(combo), 740, 280);
		g.drawString(String.valueOf(maxCombo), 740, 310);
		g.drawString("Accuracy : ", 570, 370);
		g.drawString(String.format("%.2f", accuracy), 740, 370);
		
		g.setFont(new Font("Arial", Font.BOLD, 140));
		g.setColor(Color.WHITE);
		g = (Graphics2D) g.create();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getAlpha()));
		drawCenteredString(g, String.valueOf(combo), 230, 370);
		
		g.setFont(new Font("Arial", Font.BOLD, 50));
		switch (lastJudge) {
		case "Perfect":
			g.setColor(Color.BLUE);
			break;
		case "Great":
			g.setColor(Color.CYAN);
			break;
		case "Good":
			g.setColor(Color.GREEN);
			break;
		case "Early":
		case "Late":
			g.setColor(Color.ORANGE);
			break;
		case "Miss":
			g.setColor(Color.GRAY);
			break;
		}
		drawCenteredString(g, String.valueOf(lastJudge), 230, 220);
		g.dispose();
	}

	private void drawCenteredString(Graphics2D g, String text, int centerX, int y) {
		FontMetrics metrics = g.getFontMetrics(g.getFont());
		int textWidth = metrics.stringWidth(text);
		int startX = centerX - (textWidth / 2);
		g.drawString(text, startX, y);
	}
	
	public float getAlpha() {
		alpha = Math.max(alpha - 0.005f, 0);
		return alpha;
	}

	public void fadeOut() {
		while (true) {
			fadeOut();
			try {
				Thread.sleep(Main.SLEEP_TIME);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}

	public void pressD() {
		judge("D");
		noteLineD = noteLinePressed;
	}

	public void releaseD() {
		noteLineD = noteLineReleased;
	}

	public void pressF() {
		judge("F");
		noteLineF = noteLinePressed;
	}

	public void releaseF() {
		noteLineF = noteLineReleased;
	}

	public void pressJ() {
		judge("J");
		noteLineJ = noteLinePressed;
	}

	public void releaseJ() {
		noteLineJ = noteLineReleased;
	}

	public void pressK() {
		judge("K");
		noteLineK = noteLineEndPressed;
	}

	public void releaseK() {
		noteLineK = noteLineEndReleased;
	}

	@Override
	public void run() {
		dropNote();
	}

	public void close() {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		gameMusic.close();
		javaArchive.gameResult();
		this.interrupt();
	}

	public void dropNote() {
		Beat[] beats = null;

		ArrayList<Double> time = new ArrayList<>();
		ArrayList<String> noteType = new ArrayList<>();

		InputStream in;
		BufferedReader br;
		String readfile = "";

		try {
			in = getClass().getResourceAsStream("../Asset/" + titleName + "_" + difficulty + ".txt");
			br = new BufferedReader(new InputStreamReader((in)));
			while ((readfile = br.readLine()) != null) {
				StringTokenizer note_stk = new StringTokenizer(readfile, " ");
				time.add(Double.parseDouble(note_stk.nextToken()));
				noteType.add(note_stk.nextToken());
			}

			int gap = Main.CORRECTION - Main.REACH_TIME;
			double minBeat = 1.0 / (bpm / 60.0) / 4.0 * 1000.0; // 16분음표(4분의 4박자 기준)
			beats = new Beat[time.size()];

			for (int j = 0; j < time.size(); j++) {
				beats[j] = new Beat((time.get(j) * minBeat) + gap, noteType.get(j));
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		int i = 0;

		while (true) {
			boolean dropped = false;

			if (beats[i].getTime() <= gameMusic.getTime()) {
				Note note = new Note(beats[i].getNoteName());
				note.start();
				noteList.add(note);
				i++;

				dropped = true;
			}
			if (dropped) {
				try {
					Thread.sleep(5);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void judge(String input) {
		for (int i = 0; i < noteList.size(); i++) {
			Note nowNote = noteList.get(i);

			if (input.equals(nowNote.getNoteType())) {
				judgeEvent(nowNote.judge());
				break;
			}
		}
	}

	public void judgeEvent(String judge) {
		if (judge.equals("Miss")) {
			score -= 10;
			combo = 0;
			lastJudge = "Miss";
			missCount++;
			accuracy -= 100.0 / noteNum;
		} else if (judge.equals("Late")) {
			score += 5;
			combo += 1;
			lastJudge = "Late";
			lateCount++;
			accuracy -= 100.0 / noteNum * 0.08;
		} else if (judge.equals("Early")) {
			score += 10;
			combo += 1;
			lastJudge = "Early";
			earlyCount++;
			accuracy -= 100.0 / noteNum * 0.06;
		} else if (judge.equals("Good")) {
			score += 20;
			combo += 1;
			lastJudge = "Good";
			goodCount++;
			accuracy -= 100.0 / noteNum * 0.04;
		} else if (judge.equals("Great")) {
			score += 30;
			combo += 1;
			lastJudge = "Great";
			greatCount++;
			accuracy -= 100.0 / noteNum * 0.02;
		} else if (judge.equals("Perfect")) {
			score += 50;
			combo += 1;
			lastJudge = "Perfect";
			perfectCount++;
			accuracy -= 100.0 / noteNum * 0;
		}
		alpha = 0.8f;
	}

	public Music musicTime() {
		return gameMusic;
	}
}