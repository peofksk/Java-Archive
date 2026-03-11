package state;

import java.awt.Graphics2D;
import java.awt.Image;

import asset.AssetManager;
import core.GameContext;
import core.GameState;
import stage.CorrectionConfig;
import state.gameplay.NoteManager;

public class CorrectionState implements GameState {
	
	private final GameContext context;
	private final CorrectionConfig correctionConfig;
	private final AssetManager am = AssetManager.getInstance();
	private NoteManager nm;
	private Image background, judgementLine, noteImage;
	
	private boolean musicThreadStarted = false;
	private boolean preloaded = false;
	private static final double LEAD_IN = 3.0;
    private static final double GLOBAL_OFFSET = 0.0;
    private static final double AUDIO_OUTPUT_LATENCY = 0.3;
    private double elapsedTime = -LEAD_IN;
    private long songStartNano = 0L;
    
    private int totalNoteCount = 1;
	
	
	public CorrectionState(GameContext context, CorrectionConfig correctionConfig) {
		this.context = context;
		this.correctionConfig = correctionConfig;
	}

	@Override
	public void enter() {
		context.bgm.stop();
		startScheduledMusicThread();
	}

	@Override
	public void update(double deltaTime) {
		
	}

	@Override
	public void render(Graphics2D g) {
		
	}

	@Override
	public void exit() {
		
	}
	
	public void preload() {
        if (preloaded) {
            return;
        }

        background = am.getImage(correctionConfig.getBackgroundImageKey());
        judgementLine = am.getImage("judgement_line");
        noteImage = am.getImage("note_image");

        nm = new NoteManager();
        nm.loadChart("note_" + correctionConfig.getLevelName());

        totalNoteCount = nm.getRemainingNoteCount();
        if (totalNoteCount <= 0) {
            totalNoteCount = 1;
        }

        context.bgm.load(correctionConfig.getMusicPath());

        preloaded = true;
    }
	
	private void startScheduledMusicThread() {
        if (musicThreadStarted) {
            return;
        }

        musicThreadStarted = true;

        Thread t = new Thread(() -> {
            try {
                long playRequestNano = songStartNano - (long) (AUDIO_OUTPUT_LATENCY * 1_000_000_000L);

                while (true) {
                    long now = System.nanoTime();
                    long remain = playRequestNano - now;

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

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        t.setDaemon(true);
        t.start();
    }
}
