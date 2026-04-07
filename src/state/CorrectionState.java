package state;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.CorrectionConfig;
import state.gameplay.Judgement;
import state.gameplay.Lane;
import state.gameplay.Note;
import state.gameplay.NoteManager;
import util.RenderUtils;

public class CorrectionState implements GameState {

	private final GameContext context;
	private final CorrectionConfig correctionConfig;
	private final AssetManager am = AssetManager.getInstance();

	private NoteManager nm;

	private Image background;
	private Image judgementLine;
	private Image noteImage;

	private boolean preloaded = false;
	private volatile boolean musicThreadStarted = false;

	private static final double LEAD_IN = 3.0;

	private long stateEnterNano = 0L;
	private boolean audioStarted = false;
	private double timelineTime = -LEAD_IN;

	private String lastJudge = "";
	private float judgeAlpha = 0.0f;
	private double lastDiffMs = 0.0;

	private long hitCount = 0;
	private double sumAbsDiffMs = 0.0;
	private double sumSignedDiffMs = 0.0;

	private boolean resultMode = false;
	private boolean offsetApplied = false;

	public CorrectionState(GameContext context, CorrectionConfig correctionConfig) {
		this.context = context;
		this.correctionConfig = correctionConfig;
	}

	@Override
	public void enter() {
		resetRunState();
		context.bgm.stop();

		if (!preloaded) {
			preload();
		}

		stateEnterNano = System.nanoTime();
		audioStarted = false;
		musicThreadStarted = false;
		timelineTime = -LEAD_IN;

		startScheduledMusicThread();
	}

	@Override
	public void update(double deltaTime) {
		updateTimelineTime();

		double gameTime = timelineTime + context.getGlobalOffset();

		if (!resultMode) {
			int missCount = nm.update(gameTime);
			for (int i = 0; i < missCount; i++) {
				applyJudgement(Judgement.MISS);
			}

			if (audioStarted && !context.bgm.isPlaying() && timelineTime > 0 && nm.isFinished()) {
				resultMode = true;
				applyOffsetOnce();
			}
		}

		judgeAlpha = Math.max(0.0f, judgeAlpha - 0.02f);
	}

	@Override
	public void render(Graphics2D g) {
		if (background != null) {
			g.drawImage(background, 0, 0, null);
		} else {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 1024, 576);
		}

		drawLane(g);

		if (judgementLine != null) {
			g.drawImage(judgementLine, 30, 383, null);
		}

		drawLaneKeyBindings(g);

		if (!resultMode) {
			double gameTime = timelineTime + context.getGlobalOffset();
			renderNotes(g, gameTime);
			renderHud(g);
		} else {
			renderResult(g);
		}
	}

	@Override
	public void exit() {
		context.bgm.stop();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();

		if (key == KeyEvent.VK_ESCAPE) {
			context.changeState(new LevelSelectState(context));
			return;
		}

		if (resultMode) {
			if (key == KeyEvent.VK_ENTER) {
				retry();
			}
			return;
		}

		updateTimelineTime();

		double judgeTime = timelineTime + context.getGlobalOffset();
		Judgement result = Judgement.NONE;

		Lane inputLane = context.getLaneForKeyCode(key);
		if (inputLane != null) {
			result = nm.judge(inputLane, judgeTime);
		}

		applyJudgement(result);
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	public void preload() {
		if (preloaded) {
			return;
		}

		background = am.getImage(correctionConfig.getBackgroundImageKey());
		judgementLine = am.getImage("judgement_line");
		noteImage = am.getImage("note_" + context.getNoteIndex());

		nm = new NoteManager(context, correctionConfig.getMusicBPM(), correctionConfig.getMusicOffset());
		nm.loadChart(correctionConfig.getNoteFilePath());

		context.bgm.load(correctionConfig.getMusicPath());

		preloaded = true;
	}

	private void updateTimelineTime() {
		if (audioStarted) {
			timelineTime = context.bgm.getPositionSeconds();
			return;
		}

		double sinceEnter = (System.nanoTime() - stateEnterNano) / 1_000_000_000.0;
		timelineTime = sinceEnter - LEAD_IN;
	}

	private void retry() {
		context.bgm.stop();
		resetRunState();

		nm.loadChart(correctionConfig.getNoteFilePath());

		stateEnterNano = System.nanoTime();
		audioStarted = false;
		musicThreadStarted = false;
		timelineTime = -LEAD_IN;

		startScheduledMusicThread();
	}

	private void resetRunState() {
		lastJudge = "";
		judgeAlpha = 0.0f;
		lastDiffMs = 0.0;
		hitCount = 0;
		sumAbsDiffMs = 0.0;
		sumSignedDiffMs = 0.0;
		resultMode = false;
		offsetApplied = false;
	}

	private void applyOffsetOnce() {
		if (offsetApplied) {
			return;
		}

		offsetApplied = true;

		double avgSignedMs = hitCount > 0 ? (sumSignedDiffMs / hitCount) : 0.0;
		context.setGlobalOffset(context.getGlobalOffset() - (avgSignedMs / 1000.0));
	}

	private void startScheduledMusicThread() {
		if (musicThreadStarted) {
			return;
		}

		musicThreadStarted = true;

		Thread t = new Thread(() -> {
			try {
				long target = stateEnterNano + (long) (LEAD_IN * 1_000_000_000L);

				while (true) {
					long now = System.nanoTime();
					long remain = target - now;

					if (remain <= 0) {
						break;
					}

					if (remain > 2_000_000L) {
						Thread.sleep(1);
					} else {
						Thread.yield();
					}
				}

				context.bgm.playLoaded(false);
				audioStarted = true;

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		t.setDaemon(true);
		t.start();
	}

	private void renderNotes(Graphics2D g, double gameTime) {
		int baseY = 383;
		int laneWidth = 80;
		int startX = 30;
		double speed = 800;

		Map<Lane, List<Note>> notesByLane = nm.getLaneNotes();

		for (Lane lane : Lane.values()) {
			int laneIndex = lane.ordinal();
			int x = startX + laneIndex * laneWidth;

			List<Note> notes = notesByLane.get(lane);
			if (notes == null) {
				continue;
			}

			for (Note note : notes) {
				double y = baseY - (note.getHitTime() - gameTime) * speed;

				if (noteImage != null) {
					g.drawImage(noteImage, x, (int) y, null);
				} else {
					g.setColor(Color.CYAN);
					g.fillRect(x + 10, (int) y, 60, 16);
				}
			}
		}
	}

	private void applyJudgement(Judgement j) {
		if (j == Judgement.NONE) {
			return;
		}

		judgeAlpha = 0.85f;

		switch (j) {
		case PERFECT -> lastJudge = "Perfect";
		case GREAT -> lastJudge = "Great";
		case GOOD -> lastJudge = "Good";
		case EARLY -> lastJudge = "Early";
		case LATE -> lastJudge = "Late";
		case MISS -> lastJudge = "Miss";
		default -> lastJudge = "";
		}

		if (j != Judgement.MISS) {
			lastDiffMs = nm.getLastHitTimeDiffSeconds() * 1000.0;
			hitCount++;
			sumAbsDiffMs += Math.abs(lastDiffMs);
			sumSignedDiffMs += lastDiffMs;
		} else {
			lastDiffMs = 0.0;
		}
	}

	private void renderHud(Graphics2D g) {
		Graphics2D g2 = (Graphics2D) g.create();

		try {
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(judgeAlpha, 0.25f)));
			g2.setFont(new Font("SansSerif", Font.BOLD, 48));

			if ("Perfect".equals(lastJudge)) {
				g2.setColor(Color.BLUE);
			} else if ("Great".equals(lastJudge)) {
				g2.setColor(Color.CYAN);
			} else if ("Good".equals(lastJudge)) {
				g2.setColor(Color.GREEN);
			} else if ("Early".equals(lastJudge) || "Late".equals(lastJudge)) {
				g2.setColor(Color.ORANGE);
			} else if ("Miss".equals(lastJudge)) {
				g2.setColor(Color.GRAY);
			} else {
				g2.setColor(Color.WHITE);
			}

			RenderUtils.drawCenteredString(g2, lastJudge, 30, 180, 320, 80);

			g2.setFont(new Font("SansSerif", Font.BOLD, 28));
			if (!lastJudge.isEmpty() && !"Miss".equals(lastJudge)) {
				RenderUtils.drawCenteredString(g2, String.format("%+.1f ms", lastDiffMs), 30, 240, 320, 70);
			}
		} finally {
			g2.dispose();
		}
	}

	private void drawLaneKeyBindings(Graphics2D g) {
		Graphics2D g2 = (Graphics2D) g.create();

		try {
			int startX = 30;
			int laneWidth = 80;
			int textY = 405;
			int textHeight = 42;

			g2.setFont(new Font("Arial", Font.BOLD, 28));

			for (Lane lane : Lane.values()) {
				int laneX = startX + lane.ordinal() * laneWidth;
				String keyText = context.getKeyTextForLane(lane);

				RenderUtils.drawOutlinedCenteredString(g2, keyText, laneX, textY, laneWidth, textHeight, Color.WHITE,
						new Color(0, 0, 0, 180));
			}
		} finally {
			g2.dispose();
		}
	}

	private void renderResult(Graphics2D g) {
		Graphics2D g2 = (Graphics2D) g.create();

		try {
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, 1024, 576);

			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			g2.setColor(Color.WHITE);
			g2.setFont(new Font("SansSerif", Font.BOLD, 44));
			RenderUtils.drawCenteredString(g2, "RESULT", 0, 120, 1024, 70);

			double avgAbs = hitCount > 0 ? (sumAbsDiffMs / hitCount) : 0.0;
			double avgSigned = hitCount > 0 ? (sumSignedDiffMs / hitCount) : 0.0;

			g2.setFont(new Font("SansSerif", Font.PLAIN, 26));
			RenderUtils.drawCenteredString(g2, "Hits: " + hitCount, 0, 215, 1024, 40);
			RenderUtils.drawCenteredString(g2, String.format("Avg Error (abs): %.2f ms", avgAbs), 0, 265, 1024, 40);
			RenderUtils.drawCenteredString(g2, String.format("Avg Error (signed): %+.2f ms", avgSigned), 0, 305, 1024,
					40);
			RenderUtils.drawCenteredString(g2, String.format("Global Offset: %+.4f s", context.getGlobalOffset()), 0,
					355, 1024, 40);

			g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
			RenderUtils.drawCenteredString(g2, "ENTER: Retry ESC: Back", 0, 420, 1024, 35);
		} finally {
			g2.dispose();
		}
	}

	private void drawLane(Graphics2D g) {
		int startX = 30;
		int laneWidth = 80;
		java.awt.Composite original = g.getComposite();

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

		for (int i = 0; i < 4; i++) {
			int x = startX + i * laneWidth;

			g.setColor(new Color(13, 13, 13));
			g.fillRect(x, 0, laneWidth, 576);

			g.setColor(new Color(21, 21, 21));
			g.fillRect(x + 5, 0, laneWidth - 10, 576);

			g.setColor(new Color(42, 42, 42));
			g.fillRect(x, 0, 2, 576);
			g.fillRect(x + laneWidth - 2, 0, 2, 576);
		}

		g.setComposite(original);
	}
}