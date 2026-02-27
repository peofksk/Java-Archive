package ver_1;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class JAVA_Archive extends JFrame {

	private int mouseX, mouseY;
	public int music_state = 0, difficulty_state = 0;
	public static int note_row, note_column, note_state = 0;
	public static String state = "intro";
	final int ROW_NUM = 4, COLUMN_NUM = 3, NOTE_NUM = 12;
	String[] music_arr = { "unwelcomeSchool", "afterSchoolDessert", "test" };
	String[] music_title = { "Title_unwelcomeSchool", "Title_afterSchoolDessert", "Title_comingSoon" };
	String[] game_background = { "unwelcomeSchoolBackground", "afterSchoolDessertBackground", "test" };
	String[] difficulty = { "Easy", "Hard", "Extreme" };
	int[] bpm = { 180, 160, 0 };
	private boolean sample_play, result_play = false;

	private Image screenImage;
	private Graphics screenGraphic;

	private ImageIcon exitButtonImage = new ImageIcon(Main.class.getResource("../Asset/exitButton.png"));
	private ImageIcon exitButtonPressedImage = new ImageIcon(Main.class.getResource("../Asset/exitButtonPressed.png"));

	private JButton exitButton = new JButton(exitButtonImage);

	private Image Background = new ImageIcon(Main.class.getResource("../Asset/introBackground.jpg")).getImage();

	private Image selectedImage = new ImageIcon(Main.class.getResource("../Asset/" + music_title[music_state] + ".png"))
			.getImage();

	private JLabel menuBar = new JLabel(new ImageIcon(Main.class.getResource("../Asset/menuBar.png")));
	private JLabel gameTitle = new JLabel(new ImageIcon(Main.class.getResource("../Asset/gameTitle.png")));
	private JLabel pressEnter_1 = new JLabel(new ImageIcon(Main.class.getResource("../Asset/pressEnter.png")));
	private JLabel pressEnter_2 = new JLabel(new ImageIcon(Main.class.getResource("../Asset/pressEnter.png")));
	private JLabel arrowLeft = new JLabel(new ImageIcon(Main.class.getResource("../Asset/arrowLeft.png")));
	private JLabel arrowRight = new JLabel(new ImageIcon(Main.class.getResource("../Asset/arrowRight.png")));
	private JLabel corner = new JLabel(new ImageIcon(Main.class.getResource("../Asset/corner.png")));

	private Music introMusic = new Music("introMusic.mp3", true);
	private Music sampleMusic = new Music("sample_" + music_arr[music_state] + ".mp3", true);
	private Music correctionMusic = new Music("correctionMusic.mp3", true);
	private Music optionMusic = new Music("optionMusic.mp3", true);
	private Music resultMusic = new Music("resultMusic.mp3", true);

	public Game game;
	public Correction correction;
	public CorrectionNote correctionNote;

	public JAVA_Archive() {
		setUndecorated(true);
		setTitle("Java_Archive");
		setSize(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT);
		setResizable(false);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		setBackground(new Color(0, 0, 0, 0));
		setLayout(null);

		gameTitle.setBounds(171, 50, 681, 250);
		add(gameTitle);
		pressEnter_1.setBounds(357, 470, 310, 26);
		add(pressEnter_1);
		pressEnter_2.setBounds(357, 500, 310, 26);
		add(pressEnter_2);

		arrowLeft.setBounds(67, 225, 235, 125);
		add(arrowLeft);
		arrowRight.setBounds(722, 225, 235, 125);
		add(arrowRight);

		corner.setBounds(410, 114, 140, 64);
		add(corner);

		arrowLeft.setVisible(false);
		arrowRight.setVisible(false);
		pressEnter_2.setVisible(false);
		corner.setVisible(false);

		exitButton.setBounds(994, 0, 30, 30);
		exitButton.setBorderPainted(false);
		exitButton.setContentAreaFilled(false);
		exitButton.setFocusPainted(false);
		exitButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent e) {
				exitButton.setIcon(exitButtonPressedImage);
				exitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				exitButton.setIcon(exitButtonImage);
				exitButton.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}

			@Override
			public void mousePressed(MouseEvent e) {
				System.exit(0);
			}
		});
		add(exitButton);

		menuBar.setBounds(0, 0, 1024, 30);
		menuBar.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();
			}
		});
		menuBar.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				int x = e.getXOnScreen();
				int y = e.getYOnScreen();
				setLocation(x - mouseX, y - mouseY);
			}
		});
		add(menuBar);

		addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				char keyChar = e.getKeyChar();

				switch (state) {
				case "intro":
					handleIntroState(keyCode);
					break;
				case "musicSelection":
					handleMusicSelectionState(keyCode);
					break;
				case "option":
					handleOptionState(keyCode);
					break;
				case "correction":
					handleCorrectionState(keyCode);
					break;
				case "difficultySelection":
					handleDifficultySelectionState(keyCode);
					break;
				case "game":
					handleGameState(keyChar, "Pressed");
					break;
				case "result":
					handleResultState(keyCode);
					break;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				int keyCode = e.getKeyCode();
				char keyChar = e.getKeyChar();

				switch (state) {
				case "game":
					handleGameState(keyChar, "Released");
					break;
				}
			}
		});

		setFocusable(true);
		requestFocusInWindow();

		introMusic.start();
	}

	private void handleIntroState(int keyCode) {
		if (keyCode == KeyEvent.VK_ENTER) {
			musicSelection();
		}
	}

	private void handleMusicSelectionState(int keyCode) {
		switch (keyCode) {
		case KeyEvent.VK_O:
			option();
			break;
		case KeyEvent.VK_C:
			// correction();
			break;
		case KeyEvent.VK_ENTER:
			difficultySelection();
			break;
		case KeyEvent.VK_ESCAPE:
			gameTitle();
			break;
		case KeyEvent.VK_LEFT:
			music_state = Math.max(music_state - 1, 0);
			updateSelectedMusic();
			break;
		case KeyEvent.VK_RIGHT:
			music_state = Math.min(music_state + 1, music_arr.length - 1);
			updateSelectedMusic();
			break;
		}
	}

	private void handleOptionState(int keyCode) {
		switch (keyCode) {
		case KeyEvent.VK_ENTER:
		case KeyEvent.VK_ESCAPE:
			musicSelection();
			break;
		case KeyEvent.VK_LEFT:
			note_column = (note_column - 1 + COLUMN_NUM) % COLUMN_NUM;
			break;
		case KeyEvent.VK_RIGHT:
			note_column = (note_column + 1 + COLUMN_NUM) % COLUMN_NUM;
			break;
		case KeyEvent.VK_UP:
			note_row = (note_row - 1 + ROW_NUM) % ROW_NUM;
			break;
		case KeyEvent.VK_DOWN:
			note_row = (note_row + 1 + ROW_NUM) % ROW_NUM;
			break;
		}
		corner.setBounds(410 + 185 * note_column, 114 + 90 * note_row, 140, 64);
		note_state = 3 * note_row + note_column;
	}

	private void handleCorrectionState(int keyCode) {
		switch (keyCode) {
		case KeyEvent.VK_SPACE:
			correction.pressSpace();
			break;
		}
	}

	private void handleDifficultySelectionState(int keyCode) {
		switch (keyCode) {
		case KeyEvent.VK_ENTER:
			gameStart(music_state, difficulty[difficulty_state], bpm[music_state]);
			break;
		case KeyEvent.VK_ESCAPE:
			musicSelection();
			break;
		case KeyEvent.VK_UP:
			difficulty_state = Math.min(difficulty_state + 1, difficulty.length - 1);
			break;
		case KeyEvent.VK_DOWN:
			difficulty_state = Math.max(difficulty_state - 1, 0);
			break;
		}
	}

	private void handleGameState(char keyChar, String keyState) {
		if (keyState.equals("Pressed")) {
			switch (keyChar) {
			case 'd':
			case 'D':
				game.pressD();
				break;
			case 'f':
			case 'F':
				game.pressF();
				break;
			case 'j':
			case 'J':
				game.pressJ();
				break;
			case 'k':
			case 'K':
				game.pressK();
				break;
			}
		} else if (keyState.equals("Released")) {
			switch (keyChar) {
			case 'd':
			case 'D':
				game.releaseD();
				break;
			case 'f':
			case 'F':
				game.releaseF();
				break;
			case 'j':
			case 'J':
				game.releaseJ();
				break;
			case 'k':
			case 'K':
				game.releaseK();
				break;
			}
		}

	}

	private void handleResultState(int keyCode) {
		if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_ENTER) {
			musicSelection();
		}
	}

	private void updateSelectedMusic() {
		selectedImage = new ImageIcon(Main.class.getResource("../Asset/" + music_title[music_state] + ".png"))
				.getImage();
		if (sample_play) {
			sampleMusic.close();
			sample_play = false;
		}
		sampleMusic = new Music("sample_" + music_arr[music_state] + ".mp3", true);
		sampleMusic.start();
		sample_play = true;
	}

	public void gameTitle() {
		state = "intro";
		gameTitle.setVisible(true);
		pressEnter_1.setVisible(true);
		pressEnter_2.setVisible(false);
		;
		Background = new ImageIcon(Main.class.getResource("../Asset/introBackground.jpg")).getImage();
		arrowLeft.setVisible(false);
		arrowRight.setVisible(false);
		sampleMusic.close();
		introMusic = new Music("introMusic.mp3", true);
		introMusic.start();
	}

	public void musicSelection() {
		state = "musicSelection";
		music_state = 0;
		gameTitle.setVisible(false);
		pressEnter_1.setVisible(false);
		pressEnter_2.setVisible(true);
		Background = new ImageIcon(Main.class.getResource("../Asset/selectionBackground.png")).getImage();
		arrowLeft.setVisible(true);
		arrowRight.setVisible(true);
		corner.setVisible(false);
		introMusic.close();
		resultMusic.close();
		optionMusic.close();
		correctionMusic.close();
		updateSelectedMusic();
	}

	public void option() {
		state = "option";
		arrowLeft.setVisible(false);
		arrowRight.setVisible(false);
		pressEnter_2.setVisible(false);
		corner.setVisible(true);
		Background = new ImageIcon(Main.class.getResource("../Asset/optionBackground.jpg")).getImage();
		sampleMusic.close();
		sample_play = false;
		optionMusic = new Music("optionMusic.mp3", true);
		optionMusic.start();
	}

	public void correction() {
		state = "correction";
		arrowLeft.setVisible(false);
		arrowRight.setVisible(false);
		pressEnter_2.setVisible(false);
		Background = new ImageIcon(Main.class.getResource("../Asset/correctionBackground.png")).getImage();
		sampleMusic.close();
		sample_play = false;
		correction = new Correction();
		correction.start();
	}

	public void difficultySelection() {
		state = "difficultySelection";
		pressEnter_2.setVisible(false);
	}

	public void gameStart(int music_state, String difficulty, int bpm) {
		state = "game";
		arrowLeft.setVisible(false);
		arrowRight.setVisible(false);
		pressEnter_2.setVisible(false);
		Background = new ImageIcon(Main.class.getResource("../Asset/" + game_background[music_state] + ".png"))
				.getImage();
		sampleMusic.close();
		sample_play = false;
		game = new Game(new Music(music_arr[music_state] + ".mp3", false), music_arr[music_state], difficulty, bpm,
				this);

		game.start();
	}

	public void gameResult() {
		state = "result";
		if (result_play) {
			resultMusic.close();
			result_play = false;
		}
		resultMusic = new Music("resultMusic.mp3", true);
		resultMusic.start();
		result_play = true;
	}

	public void paint(Graphics g) {
		screenImage = createImage(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT);
		screenGraphic = screenImage.getGraphics();
		screenDraw(screenGraphic);
		g.drawImage(screenImage, 0, 0, null);
		if (state.equals("musicSelection")) {
			if (music_state == music_arr.length - 1) {
				arrowRight.setVisible(false);
			} else if (music_state == 0) {
				arrowLeft.setVisible(false);

			} else {
				arrowRight.setVisible(true);
				arrowLeft.setVisible(true);
			}
		}
		if (state.equals("difficultySelection")) {
			g.setFont(new Font("SansSerif", Font.ITALIC, 35));
			g.setColor(Color.CYAN);
			g.drawString("Please set difficulty: ", 300, 530);
			g.setFont(new Font("SansSerif", Font.BOLD, 35));
			if (difficulty_state == 0)
				g.setColor(Color.GREEN);
			else if (difficulty_state == 1)
				g.setColor(Color.ORANGE);
			else if (difficulty_state == 2)
				g.setColor(Color.RED);
			g.drawString(difficulty[difficulty_state], 630, 530);
		}
		if (state.equals("result")) {

		}
	}

	public void screenDraw(Graphics g) {
		g.drawImage(Background, 0, 0, null);
		if (state.equals("musicSelection")) {
			g.drawImage(selectedImage, 312, 73, null);
		} else if (state.equals("option")) {
			for (int i = 0; i < ROW_NUM; i++) {
				for (int j = 0; j < COLUMN_NUM; j++) {
					g.drawImage(new ImageIcon(Main.class.getResource("../Asset/note_" + (3 * i + j) + ".png")).getImage(), 440 + 185 * j, 130 + 90 * i, null);
				}
			}
			corner.setBounds(410 + 185 * note_column, 114 + 90 * note_row, 140, 64);
		} else if (state.equals("correction")) {
			correction.screenDraw((Graphics2D) g);
		} else if (state.equals("difficultySelection")) {
			g.drawImage(selectedImage, 312, 73, null);
		} else if (state.equals("game")) {
			game.screenDraw((Graphics2D) g);
		} else if (state.equals("result")) {
			Background = new ImageIcon(Main.class.getResource("../Asset/resultBackground.png")).getImage();
			g.drawImage(Background, 0, 0, null);
			g.setFont(new Font("Arial", Font.BOLD, 30));
			g.setColor(Color.BLUE);
			g.drawString("Perfect: " + game.perfectCount, 170, 250);
			g.setColor(Color.CYAN);
			g.drawString("Great: " + game.greatCount, 170, 290);
			g.setColor(Color.GREEN);
			g.drawString("Good: " + game.goodCount, 170, 330);
			g.setColor(Color.ORANGE);
			g.drawString("Early: " + game.earlyCount, 170, 370);
			g.drawString("Late: " + game.lateCount, 170, 410);
			g.setColor(Color.GRAY);
			g.drawString("Miss: " + game.missCount, 170, 450);
			g.setFont(new Font("Arial", Font.BOLD, 40));
			g.setColor(Color.RED);
			g.drawString("Max Combo: " + game.maxCombo, 370, 300);
			g.setFont(new Font("Arial", Font.BOLD, 30));
			g.setColor(Color.GRAY);
			g.drawString("Accuracy: " + String.format("%.2f", game.accuracy), 370, 350);
			g.setFont(new Font("Arial", Font.BOLD, 50));
			g.setColor(Color.BLACK);
			g.drawString("Score: " + game.score, 370, 430);
		}
		paintComponents(g);
		this.repaint();
	}
}
