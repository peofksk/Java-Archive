package state.gameplay;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import asset.AssetManager;
import asset.Init;
import core.GameContext;
import core.GameState;
import stage.Difficulty;
import stage.Stage;
import state.ResultState;
import util.RenderUtils;

public class GamePlayState implements GameState {
	private static final int SCREEN_WIDTH = 1024;
	private static final int SCREEN_HEIGHT = 576;
	private static final int PLAYFIELD_START_X = 30;
	private static final int PLAYFIELD_WIDTH = 320;
	private static final int JUDGEMENT_LINE_Y = 383;

	private static final double LEAD_IN = 3.0;
	private static final double NOTE_SCROLL_SPEED = 600.0;

	private final GameContext context;
	private final AssetManager am = AssetManager.getInstance();
	private final Stage stage;
	private final Difficulty difficulty;

	private NoteManager nm;
	private AutoRobot autoRobot;

	private Image background;
	private Image judgementLine;
	private Image noteImage;
	private Color noteThemeColor = new Color(120, 220, 255);

	private boolean preloaded = false;
	private boolean started = false;
	private volatile boolean paused = false;
	private boolean resultRequested = false;

	private volatile boolean audioStarted = false;
	private double leadInRemainingSeconds = LEAD_IN;
	private double timelineTime = -LEAD_IN;

	private int score = 0;
	private int combo = 0;
	private int maxCombo = 0;
	private String lastJudge = "";
	private float alpha = 0.0f;
	private double accuracy = 100.0;
	private int totalNoteCount = 1;

	private boolean autoInputInProgress = false;
	private final Component autoEventSource = new Canvas();

	private final Map<Lane, Boolean> lanePressed = new HashMap<>();
	private final EnumMap<Judgement, Integer> judgementCounts = new EnumMap<>(Judgement.class);

	public GamePlayState(GameContext context, Stage stage, Difficulty difficulty) {
		this.context = context;
		this.stage = stage;
		this.difficulty = difficulty;

		initializeLanePressedMap();
		resetJudgementCounts();
	}

	public void preload() {
		if (preloaded) {
			return;
		}

		background = am.getImage(stage.getBackgroundImageKey());
		judgementLine = am.getImage("judgement_line");

		loadNoteSkin();
		loadNoteManager();

		context.bgm.load(stage.getMusicPath());
		preloaded = true;
	}

	@Override
	public void enter() {
		context.bgm.stop();

		if (!preloaded) {
			preload();
		} else {
			loadNoteSkin();
			loadNoteManager();
			context.bgm.load(stage.getMusicPath());
		}

		leadInRemainingSeconds = LEAD_IN;
		timelineTime = -LEAD_IN;
		audioStarted = false;
		started = true;
		paused = false;
		resultRequested = false;

		score = 0;
		combo = 0;
		maxCombo = 0;
		lastJudge = "";
		alpha = 0.0f;
		accuracy = 100.0;

		resetJudgementCounts();
		clearLanePressedStates();
	}

	private void loadNoteSkin() {
		noteImage = am.getImage("note_" + context.getNoteIndex());
		noteThemeColor = extractDominantNoteColor(noteImage);
	}

	private void loadNoteManager() {
		nm = new NoteManager(context, stage.getMusicBPM(), stage.getMusicOffsetSeconds());
		nm.loadChart(buildChartKey());
		totalNoteCount = Math.max(1, nm.getRemainingNoteCount());

		if (context.isAutoMode()) {
			autoRobot = new AutoRobot(this, context, nm);
		} else {
			autoRobot = null;
		}
	}

	private String buildChartKey() {
		return Init.buildChartKey(stage.getLevelName(), difficulty, context.getKeyMode());
	}

	@Override
	public void exit() {
		clearLanePressedStates();
		context.bgm.stop();
	}

	@Override
	public void update(double deltaTime) {
		if (!started || resultRequested) {
			return;
		}

		if (paused) {
			return;
		}

		advanceTimelineTime(deltaTime);

		double gameTime = getGameplayTime();

		if (autoRobot != null) {
			autoRobot.update(gameTime);
		}

		int missCountThisFrame = nm.update(gameTime, lanePressed);

		for (int i = 0; i < missCountThisFrame; i++) {
			applyJudgement(Judgement.MISS);
		}

		for (Judgement judgement : nm.drainPendingJudgements()) {
			applyJudgement(judgement);
		}

		if (audioStarted && !context.bgm.isPlaying() && timelineTime > 0 && nm.isFinished()) {
			clearLanePressedStates();
			resultRequested = true;
			context.changeState(new ResultState(context, buildGameResult()));
		}
	}

	@Override
	public void render(Graphics2D g) {
		LaneLayout layout = getLaneLayout();

		g.drawImage(background, 0, 0, null);

		drawLane(g, layout);
		drawJudgementLine(g, layout);
		drawScorePanelBackground(g);
		drawLaneKeyBindings(g, layout);
		renderNotes(g, layout, getGameplayTime());
		drawGameHUD(g);
		drawAutoStatus(g);

		if (paused) {
			java.awt.Composite original = g.getComposite();

			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

			g.setColor(Color.WHITE);
			g.setFont(new Font("Arial", Font.BOLD, 40));
			RenderUtils.drawCenteredString(g, "PAUSED", 0, 220, SCREEN_WIDTH, 50);

			g.setFont(new Font("Arial", Font.PLAIN, 22));
			RenderUtils.drawCenteredString(g, "Press ESC to Resume", 0, 285, SCREEN_WIDTH, 28);
			RenderUtils.drawCenteredString(g, "Press Enter to Exit", 0, 320, SCREEN_WIDTH, 28);

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

		if (context.isAutoMode() && inputLane != null && !autoInputInProgress) {
			return;
		}

		if (inputLane != null) {
			if (isLanePressed(inputLane)) {
				return;
			}

			setLanePressed(inputLane, true);
		}

		updateTimelineTime();

		Judgement result = Judgement.NONE;
		double judgeTime = getGameplayTime();

		if (inputLane != null) {
			result = nm.judge(inputLane, judgeTime);

			if (result == Judgement.NONE && !nm.hasActiveLongNote(inputLane)) {
				result = Judgement.MISS;
			}
		}

		applyJudgement(result);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (!started || paused || resultRequested) {
			return;
		}

		Lane inputLane = context.getLaneForKeyCode(e.getKeyCode());

		if (inputLane == null) {
			return;
		}

		if (context.isAutoMode() && !autoInputInProgress) {
			return;
		}

		updateTimelineTime();

		double judgeTime = getGameplayTime();
		Judgement result = nm.judgeLongNoteEnd(inputLane, judgeTime);

		setLanePressed(inputLane, false);
		applyJudgement(result);
	}

	void dispatchAutoKeyPressed(int keyCode) {
		dispatchAutoKeyEvent(KeyEvent.KEY_PRESSED, keyCode);
	}

	void dispatchAutoKeyReleased(int keyCode) {
		dispatchAutoKeyEvent(KeyEvent.KEY_RELEASED, keyCode);
	}

	private void dispatchAutoKeyEvent(int eventId, int keyCode) {
		Component source = context.getGamePanel();

		if (source == null) {
			source = autoEventSource;
		}

		KeyEvent event = new KeyEvent(
				source,
				eventId,
				System.currentTimeMillis(),
				0,
				keyCode,
				KeyEvent.CHAR_UNDEFINED
		);

		autoInputInProgress = true;

		try {
			if (eventId == KeyEvent.KEY_PRESSED) {
				keyPressed(event);
			} else if (eventId == KeyEvent.KEY_RELEASED) {
				keyReleased(event);
			}
		} finally {
			autoInputInProgress = false;
		}
	}

	private LaneLayout getLaneLayout() {
		return new LaneLayout(context.getPlayableLanes(), PLAYFIELD_START_X, PLAYFIELD_WIDTH, SCREEN_HEIGHT);
	}

	private void renderNotes(Graphics2D g, LaneLayout layout, double gameTime) {
		List<Lane> lanes = layout.getLanes();

		for (Lane lane : lanes) {
			int x = layout.getLaneX(lane);
			int laneWidth = layout.getLaneWidth(lane);

			for (Note note : nm.getLaneNotes().getOrDefault(lane, Collections.emptyList())) {
				drawGameplayNote(g, x, laneWidth, note, false, gameTime);
			}

			Note activeLongNote = nm.getActiveLongNote(lane);
			if (activeLongNote != null) {
				drawGameplayNote(g, x, laneWidth, activeLongNote, true, gameTime);
			}
		}
	}

	private void drawGameplayNote(Graphics2D g, int laneX, int laneWidth, Note note, boolean active, double gameTime) {
		if (note.isLongNote()) {
			drawLongNote(g, laneX, laneWidth, note, active, gameTime);
			return;
		}

		int noteHeight = getNoteDrawHeight();
		int y = getNoteTopY(note.getHitTime(), gameTime, noteHeight);
		drawNoteHead(g, laneX, laneWidth, y);
	}

	private void drawLongNote(Graphics2D g, int laneX, int laneWidth, Note note, boolean active, double gameTime) {
		double headTime = active ? Math.max(note.getHitTime(), gameTime) : note.getHitTime();

		int drawWidth = getNoteDrawWidth(laneWidth);
		int drawHeight = getNoteDrawHeight();
		int drawX = laneX + (laneWidth - drawWidth) / 2;

		int headY = getNoteTopY(headTime, gameTime, drawHeight);
		int tailY = getNoteTopY(note.getEndTime(), gameTime, drawHeight);

		int topY = Math.min(headY, tailY);
		int bottomY = Math.max(headY, tailY);

		int bodyX = drawX + Math.max(2, drawWidth / 6);
		int bodyWidth = Math.max(10, drawWidth - Math.max(4, drawWidth / 3));
		int bodyTop = topY + drawHeight / 2;
		int bodyBottom = bottomY + drawHeight / 2;
		int bodyHeight = Math.max(0, bodyBottom - bodyTop);

		if (bodyHeight > 0 && bodyBottom >= -drawHeight && bodyTop <= SCREEN_HEIGHT + drawHeight) {
			Graphics2D g2 = (Graphics2D) g.create();

			try {
				Color bodyColor = withAlpha(noteThemeColor, 225);
				Color shineColor = brighten(noteThemeColor, 1.7f, 100);

				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
				g2.setColor(bodyColor);
				g2.fillRoundRect(bodyX, bodyTop, bodyWidth, bodyHeight, 12, 12);

				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
				g2.setColor(shineColor);

				int shineWidth = Math.max(4, bodyWidth / 4);
				g2.fillRoundRect(bodyX + Math.max(1, bodyWidth / 8), bodyTop, shineWidth, bodyHeight, 10, 10);
			} finally {
				g2.dispose();
			}
		}

		drawNoteHead(g, laneX, laneWidth, tailY);
		drawNoteHead(g, laneX, laneWidth, headY);
	}

	private int getNoteTopY(double targetTime, double gameTime, int noteHeight) {
		return (int) Math.round(
				JUDGEMENT_LINE_Y
						- noteHeight / 2.0
						- (targetTime - gameTime) * NOTE_SCROLL_SPEED
		);
	}

	private void drawNoteHead(Graphics2D g, int laneX, int laneWidth, int y) {
		int drawWidth = getNoteDrawWidth(laneWidth);
		int drawHeight = getNoteDrawHeight();
		int drawX = laneX + (laneWidth - drawWidth) / 2;

		if (noteImage != null && noteImage.getWidth(null) > 0 && noteImage.getHeight(null) > 0) {
			g.drawImage(noteImage, drawX, y, drawWidth, drawHeight, null);
			return;
		}

		g.setColor(noteThemeColor);
		g.fillRoundRect(drawX, y, drawWidth, drawHeight, 10, 10);
	}

	private int getNoteDrawWidth(int laneWidth) {
		if (noteImage != null && noteImage.getWidth(null) > 0) {
			return Math.min(noteImage.getWidth(null), Math.max(20, laneWidth - 8));
		}

		return Math.max(20, laneWidth - 20);
	}

	private int getNoteDrawHeight() {
		if (noteImage != null && noteImage.getHeight(null) > 0) {
			return noteImage.getHeight(null);
		}

		return 16;
	}

	private void drawJudgementLine(Graphics2D g, LaneLayout layout) {
		if (judgementLine != null) {
			int lineHeight = judgementLine.getHeight(null) > 0 ? judgementLine.getHeight(null) : 8;
			g.drawImage(judgementLine, layout.getStartX(), JUDGEMENT_LINE_Y, layout.getTotalWidth(), lineHeight, null);
		}
	}

	private void advanceTimelineTime(double deltaTime) {
		if (audioStarted) {
			updateTimelineTime();
			return;
		}

		leadInRemainingSeconds -= deltaTime;

		if (leadInRemainingSeconds <= 0.0) {
			context.bgm.playLoaded(false);
			audioStarted = true;
			timelineTime = context.bgm.getPositionSeconds();
			return;
		}

		timelineTime = -leadInRemainingSeconds;
	}

	private void updateTimelineTime() {
		if (audioStarted) {
			timelineTime = context.bgm.getPositionSeconds();
			return;
		}

		timelineTime = -leadInRemainingSeconds;
	}

	private double getGameplayTime() {
		return timelineTime + context.getGlobalOffset();
	}

	private void pauseGame() {
		if (paused || resultRequested) {
			return;
		}

		updateTimelineTime();

		paused = true;
		clearLanePressedStates();

		if (audioStarted && context.bgm.isPlaying()) {
			context.bgm.pause();
		}
	}

	private void resumeGame() {
		if (!paused) {
			return;
		}

		paused = false;
		clearLanePressedStates();

		if (audioStarted && context.bgm.isPaused()) {
			context.bgm.resume();
		}

		updateTimelineTime();
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
		g.drawString(String.valueOf(combo), 740, 280);

		g.drawString("MAX COMBO : ", 570, 310);
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

	private void drawAutoStatus(Graphics2D g) {
		if (!context.isAutoMode()) {
			return;
		}

		String text = "AUTO ON";

		g.setFont(new Font("SansSerif", Font.BOLD, 18));
		g.setColor(new Color(255, 120, 255));

		int x = 20;
		int y = 32;

		g.drawString(text, x, y);
	}

	private void drawScorePanelBackground(Graphics2D g) {
		Graphics2D g2 = (Graphics2D) g.create();

		try {
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
			g2.setColor(Color.BLACK);
			g2.fillRoundRect(545, 205, 285, 195, 20, 20);

			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
			g2.setColor(Color.WHITE);
			g2.drawRoundRect(545, 205, 285, 195, 20, 20);
		} finally {
			g2.dispose();
		}
	}

	private float getAlpha() {
		alpha = Math.max(alpha - 0.02f, 0f);
		return alpha;
	}

	private void drawLane(Graphics2D g, LaneLayout layout) {
		java.awt.Composite original = g.getComposite();

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

		for (Lane lane : layout.getLanes()) {
			int x = layout.getLaneX(lane);
			int laneWidth = layout.getLaneWidth(lane);

			g.setColor(new Color(13, 13, 13));
			g.fillRect(x, 0, laneWidth, layout.getLaneHeight());

			g.setColor(new Color(21, 21, 21));
			g.fillRect(x + 5, 0, Math.max(0, laneWidth - 10), layout.getLaneHeight());

			if (isLanePressed(lane)) {
				drawLaneGlow(g, x, laneWidth, layout.getLaneHeight());
			}

			g.setColor(new Color(42, 42, 42));
			g.fillRect(x, 0, 2, layout.getLaneHeight());
			g.fillRect(x + laneWidth - 2, 0, 2, layout.getLaneHeight());
		}

		drawCenterDivider(g, layout);

		g.setComposite(original);
	}

	private void drawCenterDivider(Graphics2D g, LaneLayout layout) {
		int laneCount = layout.getLaneCount();

		if (laneCount < 2) {
			return;
		}

		int splitIndex = laneCount / 2;
		int dividerX = layout.getLaneX(splitIndex);

		Color base = noteThemeColor;
		Color outerGlow = withAlpha(base, 75);
		Color innerGlow = withAlpha(base, 165);
		Color core = brighten(base, 1.55f, 230);

		Graphics2D g2 = (Graphics2D) g.create();

		try {
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
			g2.setColor(outerGlow);
			g2.fillRect(dividerX - 7, 0, 14, layout.getLaneHeight());

			g2.setColor(innerGlow);
			g2.fillRect(dividerX - 4, 0, 8, layout.getLaneHeight());

			g2.setColor(core);
			g2.fillRect(dividerX - 1, 0, 2, layout.getLaneHeight());
		} finally {
			g2.dispose();
		}
	}

	private void drawLaneGlow(Graphics2D g, int x, int laneWidth, int laneHeight) {
		Graphics2D g2 = (Graphics2D) g.create();

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f));
		g2.setColor(withAlpha(noteThemeColor, 120));
		g2.fillRect(x, 0, laneWidth, laneHeight);

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
		g2.setPaint(new GradientPaint(
				x + laneWidth / 2f,
				0f,
				brighten(noteThemeColor, 1.75f, 220),
				x + laneWidth / 2f,
				laneHeight,
				withAlpha(noteThemeColor, 180)
		));
		g2.fillRect(x + 8, 0, Math.max(0, laneWidth - 16), laneHeight);

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
		g2.setColor(brighten(noteThemeColor, 1.9f, 160));
		g2.fillRect(x + laneWidth / 2 - 10, 0, 20, laneHeight);

		g2.dispose();
	}

	private void drawLaneKeyBindings(Graphics2D g, LaneLayout layout) {
		Graphics2D g2 = (Graphics2D) g.create();

		try {
			int textY = 405;
			int textHeight = 42;
			int fontSize = Math.max(16, Math.min(28, layout.getMinLaneWidth() - 18));

			g2.setFont(new Font("Arial", Font.BOLD, fontSize));

			for (Lane lane : layout.getLanes()) {
				int laneX = layout.getLaneX(lane);
				int laneWidth = layout.getLaneWidth(lane);
				String keyText = context.getKeyTextForLane(lane);

				RenderUtils.drawOutlinedCenteredString(
						g2,
						keyText,
						laneX,
						textY,
						laneWidth,
						textHeight,
						Color.WHITE,
						new Color(0, 0, 0, 180)
				);
			}
		} finally {
			g2.dispose();
		}
	}

	private Color extractDominantNoteColor(Image image) {
		if (image == null) {
			return new Color(120, 220, 255);
		}

		int width = image.getWidth(null);
		int height = image.getHeight(null);

		if (width <= 0 || height <= 0) {
			return new Color(120, 220, 255);
		}

		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bufferedImage.createGraphics();

		try {
			g2.drawImage(image, 0, 0, null);
		} finally {
			g2.dispose();
		}

		long rSum = 0;
		long gSum = 0;
		long bSum = 0;
		int count = 0;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int argb = bufferedImage.getRGB(x, y);
				int alpha = (argb >>> 24) & 0xFF;

				if (alpha < 80) {
					continue;
				}

				int r = (argb >>> 16) & 0xFF;
				int g = (argb >>> 8) & 0xFF;
				int b = argb & 0xFF;

				int max = Math.max(r, Math.max(g, b));
				int min = Math.min(r, Math.min(g, b));

				if (max > 235 && min > 220) {
					continue;
				}

				if (max < 35) {
					continue;
				}

				rSum += r;
				gSum += g;
				bSum += b;
				count++;
			}
		}

		if (count == 0) {
			return new Color(120, 220, 255);
		}

		return new Color(
				clampColor((int) (rSum / count)),
				clampColor((int) (gSum / count)),
				clampColor((int) (bSum / count))
		);
	}

	private Color withAlpha(Color color, int alpha) {
		return new Color(
				color.getRed(),
				color.getGreen(),
				color.getBlue(),
				clampColor(alpha)
		);
	}

	private Color brighten(Color color, float factor, int alpha) {
		return new Color(
				clampColor((int) (color.getRed() * factor)),
				clampColor((int) (color.getGreen() * factor)),
				clampColor((int) (color.getBlue() * factor)),
				clampColor(alpha)
		);
	}

	private int clampColor(int value) {
		return Math.max(0, Math.min(255, value));
	}

	private void initializeLanePressedMap() {
		lanePressed.clear();

		for (Lane lane : context.getPlayableLanes()) {
			lanePressed.put(lane, false);
		}
	}

	private boolean isLanePressed(Lane lane) {
		return lanePressed.getOrDefault(lane, false);
	}

	private void setLanePressed(Lane lane, boolean pressed) {
		lanePressed.put(lane, pressed);
	}

	private void clearLanePressedStates() {
		lanePressed.clear();

		for (Lane lane : context.getPlayableLanes()) {
			lanePressed.put(lane, false);
		}
	}

	private void resetJudgementCounts() {
		judgementCounts.clear();

		for (Judgement judgement : Judgement.values()) {
			if (judgement != Judgement.NONE) {
				judgementCounts.put(judgement, 0);
			}
		}
	}

	private GameResult buildGameResult() {
		return new GameResult(score, maxCombo, accuracy, judgementCounts);
	}
}