/**
 * PlayerGreedy makes a move by considering all possible moves that the player
 * can make. Each move leaves the player with a total number of tokens.
 * getMove() returns the first move which maximizes the number of
 * tokens owned by this player. In case of a tie, between two moves,
 * (row1,column1) and (row2,column2) the one with the smallest row wins. In case
 * both moves have the same row, then the smaller column wins.
 * 
 * Example: Say moves (2,7) and (3,1) result in the maximum number of tokens for
 * this player. Then (2,7) is returned since 2 is the smaller row.
 * 
 * Example: Say moves (2,7) and (2,4) result in the maximum number of tokens for
 * this player. Then (2,4) is returned, since the rows are tied, but (2,4) has
 * the smaller column.
 * 
 *
 */

public class PlayerGreedy {
	private Othello othello;
	private char player;

	public PlayerGreedy(Othello othello, char player) {
		this.othello = othello;
		this.player = player;
	}
	public Move getMove() {
		Move bestMove = null;
		int bestCount = -1;

		for (int row = 0; row < Othello.DIMENSION; row++) {
			for (int col = 0; col < Othello.DIMENSION; col++) {
				Othello trial = othello.copy();
				if (trial.move(row, col)) {
					int count = trial.getCount(player);
					if (count > bestCount) {
						bestCount = count;
						bestMove = new Move(row, col);
					}
				}
			}
		}
		return bestMove;
	}
}
