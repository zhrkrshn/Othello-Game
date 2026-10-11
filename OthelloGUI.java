import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * A Swing GUI for Othello. Run main to play.
 *
 * Each side (Black = X = P1, White = O = P2) can be a Human, Random or Greedy
 * player, which covers every terminal controller:
 *   Human vs Human, Human vs Random, Human vs Greedy  -> pick sides and click
 *   Random vs Random, Random vs Greedy                -> watch, or press
 *                                                         "Simulate" for the
 *                                                         10,000-game statistics
 *
 * Humans click a square; legal moves are marked with a dot, and hovering shows
 * a preview of your disc. All rules come from Othello / OthelloBoard and the
 * computer moves come from PlayerRandom / PlayerGreedy.
 */
public class OthelloGUI extends JFrame {

	private static final String HUMAN = "Human";
	private static final String RANDOM = "Random";
	private static final String GREEDY = "Greedy";
	private static final String[] PLAYER_TYPES = { HUMAN, RANDOM, GREEDY };

	private static final int COMPUTER_DELAY_MS = 450; // pause so you can watch computer moves
	private static final int SIM_GAMES = 10000;

	private static final Color BACKGROUND = new Color(0x163524);
	private static final Color FELT = new Color(0x1F7A4D);
	private static final Color GRID = new Color(0x12512F);
	private static final Color LABEL = new Color(0xBFD9C8);
	private static final Color HINT = new Color(0, 0, 0, 70);
	private static final Color LAST_MOVE = new Color(0xE5533D);

	private Othello othello = new Othello();
	private Move lastMove = null;
	private String note = "";
	private boolean simulating = false;

	private final JComboBox<String> blackChoice = new JComboBox<>(PLAYER_TYPES);
	private final JComboBox<String> whiteChoice = new JComboBox<>(PLAYER_TYPES);
	private final JButton newGameButton = new JButton("New game");
	private final JButton simulateButton = new JButton("Simulate " + String.format("%,d", SIM_GAMES) + " games");
	private final JLabel blackScore = new JLabel();
	private final JLabel whiteScore = new JLabel();
	private final JLabel status = new JLabel(" ");
	private final BoardPanel boardPanel = new BoardPanel();
	private final Timer computerTimer;

	public OthelloGUI() {
		super("Othello");
		whiteChoice.setSelectedItem(GREEDY);

		computerTimer = new Timer(COMPUTER_DELAY_MS, e -> playComputerMove());
		computerTimer.setRepeats(false);

		JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
		controls.add(new JLabel("Black (X):"));
		controls.add(blackChoice);
		controls.add(new JLabel("White (O):"));
		controls.add(whiteChoice);
		controls.add(newGameButton);
		controls.add(simulateButton);

		Font scoreFont = blackScore.getFont().deriveFont(Font.BOLD, 16f);
		blackScore.setFont(scoreFont);
		whiteScore.setFont(scoreFont);
		blackScore.setIcon(new DiscIcon(OthelloBoard.P1));
		whiteScore.setIcon(new DiscIcon(OthelloBoard.P2));
		blackScore.setIconTextGap(6);
		whiteScore.setIconTextGap(6);

		JPanel scores = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
		scores.add(blackScore);
		scores.add(whiteScore);

		JPanel bottom = new JPanel(new BorderLayout());
		bottom.add(scores, BorderLayout.WEST);
		status.setHorizontalAlignment(JLabel.RIGHT);
		status.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 14));
		bottom.add(status, BorderLayout.CENTER);

		newGameButton.addActionListener(e -> newGame());
		simulateButton.addActionListener(e -> simulate());
		ActionListener playersChanged = e -> {
			note = "";
			refresh();
			scheduleNextTurn();
		};
		blackChoice.addActionListener(playersChanged);
		whiteChoice.addActionListener(playersChanged);

		setLayout(new BorderLayout());
		add(controls, BorderLayout.NORTH);
		add(boardPanel, BorderLayout.CENTER);
		add(bottom, BorderLayout.SOUTH);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setMinimumSize(new Dimension(520, 520));
		setLocationRelativeTo(null);
		newGame();
	}

	// ---------------------------------------------------------------- game flow

	private void newGame() {
		computerTimer.stop();
		othello = new Othello();
		lastMove = null;
		note = "";
		refresh();
		scheduleNextTurn();
	}

	/** Start the computer's timer if it is a computer's turn; otherwise wait for a click. */
	private void scheduleNextTurn() {
		computerTimer.stop();
		if (!simulating && !othello.isGameOver() && !isHumanTurn()) {
			computerTimer.start();
		}
	}

	private void playComputerMove() {
		if (simulating || othello.isGameOver() || isHumanTurn()) {
			return;
		}
		char turn = othello.getWhosTurn();
		applyMove(computerMove(othello, turn, typeOf(turn)));
	}

	private void applyMove(Move move) {
		if (move == null) {
			return;
		}
		char mover = othello.getWhosTurn();
		if (!othello.move(move.getRow(), move.getCol())) {
			note = "That isn't a legal move.";
			refresh();
			return;
		}
		lastMove = move;
		char next = othello.getWhosTurn();
		if (next == mover) {
			note = name(OthelloBoard.otherPlayer(mover)) + " has no legal move and passes.";
		} else {
			note = "";
		}
		refresh();
		scheduleNextTurn();
	}

	private static Move computerMove(Othello game, char player, String type) {
		if (GREEDY.equals(type)) {
			return new PlayerGreedy(game, player).getMove();
		}
		return new PlayerRandom(game, player).getMove();
	}

	private boolean isLegal(int row, int col) {
		return othello.copy().move(row, col);
	}

	private String typeOf(char player) {
		JComboBox<String> box = (player == OthelloBoard.P1) ? blackChoice : whiteChoice;
		return (String) box.getSelectedItem();
	}

	private boolean isHumanTurn() {
		char turn = othello.getWhosTurn();
		return turn != OthelloBoard.EMPTY && HUMAN.equals(typeOf(turn));
	}

	private static String name(char player) {
		return player == OthelloBoard.P1 ? "Black (X)" : "White (O)";
	}

	private void refresh() {
		blackScore.setText(String.valueOf(othello.getCount(OthelloBoard.P1)));
		whiteScore.setText(String.valueOf(othello.getCount(OthelloBoard.P2)));

		String text;
		if (othello.isGameOver()) {
			char winner = othello.getWinner();
			text = (winner == OthelloBoard.EMPTY) ? "Game over: it's a draw." : "Game over: " + name(winner) + " wins!";
		} else {
			char turn = othello.getWhosTurn();
			text = isHumanTurn() ? name(turn) + " to move. Click a marked square."
					: name(turn) + " (" + typeOf(turn) + ") is thinking…";
		}
		if (!note.isEmpty()) {
			text = note + "  " + text;
		}
		status.setText(text);
		boardPanel.repaint();
	}

	// ---------------------------------------------------------------- simulation

	private void simulate() {
		String blackType = typeOf(OthelloBoard.P1);
		String whiteType = typeOf(OthelloBoard.P2);
		if (HUMAN.equals(blackType) || HUMAN.equals(whiteType)) {
			JOptionPane.showMessageDialog(this,
					"Simulation needs two computer players.\nSet both sides to Random or Greedy.",
					"Simulate", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		computerTimer.stop();
		setSimulating(true);
		status.setText("Simulating " + String.format("%,d", SIM_GAMES) + " games…");

		new SwingWorker<int[], Integer>() {
			@Override
			protected int[] doInBackground() {
				int p1wins = 0, p2wins = 0;
				for (int i = 0; i < SIM_GAMES; i++) {
					Othello game = new Othello();
					while (!game.isGameOver()) {
						char turn = game.getWhosTurn();
						Move m = computerMove(game, turn, turn == OthelloBoard.P1 ? blackType : whiteType);
						game.move(m.getRow(), m.getCol());
					}
					char winner = game.getWinner();
					if (winner == OthelloBoard.P1) {
						p1wins++;
					} else if (winner == OthelloBoard.P2) {
						p2wins++;
					}
					if ((i + 1) % 250 == 0) {
						publish(i + 1);
					}
				}
				return new int[] { p1wins, p2wins };
			}

			@Override
			protected void process(List<Integer> done) {
				status.setText(String.format("Simulating… %,d / %,d", done.get(done.size() - 1), SIM_GAMES));
			}

			@Override
			protected void done() {
				setSimulating(false);
				try {
					int[] r = get();
					int ties = SIM_GAMES - r[0] - r[1];
					String msg = String.format(
							"Black (X) = %s, White (O) = %s%n%,d games%n%n"
									+ "Probability P1 wins=%.4f%nProbability P2 wins=%.4f%nTies: %.4f",
							blackType, whiteType, SIM_GAMES,
							(float) r[0] / SIM_GAMES, (float) r[1] / SIM_GAMES, (float) ties / SIM_GAMES);
					JOptionPane.showMessageDialog(OthelloGUI.this, msg, "Simulation results",
							JOptionPane.INFORMATION_MESSAGE);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(OthelloGUI.this, "Simulation failed: " + ex,
							"Simulation", JOptionPane.ERROR_MESSAGE);
				}
				refresh();
				scheduleNextTurn();
			}
		}.execute();
	}

	private void setSimulating(boolean on) {
		simulating = on;
		blackChoice.setEnabled(!on);
		whiteChoice.setEnabled(!on);
		newGameButton.setEnabled(!on);
		simulateButton.setEnabled(!on);
	}

	// ---------------------------------------------------------------- drawing

	/** Draw one disc filling a square of the given size at (x, y). */
	private static void drawDisc(Graphics2D g, char player, int x, int y, int size) {
		int pad = Math.max(2, size / 9);
		int d = size - 2 * pad;
		int cx = x + pad, cy = y + pad;
		boolean black = (player == OthelloBoard.P1);

		g.setColor(new Color(0, 0, 0, 70)); // shadow
		g.fillOval(cx + 1, cy + 2, d, d);

		Color base = black ? new Color(0x161616) : new Color(0xF2F2EC);
		Color light = black ? new Color(0x5A5A5A) : Color.WHITE;
		g.setPaint(new RadialGradientPaint(new Point2D.Float(cx + d * 0.35f, cy + d * 0.3f), Math.max(1f, d * 0.75f),
				new float[] { 0f, 1f }, new Color[] { light, base }));
		g.fillOval(cx, cy, d, d);

		g.setColor(black ? Color.BLACK : new Color(0xB8B8B0));
		g.setStroke(new BasicStroke(1f));
		g.drawOval(cx, cy, d, d);
	}

	private class BoardPanel extends JPanel {
		private static final int MARGIN = 28;
		private static final int DIM = Othello.DIMENSION;
		private int hoverRow = -1, hoverCol = -1;

		BoardPanel() {
			int preferred = MARGIN * 2 + 66 * DIM;
			setPreferredSize(new Dimension(preferred, preferred));
			setBackground(BACKGROUND);

			MouseAdapter mouse = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					if (simulating || othello.isGameOver() || !isHumanTurn()) {
						return;
					}
					int[] rc = cellAt(e.getPoint());
					if (rc != null) {
						applyMove(new Move(rc[0], rc[1]));
					}
				}

				@Override
				public void mouseMoved(MouseEvent e) {
					int[] rc = cellAt(e.getPoint());
					int r = rc == null ? -1 : rc[0], c = rc == null ? -1 : rc[1];
					if (r != hoverRow || c != hoverCol) {
						hoverRow = r;
						hoverCol = c;
						repaint();
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {
					hoverRow = hoverCol = -1;
					repaint();
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
		}

		private int cell() {
			return Math.max(10, (Math.min(getWidth(), getHeight()) - 2 * MARGIN) / DIM);
		}

		private int originX() {
			return (getWidth() - cell() * DIM) / 2;
		}

		private int originY() {
			return (getHeight() - cell() * DIM) / 2;
		}

		private int[] cellAt(Point p) {
			int c = cell();
			int col = Math.floorDiv(p.x - originX(), c);
			int row = Math.floorDiv(p.y - originY(), c);
			if (row < 0 || row >= DIM || col < 0 || col >= DIM) {
				return null;
			}
			return new int[] { row, col };
		}

		@Override
		protected void paintComponent(Graphics g0) {
			super.paintComponent(g0);
			Graphics2D g = (Graphics2D) g0.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int c = cell(), ox = originX(), oy = originY(), size = c * DIM;

			// felt and grid
			g.setColor(FELT);
			g.fillRoundRect(ox - 4, oy - 4, size + 8, size + 8, 10, 10);
			g.setColor(GRID);
			g.setStroke(new BasicStroke(1.5f));
			for (int i = 0; i <= DIM; i++) {
				g.drawLine(ox + i * c, oy, ox + i * c, oy + size);
				g.drawLine(ox, oy + i * c, ox + size, oy + i * c);
			}
			int dot = Math.max(5, c / 9);
			for (int r : new int[] { 2, 6 }) {
				for (int col : new int[] { 2, 6 }) {
					g.fillOval(ox + col * c - dot / 2, oy + r * c - dot / 2, dot, dot);
				}
			}

			// row / column numbers, matching the terminal version
			g.setColor(LABEL);
			g.setFont(getFont().deriveFont(Font.BOLD, 12f));
			FontMetrics fm = g.getFontMetrics();
			for (int i = 0; i < DIM; i++) {
				String s = String.valueOf(i);
				g.drawString(s, ox + i * c + c / 2 - fm.stringWidth(s) / 2, oy - 10);
				g.drawString(s, ox - 12 - fm.stringWidth(s), oy + i * c + c / 2 + fm.getAscent() / 2 - 1);
			}

			// discs, legal-move hints, hover preview
			boolean human = !simulating && isHumanTurn();
			char turn = othello.getWhosTurn();
			for (int r = 0; r < DIM; r++) {
				for (int col = 0; col < DIM; col++) {
					int x = ox + col * c, y = oy + r * c;
					char token = othello.getToken(r, col);
					if (token != OthelloBoard.EMPTY) {
						drawDisc(g, token, x, y, c);
					} else if (human && isLegal(r, col)) {
						if (r == hoverRow && col == hoverCol) {
							Graphics2D ghost = (Graphics2D) g.create();
							ghost.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
							drawDisc(ghost, turn, x, y, c);
							ghost.dispose();
						} else {
							int d = Math.max(6, c / 4);
							g.setColor(HINT);
							g.fillOval(x + (c - d) / 2, y + (c - d) / 2, d, d);
						}
					}
				}
			}

			// mark the last move
			if (lastMove != null) {
				int d = Math.max(6, c / 7);
				int x = ox + lastMove.getCol() * c + (c - d) / 2;
				int y = oy + lastMove.getRow() * c + (c - d) / 2;
				g.setColor(LAST_MOVE);
				g.fillOval(x, y, d, d);
			}
			g.dispose();
		}
	}

	/** Small disc icon for the score labels. */
	private static class DiscIcon implements Icon {
		private final char player;

		DiscIcon(char player) {
			this.player = player;
		}

		public int getIconWidth() {
			return 22;
		}

		public int getIconHeight() {
			return 22;
		}

		public void paintIcon(Component comp, Graphics g0, int x, int y) {
			Graphics2D g = (Graphics2D) g0.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			drawDisc(g, player, x, y, 22);
			g.dispose();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
				// fall back to the default look and feel
			}
			new OthelloGUI().setVisible(true);
		});
	}
}
