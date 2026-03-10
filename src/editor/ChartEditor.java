package editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;

public class ChartEditor extends JFrame {

	private final LanePanel lanePanel;

	public ChartEditor() {
		setTitle("Rhythm Chart Editor");
		setSize(700, 760);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());

		lanePanel = new LanePanel();

		add(createTopPanel(), BorderLayout.NORTH);
		add(lanePanel, BorderLayout.CENTER);

		setLocationRelativeTo(null);
	}

	private JPanel createTopPanel() {
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		JButton loadButton = new JButton("Load");
		JButton saveButton = new JButton("Save");
		JButton clearButton = new JButton("Clear");

		JLabel gridLabel = new JLabel("Grid:");
		String[] gridOptions = { "1.0", "0.5", "0.25", "0.125" };
		JComboBox<String> gridCombo = new JComboBox<>(gridOptions);
		gridCombo.setSelectedItem("0.25");

		JLabel speedLabel = new JLabel("Pixels/Sec:");
		JTextField speedField = new JTextField("120", 5);

		panel.add(loadButton);
		panel.add(saveButton);
		panel.add(clearButton);
		panel.add(Box.createHorizontalStrut(20));
		panel.add(gridLabel);
		panel.add(gridCombo);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(speedLabel);
		panel.add(speedField);

		loadButton.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser();
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				lanePanel.loadChart(chooser.getSelectedFile());
			}
		});

		saveButton.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser();
			if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				lanePanel.saveChart(chooser.getSelectedFile());
			}
		});

		clearButton.addActionListener(e -> lanePanel.clearNotes());

		gridCombo.addActionListener(e -> {
			try {
				double grid = Double.parseDouble((String) gridCombo.getSelectedItem());
				lanePanel.setGridInterval(grid);
			} catch (Exception ignored) {
			}
		});

		speedField.addActionListener(e -> {
			try {
				double speed = Double.parseDouble(speedField.getText().trim());
				if (speed > 0) {
					lanePanel.setPixelsPerSecond(speed);
				}
			} catch (Exception ignored) {
			}
		});

		return panel;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new ChartEditor().setVisible(true));
	}
}

class LanePanel extends JPanel {

	private final ArrayList<Note> notes = new ArrayList<>();

	private final int lanes = 4;
	private final int laneWidth = 100;
	private final int noteWidth = 60;
	private final int noteHeight = 12;
	private final int baseX = 100;

	private double pixelsPerSecond = 120.0;
	private double gridInterval = 0.25;

	private double scrollOffsetSeconds = 0.0;
	private final double wheelScrollSeconds = 0.5;

	public LanePanel() {
		setBackground(Color.BLACK);
		setFocusable(true);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					toggleNoteByMouse(e);
				}
			}
		});

		addMouseWheelListener(e -> {
			scrollOffsetSeconds += e.getPreciseWheelRotation() * wheelScrollSeconds;

			if (scrollOffsetSeconds < 0) {
				scrollOffsetSeconds = 0;
			}

			repaint();
		});
	}

	private void toggleNoteByMouse(MouseEvent e) {
		int relativeX = e.getX() - baseX;

		if (relativeX < 0 || relativeX >= lanes * laneWidth) {
			return;
		}

		int lane = relativeX / laneWidth;

		double rawTime = screenYToTime(e.getY());
		double snappedTime = Math.round(rawTime / gridInterval) * gridInterval;

		if (snappedTime < 0) {
			snappedTime = 0;
		}

		Note existing = findNote(lane, snappedTime);

		if (existing != null) {
			notes.remove(existing);
		} else {
			notes.add(new Note(lane, snappedTime));
			sortNotes();
		}

		repaint();
	}

	private Note findNote(int lane, double time) {
		final double epsilon = 0.0001;

		for (Note note : notes) {
			if (note.lane == lane && Math.abs(note.time - time) < epsilon) {
				return note;
			}
		}

		return null;
	}

	private void sortNotes() {
		notes.sort(Comparator.comparingDouble((Note n) -> n.time).thenComparingInt(n -> n.lane));
	}

	private double screenYToTime(int y) {
		return scrollOffsetSeconds + (y / pixelsPerSecond);
	}

	private int timeToScreenY(double time) {
		return (int) Math.round((time - scrollOffsetSeconds) * pixelsPerSecond);
	}

	public void setGridInterval(double gridInterval) {
		this.gridInterval = gridInterval;
		repaint();
	}

	public void setPixelsPerSecond(double pixelsPerSecond) {
		this.pixelsPerSecond = pixelsPerSecond;
		repaint();
	}

	public void clearNotes() {
		notes.clear();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g.create();
		try {
			drawLanes(g2);
			drawGrid(g2);
			drawLaneBorders(g2);
			drawNotes(g2);
			drawInfo(g2);
		} finally {
			g2.dispose();
		}
	}

	private void drawLanes(Graphics2D g) {
		for (int i = 0; i < lanes; i++) {
			int x = baseX + i * laneWidth;

			g.setColor(new Color(30, 30, 30));
			g.fillRect(x, 0, laneWidth, getHeight());

			g.setColor(new Color(40, 40, 40));
			g.fillRect(x + 4, 0, laneWidth - 8, getHeight());
		}
	}

	private void drawGrid(Graphics2D g) {
		int panelHeight = getHeight();
		int x1 = baseX;
		int x2 = baseX + lanes * laneWidth;

		double startTime = scrollOffsetSeconds;
		double endTime = scrollOffsetSeconds + (panelHeight / pixelsPerSecond);

		double firstGrid = Math.floor(startTime / gridInterval) * gridInterval;

		g.setFont(new Font("Arial", Font.PLAIN, 12));

		for (double t = firstGrid; t <= endTime + gridInterval; t += gridInterval) {
			int y = timeToScreenY(t);

			boolean major = Math.abs(t - Math.round(t)) < 0.0001;

			if (major) {
				g.setColor(new Color(120, 120, 120));
			} else {
				g.setColor(new Color(75, 75, 75));
			}

			g.drawLine(x1, y, x2, y);

			// 왼쪽 시간 숫자 표시 (1초 단위 major grid만)
			if (major) {
				g.setColor(Color.WHITE);
				String label = String.format("%.0f", t);
				FontMetrics fm = g.getFontMetrics();
				int textX = baseX - 10 - fm.stringWidth(label);
				int textY = y + (fm.getAscent() / 2) - 2;
				g.drawString(label, textX, textY);
			}
		}
	}

	private void drawLaneBorders(Graphics2D g) {
		g.setColor(Color.GRAY);

		for (int i = 0; i <= lanes; i++) {
			int x = baseX + i * laneWidth;
			g.drawLine(x, 0, x, getHeight());
		}
	}

	private void drawNotes(Graphics2D g) {

		g.setColor(Color.CYAN);

		for (Note note : notes) {

			int y = timeToScreenY(note.time) - noteHeight / 2;

			if (y + noteHeight < 0 || y > getHeight()) {
				continue;
			}

			int x = baseX + note.lane * laneWidth + (laneWidth - noteWidth) / 2;

			g.fillRect(x, y, noteWidth, noteHeight);
		}
	}

	private void drawInfo(Graphics2D g) {
		g.setColor(Color.WHITE);
		g.setFont(new Font("Arial", Font.PLAIN, 14));

		g.drawString(String.format("Scroll: %.3f s", scrollOffsetSeconds), 10, 20);
		g.drawString(String.format("Grid: %.3f", gridInterval), 10, 40);
	}

	public void saveChart(File file) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			sortNotes();

			for (Note note : notes) {
				String laneText = switch (note.lane) {
				case 0 -> "D";
				case 1 -> "F";
				case 2 -> "J";
				case 3 -> "K";
				default -> throw new IllegalStateException("Unexpected lane: " + note.lane);
				};

				bw.write(String.format("%.3f %s%n", note.time, laneText));
			}

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "저장 실패: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public void loadChart(File file) {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			notes.clear();

			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();

				if (line.isEmpty()) {
					continue;
				}

				String[] parts = line.split("\\s+");
				if (parts.length < 2) {
					continue;
				}

				double time;
				try {
					time = Double.parseDouble(parts[0]);
				} catch (NumberFormatException e) {
					continue;
				}

				int lane = switch (parts[1]) {
				case "D" -> 0;
				case "F" -> 1;
				case "J" -> 2;
				case "K" -> 3;
				default -> -1;
				};

				if (lane == -1) {
					continue;
				}

				notes.add(new Note(lane, time));
			}

			sortNotes();
			repaint();

		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "불러오기 실패: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}

class Note {
	int lane;
	double time;

	public Note(int lane, double time) {
		this.lane = lane;
		this.time = time;
	}
}