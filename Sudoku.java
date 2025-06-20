// filepath: /sudoku-java/sudoku-java/src/Sudoku.java
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Sudoku {
    private static final int SIZE = 9;

    public static void printBoard(int[][] board) {
        for (int i = 0; i < SIZE; i++) {
            if (i % 3 == 0 && i != 0)
                System.out.println("------+-------+------");
            for (int j = 0; j < SIZE; j++) {
                if (j % 3 == 0 && j != 0)
                    System.out.print("| ");
                System.out.print(board[i][j] == 0 ? ". " : board[i][j] + " ");
            }
        }
            System.out.println();
    }

    public static boolean isValid(int[][] board, int row, int col, int num) {
        for (int i = 0; i < SIZE; i++) {
            if (board[row][i] == num || board[i][col] == num)
                return false;
        }
        int boxRow = row - row % 3;
        int boxCol = col - col % 3;
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                if (board[boxRow + i][boxCol + j] == num)
                    return false;
        return true;
    }

    public static boolean solve(int[][] board) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == 0) {
                    for (int num = 1; num <= SIZE; num++) {
                        if (isValid(board, row, col, num)) {
                            board[row][col] = num;
                            if (solve(board))
                                return true;
                            board[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean fillBoard(int[][] board) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == 0) {
                    ArrayList<Integer> nums = new ArrayList<>();
                    for (int num = 1; num <= SIZE; num++) {
                        nums.add(num);
                    }
                    Collections.shuffle(nums);
                    for (int num : nums) {
                        if (isValid(board, row, col, num)) {
                            board[row][col] = num;
                            if (fillBoard(board))
                                return true;
                            board[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    public static int[][] generatePuzzle(int emptyCells) {
        int[][] board = new int[SIZE][SIZE];
        fillBoard(board);
        int[][] puzzle = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(board[i], 0, puzzle[i], 0, SIZE);
        }
        Random rand = new Random();
        int count = 0;
        while (count < emptyCells) {
            int row = rand.nextInt(SIZE);
            int col = rand.nextInt(SIZE);
            if (puzzle[row][col] != 0) {
                puzzle[row][col] = 0;
                count++;
            }
        }
        return puzzle;
    }
    
    // --- GUI Section ---
public static void launchGUI() {
    JFrame frame = new JFrame("Sudoku");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(550, 700);

    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JLabel titleLabel = new JLabel("Sudoku Game", SwingConstants.CENTER);
    titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
    mainPanel.add(titleLabel, BorderLayout.NORTH);

    JPanel boardPanel = new JPanel(new GridLayout(SIZE, SIZE));
    JTextField[][] cells = new JTextField[SIZE][SIZE];

    // Timer
    JLabel timerLabel = new JLabel("Time: 00:00", SwingConstants.CENTER);
    timerLabel.setFont(new Font("Arial", Font.PLAIN, 18));
    mainPanel.add(timerLabel, BorderLayout.SOUTH);

    Timer timer = new Timer(1000, null);
    final int[] seconds = {0};
    timer.addActionListener(e -> {
        seconds[0]++;
        int min = seconds[0] / 60;
        int sec = seconds[0] % 60;
        timerLabel.setText(String.format("Time: %02d:%02d", min, sec));
    });
    timer.start();

    // Highlighting
    Color highlightColor = new Color(220, 240, 255);

    for (int i = 0; i < SIZE; i++) {
        for (int j = 0; j < SIZE; j++) {
            JTextField cell = new JTextField();
            cell.setHorizontalAlignment(JTextField.CENTER);
            cell.setFont(new Font("Arial", Font.BOLD, 22));
            int top = (i % 3 == 0) ? 2 : 1;
            int left = (j % 3 == 0) ? 2 : 1;
            int bottom = (i == SIZE - 1) ? 2 : 1;
            int right = (j == SIZE - 1) ? 2 : 1;
            cell.setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, Color.BLACK));
            cells[i][j] = cell;
            boardPanel.add(cell);

            // Highlight row, col, box on focus
            cell.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent evt) {
                    for (int r = 0; r < SIZE; r++)
                        for (int c = 0; c < SIZE; c++)
                            cells[r][c].setBackground(Color.WHITE);
                    int row = -1, col = -1;
                    for (int r = 0; r < SIZE; r++)
                        for (int c = 0; c < SIZE; c++)
                            if (cells[r][c] == cell) { row = r; col = c; }
                    if (row != -1 && col != -1) {
                        for (int k = 0; k < SIZE; k++) {
                            cells[row][k].setBackground(highlightColor);
                            cells[k][col].setBackground(highlightColor);
                        }
                        int boxRow = row - row % 3;
                        int boxCol = col - col % 3;
                        for (int r = 0; r < 3; r++)
                            for (int c = 0; c < 3; c++)
                                cells[boxRow + r][boxCol + c].setBackground(highlightColor);
                        cell.setBackground(new Color(180, 220, 255));
                    }
                }
                @Override
                public void focusLost(java.awt.event.FocusEvent evt) {
                    for (int r = 0; r < SIZE; r++)
                        for (int c = 0; c < SIZE; c++)
                            cells[r][c].setBackground(Color.WHITE);
                }
            });

            // Add DocumentListener for real-time validation
            final int row = i, col = j;
            cell.getDocument().addDocumentListener(new DocumentListener() {
                void validateCell() {
                    String text = cell.getText();
                    if (text.isEmpty()) {
                        cell.setForeground(Color.BLUE);
                        return;
                    }
                    try {
                        int num = Integer.parseInt(text);
                        if (num < 1 || num > 9) {
                            cell.setForeground(Color.RED);
                            return;
                        }
                        int[][] tempBoard = new int[SIZE][SIZE];
                        for (int r = 0; r < SIZE; r++) {
                            for (int c = 0; c < SIZE; c++) {
                                String t = cells[r][c].getText();
                                tempBoard[r][c] = (t.isEmpty() ? 0 : Integer.parseInt(t));
                            }
                        }
                        tempBoard[row][col] = 0;
                        if (isValid(tempBoard, row, col, num)) {
                            cell.setForeground(Color.BLUE);
                        } else {
                            cell.setForeground(Color.RED);
                        }
                    } catch (NumberFormatException e) {
                        cell.setForeground(Color.RED);
                    }
                }
                @Override public void insertUpdate(DocumentEvent e) { validateCell(); }
                @Override public void removeUpdate(DocumentEvent e) { validateCell(); }
                @Override public void changedUpdate(DocumentEvent e) { validateCell(); }
            });
        }
    }

    // Control panel with more buttons
    JButton generateBtn = new JButton("Generate Puzzle");
    JButton solveBtn = new JButton("Solve");
    JButton clearBtn = new JButton("Clear");
    JButton checkBtn = new JButton("Check");

    JPanel controlPanel = new JPanel();
    controlPanel.setLayout(new GridLayout(1, 4, 10, 10));
    controlPanel.add(generateBtn);
    controlPanel.add(solveBtn);
    controlPanel.add(checkBtn);
    controlPanel.add(clearBtn);

    mainPanel.add(boardPanel, BorderLayout.CENTER);
    mainPanel.add(controlPanel, BorderLayout.NORTH);
    mainPanel.add(timerLabel, BorderLayout.SOUTH);

    frame.setContentPane(mainPanel);

    generateBtn.addActionListener(e -> {
        String input = JOptionPane.showInputDialog(frame, "Number of cells to remove (30-60 recommended):", "40");
        int emptyCells = 40;
        try {
            emptyCells = Integer.parseInt(input);
        } catch (NumberFormatException | NullPointerException ignored) {}
        int[][] puzzle = generatePuzzle(emptyCells);
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (puzzle[i][j] == 0) {
                    cells[i][j].setText("");
                    cells[i][j].setEditable(true);
                    cells[i][j].setForeground(Color.BLUE);
                } else {
                    cells[i][j].setText(String.valueOf(puzzle[i][j]));
                    cells[i][j].setEditable(false);
                    cells[i][j].setForeground(Color.BLACK);
                }
            }
        }
        seconds[0] = 0; // reset timer
        timer.restart();
    });

    solveBtn.addActionListener(e -> {
        int[][] board = new int[SIZE][SIZE];
        try {
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    String text = cells[i][j].getText();
                    board[i][j] = (text.isEmpty() ? 0 : Integer.parseInt(text));
                }
            }
            if (solve(board)) {
                for (int i = 0; i < SIZE; i++) {
                    for (int j = 0; j < SIZE; j++) {
                        cells[i][j].setText(String.valueOf(board[i][j]));
                        cells[i][j].setEditable(false);
                        cells[i][j].setForeground(Color.BLACK);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(frame, "No solution exists.");
            }
        } catch (NumberFormatException | NullPointerException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid input detected.");
        }
    });

    checkBtn.addActionListener(e -> {
        boolean valid = true;
        int[][] board = new int[SIZE][SIZE];
        try {
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    String text = cells[i][j].getText();
                    board[i][j] = (text.isEmpty() ? 0 : Integer.parseInt(text));
                }
            }
            outer: for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    String text = cells[i][j].getText();
                    if (!text.isEmpty()) {
                        int num = Integer.parseInt(text);
                        // Temporarily clear this cell for validation
                        board[i][j] = 0;
                        if (!isValid(board, i, j, num)) {
                            cells[i][j].setForeground(Color.RED);
                            valid = false;
                        } else {
                            cells[i][j].setForeground(Color.BLUE);
                        }
                        board[i][j] = num;
                    }
                }
            }
            if (valid) {
                JOptionPane.showMessageDialog(frame, "All entries are valid so far!");
            } else {
                JOptionPane.showMessageDialog(frame, "There are mistakes highlighted in red.");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Invalid input detected.");
        }
    });

    clearBtn.addActionListener(e -> {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                cells[i][j].setText("");
                cells[i][j].setEditable(true);
                cells[i][j].setForeground(Color.BLUE);
                cells[i][j].setBackground(Color.WHITE);
            }
        }
        seconds[0] = 0; // reset timer
        timer.restart();
    });

    frame.setVisible(true);
}
}