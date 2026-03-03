package app;

import javax.swing.JFrame;
import javax.swing.Timer;

import core.GameContext;
import core.GamePanel;
import core.GameState;
import state.IntroState;

public class JAVA_Archive extends JFrame {

	private static final long serialVersionUID = 1L;

	private GameState currentState;
	private GamePanel panel;
	private Timer gameTimer;
	private long lastTime;

	private final GameContext context = new GameContext();

	public JAVA_Archive() {
		setTitle("JAVA_Archive");
		setSize(1024, 576);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		setLocationRelativeTo(null);

		panel = new GamePanel(context);
		setContentPane(panel);

		setVisible(true);
		panel.requestFocusInWindow();

		startGameLoop();
		context.changeState(new IntroState(context));
	}

	private void startGameLoop() {
		lastTime = System.nanoTime();

		gameTimer = new Timer(16, e -> {
			long now = System.nanoTime();
			double deltaTime = (now - lastTime) / 1_000_000_000.0;
			lastTime = now;

			if (currentState != null) {
				currentState.update(deltaTime);
			}

			panel.repaint();
		});

		gameTimer.start();
	}

	
}