/**
 * The goal here is to print out the probability that Random wins and Greedy
 * wins as a result of playing 10000 games against each other with P1=Random and
 * P2=Greedy. What is your conclusion, which is the better strategy?
 *
 */
public class OthelloControllerRandomVSGreedy {

	/**
	 * Run main to execute the simulation and print out the two line results.
	 * Output looks like: 
	 * Probability P1 wins=.75 
	 * Probability P2 wins=.20
	 * @param args
	 */
	public static void main(String[] args) {
		
		int p1wins = 0, p2wins = 0, numGames = 10000;
		for (int i = 0; i < numGames; i++) {
			Othello othello = new Othello();
			PlayerRandom player1 = new PlayerRandom(othello, OthelloBoard.P1);
			PlayerGreedy player2 = new PlayerGreedy(othello, OthelloBoard.P2);
			while (!othello.isGameOver()) {
				Move move;
				if (othello.getWhosTurn() == OthelloBoard.P1) {
					move = player1.getMove();
				} else {
					move = player2.getMove();
				}
				othello.move(move.getRow(), move.getCol());
			}
			char winner = othello.getWinner();
			if (winner == OthelloBoard.P1) {
				p1wins++;
			} else if (winner == OthelloBoard.P2) {
				p2wins++;
			}
		}
		System.out.println("Probability P1 wins=" + (float) p1wins / numGames);
		System.out.println("Probability P2 wins=" + (float) p2wins / numGames);
	}
}
