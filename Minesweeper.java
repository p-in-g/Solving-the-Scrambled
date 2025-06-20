import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class Minesweeper {
    private class MineTile extends JButton {
        int r, c;
        public MineTile(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    int tileSize = 50;
    int numRows = 9;
    int numCols = 9;
    int boardWidth = numCols * tileSize;
    int boardHeight = numRows * tileSize;

    JFrame frame = new JFrame("Minesweeper");
    JLabel textLabel = new JLabel();
    JPanel textPanel = new JPanel();
    JPanel boardPanel = new JPanel();
    JButton hintButton = new JButton("Hint");

    int mineCount = 15;
    MineTile[][] board = new MineTile[numRows][numCols];
    ArrayList<MineTile> mineList = new ArrayList<>();
    Random random = new Random();

    int tilesClicked = 0;
    boolean gameOver = false;

    Minesweeper() {
        frame.setSize(boardWidth, boardHeight + 130);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        textLabel.setFont(new Font("Arial", Font.BOLD, 25));
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        textLabel.setText("Minesweeper: " + mineCount);
        textLabel.setOpaque(true);

        textPanel.setLayout(new BorderLayout());
        textPanel.add(textLabel, BorderLayout.CENTER);
        frame.add(textPanel, BorderLayout.NORTH);

        boardPanel.setLayout(new GridLayout(numRows, numCols));
        frame.add(boardPanel, BorderLayout.CENTER);

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                MineTile tile = new MineTile(r, c);
                board[r][c] = tile;

                tile.setFocusable(false);
                tile.setMargin(new Insets(0, 0, 0, 0));
                tile.setFont(new Font("Arial Unicode MS", Font.PLAIN, 40));

                tile.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (gameOver) return;

                        MineTile clickedTile = (MineTile) e.getSource();

                        if (e.getButton() == MouseEvent.BUTTON1) {
                            if (clickedTile.getText().equals("")) {
                                if (mineList.contains(clickedTile)) {
                                    revealMines();
                                } else {
                                    checkMine(clickedTile.r, clickedTile.c);
                                }
                            }
                        } else if (e.getButton() == MouseEvent.BUTTON3) {
                            if (clickedTile.getText().equals("") && clickedTile.isEnabled()) {
                                clickedTile.setText("🚩");
                            } else if (clickedTile.getText().equals("🚩")) {
                                clickedTile.setText("");
                            }
                        }
                    }
                });

                boardPanel.add(tile);
            }
        }

        hintButton.setFont(new Font("Arial", Font.BOLD, 18));
        hintButton.addActionListener(e -> provideHint());
        frame.add(hintButton, BorderLayout.SOUTH);

        frame.setVisible(true);
        setMines();
    }

    void setMines() {
        int mineLeft = mineCount;
        while (mineLeft > 0) {
            int r = random.nextInt(numRows);
            int c = random.nextInt(numCols);
            MineTile tile = board[r][c];
            if (!mineList.contains(tile)) {
                mineList.add(tile);
                mineLeft--;
            }
        }
    }

    void revealMines() {
        for (MineTile tile : mineList) {
            tile.setText("💣");
            tile.setForeground(Color.RED);
            tile.setBackground(Color.RED);
            tile.setEnabled(false);
        }
        gameOver = true;
        if (!textLabel.getText().contains("Win")) {
            textLabel.setText("💥 Game Over!");
            textLabel.setForeground(Color.RED);
        }
    }

    void checkMine(int r, int c) {
        if (r < 0 || r >= numRows || c < 0 || c >= numCols) return;
        MineTile tile = board[r][c];
        if (!tile.isEnabled() || tile.getText().equals("🚩")) return;

        tile.setEnabled(false);
        tilesClicked++;

        int minesFound = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr != 0 || dc != 0) {
                    minesFound += countMine(r + dr, c + dc);
                }
            }
        }

        if (minesFound > 0) {
            tile.setText(Integer.toString(minesFound));
        } else {
            tile.setText("");
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr != 0 || dc != 0) {
                        checkMine(r + dr, c + dc);
                    }
                }
            }
        }

        if (tilesClicked == numRows * numCols - mineList.size()) {
            gameOver = true;
            textLabel.setText("🎉 You Win! 🎉");
            textLabel.setForeground(new Color(0, 128, 0));
            textLabel.setFont(new Font("Arial", Font.BOLD, 28));
            revealMines();
        }
    }

    int countMine(int r, int c) {
        if (r < 0 || r >= numRows || c < 0 || c >= numCols) return 0;
        return mineList.contains(board[r][c]) ? 1 : 0;
    }

    void provideHint() {
        if (gameOver) return;

        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                MineTile tile = board[r][c];
                if (tile.isEnabled() && tile.getText().equals("") && !mineList.contains(tile)) {
                    checkMine(r, c);
                    JOptionPane.showMessageDialog(frame, "Hint: Safe cell at (" + (r + 1) + ", " + (c + 1) + ")");
                    return;
                }
            }
        }
        JOptionPane.showMessageDialog(frame, "No safe hints available!");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Minesweeper::new);
    }
}
