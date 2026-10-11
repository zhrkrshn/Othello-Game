import java.util.ArrayList;
import java.util.Random;

/**
 * PlayerRandom makes a move by first determining all possible moves that this
 * player can make, putting them in an ArrayList, and then randomly choosing one
 * of them.
 * 
 *
 */
public class PlayerRandom {
	private Random rand = new Random();
	private Othello othello;
	private char player;
	public PlayerRandom(Othello othello, char player) {
		this.othello = othello;
		this.player = player;
	}
	public Move getMove() {
		ArrayList<Move> moves = new ArrayList<Move>();
		for (int row = 0; row < Othello.DIMENSION; row++) {
			for (int col = 0; col < Othello.DIMENSION; col++) {
				Othello trial = othello.copy();
				if (trial.move(row, col)) {
					moves.add(new Move(row, col));
				}
			}
		}
		if (moves.isEmpty()) {
			return null;
		}
		return moves.get(rand.nextInt(moves.size()));
	}
}
