import java.util.*;
import javax.swing.SwingUtilities;
public class App {
    public static void main(String[] args) throws Exception {
             System.out.println("GAME MENU:\n1 - Minesweeper\n2 - Sudoku\n3 - Klondike Solitaire\n0 - Exit\nChoose your preferred game : ");
        Scanner sr=new Scanner(System.in);
        int n=sr.nextInt();
            switch (n) {
                case 0 -> {
                    return;
                }
                case 1 -> new Minesweeper();
                case 2 -> Sudoku.launchGUI();
                case 3 -> SwingUtilities.invokeLater(() -> {
                        ks game = new ks();
                        game.setVisible(true);
                    });
                default -> System.out.println("Invalid Choice! Choose again : ");
            }
        }
    }