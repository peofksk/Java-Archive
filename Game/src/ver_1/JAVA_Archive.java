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

	private int mouseX, mouseY, state = 0, music_state = 0;
	final int MUSIC_NUM = 3;
	String[] music_arr = { "unwelcomeSchool", "test", "test" };
	String[] music_title = { "Title_unwelcomeSchool", "Title_comingSoon", "Title_comingSoon" };
	String[] game_background = { "unwelcomeSchoolBackground", "test", "test" };
	private boolean sample_play = false;

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

	private Music introMusic = new Music("introMusic.mp3", true);
	// private Music GameMusic = new Music(music_arr[music_state]+".mp3", false);
	private Music sampleMusic = new Music("sample_" + music_arr[music_state] + ".mp3", true);

	public static Game game;

	public JAVA_Archive() {
		setUndecorated(true);
		setTitle("Bacon");
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

		arrowLeft.setVisible(false);
		arrowRight.setVisible(false);
		pressEnter_2.setVisible(false);;

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
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					if (state == 0) {
						musicSelection();
					} else if (state == 1) {
						gameStart(music_state);
					}
				}
				if (state == 1) {
					if (e.getKeyCode() == 37) {
						music_state = (music_state + MUSIC_NUM - 1) % MUSIC_NUM;
					}
					if (e.getKeyCode() == 39 && state == 1) { // 오른쪽 방향키 입력
						music_state = (music_state + MUSIC_NUM + 1) % MUSIC_NUM;
					}
					selectedImage = new ImageIcon(
							Main.class.getResource("../Asset/" + music_title[music_state] + ".png")).getImage();
					if (sample_play) {
						sampleMusic.close();
						sample_play = false;
					}
					sampleMusic = new Music("sample_" + music_arr[music_state] + ".mp3", true);
					sampleMusic.start();
					sample_play = true;

				} else if (state == 2) { // 게임중
					switch (e.getKeyChar()) {
					case 'd':
						game.pressD();
						break;
					case 'f':
						game.pressF();
						break;
					case 'j':
						game.pressJ();
						break;
					case 'k':
						game.pressK();
						break;
					}
				} else if (state == 3) { // 결과창

				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});

		setFocusable(true);
		requestFocusInWindow();

		introMusic.start();
	}
	
	public void setState(int state) {
		this.state = state;
	}
	public int getState() {
		return state;
	}

	public void gameStart(int music_state) {
		state = 2;
		arrowLeft.setVisible(false);
		arrowRight.setVisible(false);
		pressEnter_2.setVisible(false);
		Background = new ImageIcon(Main.class.getResource("../Asset/" + game_background[music_state] + ".png"))
				.getImage();
		sampleMusic.close();
		sample_play = false;

		game = new Game(new Music(music_arr[music_state] + ".mp3", false), music_arr[music_state], this);

		game.start();
	}

	public void musicSelection() {
		state = 1;
		gameTitle.setVisible(false);
		pressEnter_1.setVisible(false);
		pressEnter_2.setVisible(true);
		Background = new ImageIcon(Main.class.getResource("../Asset/selectionBackground.png")).getImage();
		arrowLeft.setVisible(true);
		arrowRight.setVisible(true);
		introMusic.close();
	}

	public void paint(Graphics g) {
		screenImage = createImage(Main.SCREEN_WIDTH, Main.SCREEN_HEIGHT);
		screenGraphic = screenImage.getGraphics();
		screenDraw(screenGraphic);
		g.drawImage(screenImage, 0, 0, null);
		
		if (game != null && state == 3) {
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
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.setColor(Color.BLACK);
            g.drawString("Score: " + game.score, 370, 430);
        }
	}

	public void screenDraw(Graphics g) {
		g.drawImage(Background, 0, 0, null);
		if (state == 1) {
			g.drawImage(selectedImage, 312, 73, null);
		} else if (state == 2) {
			game.screenDraw((Graphics2D) g);
		} else if (state == 3) {
			Background = new ImageIcon(Main.class.getResource("../Asset/resultBackground.png"))
					.getImage();
			g.drawImage(Background, 0, 0, null);
		}
		paintComponents(g);
		this.repaint();
	}
}
