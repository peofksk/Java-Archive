package state.gameplay;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.util.EnumMap;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.Difficulty;
import stage.Stage;
import state.ResultState;
import util.RenderUtils;

public class GamePlayState implements GameState {

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();
	private NoteManager nm;
	private final Stage stage;
	private final Difficulty difficulty;

	private Image background;
	private Image judgementLine;
	private Image noteImage;

	private boolean preloaded = false;
	private boolean started = false;
	private volatile boolean paused = false;
	private boolean resultRequested = false;

	private volatile boolean musicThreadStarted = false;
	private volatile boolean audioStarted = false;

	private static final double LEAD_IN = 3.0;
	private static final double AUDIO_OUTPUT_LATENCY = 0.2;

	private volatile long songStartNano = 0L;
	private long pauseStartNano = 0L;

	private double timelineTime = -LEAD_IN;

	private int score = 0;
	private int combo = 0;
	private int maxCombo = 0;

	private String lastJudge = "";
	private float alpha = 0.0f;

	private double accuracy = 100.0;
	private int totalNoteCount = 1;

	private final boolean[] lanePressed = new boolean[Lane.values().length];
	private final EnumMap<Judgement, Integer> judgementCounts = new EnumMap<>(Judgement.class);

	public GamePlayState(GameContext context, Stage stage, Difficulty difficulty) {
		this.context = context;
		this.stage = stage;
		this.difficulty = difficulty;
		resetJudgementCounts();
	}

	public void preload() {
		if (preloaded) {
			return;
		}

		background = am.getImage(stage.getBackgroundImageKey());
		judgementLine = am.getImage("judgement_line");
		noteImage = am.getImage("note_" + context.getNoteIndex());

		nm = new NoteManager(context, stage.getMusicBPM(), stage.getMusicOffsetSeconds());
		nm.loadChart("note_" + stage.getLevelName() + "_" + difficulty.name().toLowerCase());

		totalNoteCount = nm.getRemainingNoteCount();
		if (totalNoteCount <= 0) {
			totalNoteCount = 1;
		}

		context.bgm.load(stage.getMusicPath());

		preloaded = true;
	}

	@Override
	public void enter() {
		context.bgm.stop();

		if (!preloaded) {
			preload();
		} else {
			nm.loadChart("note_" + stage.getLevelName() + "_" + difficulty.name().toLowerCase());
			totalNoteCount = nm.getRemainingNoteCount();
			if (totalNoteCount <= 0) {
				totalNoteCount = 1;
			}
			context.bgm.load(stage.getMusicPath());
		}

		long now = System.nanoTime();
		songStartNano = now + (long) (LEAD_IN * 1_000_000_000L);

		timelineTime = -LEAD_IN;
		musicThreadStarted = false;
		audioStarted = false;
		started = true;
		paused = false;
		resultRequested = false;

		pauseStartNano = 0L;

		score = 0;
		combo = 0;
		maxCombo = 0;

		lastJudge = "";
		alpha = 0.0f;
		accuracy = 100.0;

		resetJudgementCounts();
		clearLanePressedStates();
		startScheduledMusicThread();
	}

	@Override
	public void exit() {
		clearLanePressedStates();
		context.bgm.stop();
	}

	@Override
	public void update(double deltaTime) {
		if (!started || paused || resultRequested) {
			return;
		}

		updateTimelineTime();

		double gameTime = getGameplayTime();

		int missCountThisFrame = nm.update(gameTime);
		for (int i = 0; i < missCountThisFrame; i++) {
			applyJudgement(Judgement.MISS);
		}

		if (audioStarted && !context.bgm.isPlaying() && timelineTime > 0 && nm.isFinished()) {
			clearLanePressedStates();
			resultRequested = true;
			context.changeState(new ResultState(context, buildGameResult()));
		}
	}

	@Override
	public void render(Graphics2D g) {
		g.drawImage(background, 0, 0, null);
		drawLane(g);
		g.drawImage(judgementLine, 30, 383, null);
		drawLaneKeyBindings(g);

		int baseY = 383;
		int laneWidth = 80;
		int startX = 30;
		double speed = 600;

		double gameTime = getGameplayTime();

		for (Lane lane : Lane.values()) {
			int laneIndex = lane.ordinal();
			int x = startX + laneIndex * laneWidth;

			for (Note note : nm.getLaneNotes().get(lane)) {
				double y = baseY - (note.getHitTime() - gameTime) * speed;
				g.drawImage(noteImage, x, (int) y, null);
			}
		}

		drawGameHUD(g);

		if (paused) {
			java.awt.Composite original = g.getComposite();

			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, 1024, 576);

			g.setColor(Color.WHITE);
			g.setFont(new Font("Arial", Font.BOLD, 40));
			RenderUtils.drawCenteredString(g, "PAUSED", 0, 220, 1024, 50);

			g.setFont(new Font("Arial", Font.PLAIN, 22));
			RenderUtils.drawCenteredString(g, "Press ESC to Resume", 0, 285, 1024, 28);
			RenderUtils.drawCenteredString(g, "Press Enter to Exit", 0, 320, 1024, 28);

			g.setComposite(original);
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (paused) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				context.changeState(new ResultState(context, buildGameResult()));
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				resumeGame();
			}
			return;
		}

		if (!started || resultRequested) {
			return;
		}

		if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			pauseGame();
			return;
		}

		Lane inputLane = context.getLaneForKeyCode(e.getKeyCode());
		if (inputLane != null) {
			if (lanePressed[inputLane.ordinal()]) {
				return;
			}

			setLanePressed(inputLane, true);
		}

		updateTimelineTime();

		Judgement result = Judgement.NONE;
		double judgeTime = getGameplayTime();

		if (inputLane != null) {
			result = nm.judge(inputLane, judgeTime);

			if (result == Judgement.NONE) {
				result = Judgement.MISS;
			}
		}

		applyJudgement(result);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		Lane inputLane = context.getLaneForKeyCode(e.getKeyCode());
		if (inputLane != null) {
			setLanePressed(inputLane, false);
		}
	}

	private void updateTimelineTime() {
		if (audioStarted) {
			timelineTime = context.bgm.getPositionSeconds();
			return;
		}

		long now = System.nanoTime();
		timelineTime = ((now - songStartNano) / 1_000_000_000.0) + AUDIO_OUTPUT_LATENCY;
	}

	private double getGameplayTime() {
		return timelineTime + context.getGlobalOffset();
	}

	private void startScheduledMusicThread() {
		if (musicThreadStarted) {
			return;
		}

		musicThreadStarted = true;

		Thread t = new Thread(() -> {
			try {
				while (true) {
					long playRequestNano = songStartNano - (long) (AUDIO_OUTPUT_LATENCY * 1_000_000_000L);
					long now = System.nanoTime();
					long remain = playRequestNano - now;

					if (remain <= 0) {
						break;
					}

					if (paused) {
						Thread.sleep(1);
						continue;
					}

					if (remain > 2_000_000L) {
						Thread.sleep(1);
					} else {
						Thread.yield();
					}
				}

				context.bgm.playLoaded(false);
				audioStarted = true;

			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		t.setDaemon(true);
		t.start();
	}

	private void pauseGame() {
		paused = true;
		pauseStartNano = System.nanoTime();

		if (context.bgm.isPlaying()) {
			context.bgm.pause();
		}
	}

	private void resumeGame() {
		long now = System.nanoTime();
		long pausedDuration = now - pauseStartNano;

		songStartNano += pausedDuration;
		paused = false;

		if (context.bgm.isPaused()) {
			context.bgm.resume();
		}
	}

	private void applyJudgement(Judgement result) {
		if (result == Judgement.NONE) {
			return;
		}

		judgementCounts.put(result, judgementCounts.getOrDefault(result, 0) + 1);

		switch (result) {
		case MISS -> {
			score -= 10;
			combo = 0;
			lastJudge = "Miss";
			accuracy -= 100.0 / totalNoteCount;
		}
		case LATE -> {
			score += 5;
			combo += 1;
			lastJudge = "Late";
			accuracy -= (100.0 / totalNoteCount) * 0.08;
		}
		case EARLY -> {
			score += 10;
			combo += 1;
			lastJudge = "Early";
			accuracy -= (100.0 / totalNoteCount) * 0.06;
		}
		case GOOD -> {
			score += 20;
			combo += 1;
			lastJudge = "Good";
			accuracy -= (100.0 / totalNoteCount) * 0.04;
		}
		case GREAT -> {
			score += 30;
			combo += 1;
			lastJudge = "Great";
			accuracy -= (100.0 / totalNoteCount) * 0.02;
		}
		case PERFECT -> {
			score += 50;
			combo += 1;
			lastJudge = "Perfect";
		}
		case NONE -> {
			return;
		}
		}

		if (accuracy < 0) {
			accuracy = 0;
		}

		if (combo > maxCombo) {
			maxCombo = combo;
		}

		alpha = 0.8f;
	}

	private void drawGameHUD(Graphics2D g) {
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

		Graphics2D g2 = (Graphics2D) g.create();

		g2.setFont(new Font("Arial", Font.BOLD, 140));
		g2.setColor(Color.WHITE);
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getAlpha()));
		RenderUtils.drawCenteredString(g2, String.valueOf(combo), 30, 270, 320, 120);

		g2.setFont(new Font("Arial", Font.BOLD, 50));
		switch (lastJudge) {
		case "Perfect" -> g2.setColor(Color.BLUE);
		case "Great" -> g2.setColor(Color.CYAN);
		case "Good" -> g2.setColor(Color.GREEN);
		case "Early", "Late" -> g2.setColor(Color.ORANGE);
		case "Miss" -> g2.setColor(Color.GRAY);
		default -> g2.setColor(Color.WHITE);
		}

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(alpha, 0.25f)));
		RenderUtils.drawCenteredString(g2, lastJudge, 30, 180, 320, 80);

		g2.dispose();
	}

	private float getAlpha() {
		alpha = Math.max(alpha - 0.02f, 0f);
		return alpha;
	}

	public void drawLane(Graphics2D g) {
		int startX = 30;
		int laneWidth = 80;
		int laneHeight = 576;

		java.awt.Composite original = g.getComposite();

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

		for (int i = 0; i < Lane.values().length; i++) {
			int x = startX + i * laneWidth;

			g.setColor(new Color(13, 13, 13));
			g.fillRect(x, 0, laneWidth, laneHeight);

			g.setColor(new Color(21, 21, 21));
			g.fillRect(x + 5, 0, laneWidth - 10, laneHeight);

			if (lanePressed[i]) {
				drawLaneGlow(g, x, laneWidth, laneHeight);
			}

			g.setColor(new Color(42, 42, 42));
			g.fillRect(x, 0, 2, laneHeight);
			g.fillRect(x + laneWidth - 2, 0, 2, laneHeight);
		}

		g.setComposite(original);
	}

	private void drawLaneGlow(Graphics2D g, int x, int laneWidth, int laneHeight) {
		Graphics2D g2 = (Graphics2D) g.create();

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f));
		g2.setColor(new Color(120, 220, 255));
		g2.fillRect(x, 0, laneWidth, laneHeight);

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
		g2.setPaint(new GradientPaint(x + laneWidth / 2f, 0f, new Color(235, 250, 255), x + laneWidth / 2f, laneHeight,
				new Color(90, 200, 255)));
		g2.fillRect(x + 8, 0, laneWidth - 16, laneHeight);

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
		g2.setColor(new Color(220, 245, 255));
		g2.fillRect(x + laneWidth / 2 - 10, 0, 20, laneHeight);

		g2.dispose();
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

	private void setLanePressed(Lane lane, boolean pressed) {
		lanePressed[lane.ordinal()] = pressed;
	}

	private void clearLanePressedStates() {
		for (int i = 0; i < lanePressed.length; i++) {
			lanePressed[i] = false;
		}
	}

	private void resetJudgementCounts() {
		judgementCounts.clear();

		for (Judgement judgement : Judgement.values()) {
			if (judgement == Judgement.NONE) {
				continue;
			}
			judgementCounts.put(judgement, 0);
		}
	}

	private GameResult buildGameResult() {
		return new GameResult(score, maxCombo, accuracy, judgementCounts);
	}
}