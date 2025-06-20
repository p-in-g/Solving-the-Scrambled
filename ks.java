import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

// Card class
class Card implements Serializable{
    enum Suit {HEARTS, DIAMONDS, CLUBS, SPADES}
    int rank; // 1=Ace, 11=Jack, 12=Queen, 13=King
    Suit suit;
    boolean faceUp;

    Card(int r, Suit s) {
        rank = r;
        suit = s;
        faceUp = false;
    }

    boolean isRed() {
        return suit == Suit.HEARTS || suit == Suit.DIAMONDS;
    }

    @Override
    public String toString() {
        String r = switch(rank) {
            case 1 -> "A"; case 11 -> "J"; case 12 -> "Q"; case 13 -> "K";
            default -> String.valueOf(rank);
        };
        return r + switch(suit) {
            case HEARTS -> "♥";
            case DIAMONDS -> "♦";
            case CLUBS -> "♣";
            case SPADES -> "♠";
        };
    }
}

// Pile interface
interface Pile {
    List<Card> getCards();
    void addCard(Card c);
    Card removeTopCard();
    Card topCard();
    boolean isEmpty();
}

// Simple implementation of pile
class SimplePile implements Pile,Serializable {
    protected LinkedList<Card> cards = new LinkedList<>();

    @Override
    public List<Card> getCards() { return cards; }
    @Override
    public void addCard(Card c) { cards.add(c); }
    @Override
    public Card removeTopCard() { return cards.isEmpty() ? null : cards.removeLast(); }
    @Override
    public Card topCard() { return cards.isEmpty() ? null : cards.getLast(); }
    @Override
    public boolean isEmpty() { return cards.isEmpty(); }
    public int size() { return cards.size(); }
}

// Tableau pile
class TableauPile extends SimplePile {
    // Validate if a card (or sequence) can be placed on this pile
    boolean canPlace(Card c) {
        if (isEmpty()) return c.rank == 13; // Only King can start empty tableau
        Card top = topCard();
        return top.faceUp && top.rank == c.rank + 1 && top.isRed() != c.isRed();
    }
}

// Foundation pile
class FoundationPile extends SimplePile {
    Card.Suit suit;

    FoundationPile(Card.Suit s) { suit = s; }

    boolean canPlace(Card c) {
        if (c.suit != suit) return false;
        if (isEmpty()) return c.rank == 1; // Ace start foundation
        Card top = topCard();
        return c.rank == top.rank + 1;
    }
}

// Stock pile
class StockPile extends SimplePile {
    int recycleCount = 0;
    final int maxRecycles = 3;

    // Draw card to waste
    Card draw() {
        if (cards.isEmpty()) return null;
        return removeTopCard();
    }

    boolean canRecycle() {
        return recycleCount < maxRecycles;
    }

    void recycle(LinkedList<Card> waste) {
        if (!canRecycle()) return;
        while (!waste.isEmpty()) {
            Card c = waste.removeLast();
            c.faceUp = false;
            addCard(c);
        }
        recycleCount++;
    }
}

// Waste pile
class WastePile extends SimplePile {}

// Game model holding all piles and rules
class KlondikeModel implements Cloneable,Serializable{
    TableauPile[] tableau = new TableauPile[7];
    FoundationPile[] foundation = new FoundationPile[4];
    StockPile stock = new StockPile();
    WastePile waste = new WastePile();

    public String getStateHash() {
    try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (IOException e) {
        throw new RuntimeException("Unable to compute state hash", e);
    }
}


    // Constructor - initialize deck, shuffle, deal cards
    KlondikeModel() {
        for (int i = 0; i < 7; i++) tableau[i] = new TableauPile();
        foundation[0] = new FoundationPile(Card.Suit.HEARTS);
        foundation[1] = new FoundationPile(Card.Suit.DIAMONDS);
        foundation[2] = new FoundationPile(Card.Suit.CLUBS);
        foundation[3] = new FoundationPile(Card.Suit.SPADES);

        List<Card> deck = new ArrayList<>();
        for (Card.Suit s : Card.Suit.values())
            for (int r = 1; r <= 13; r++)
                deck.add(new Card(r, s));

        Collections.shuffle(deck);
        
        // Deal cards to tableau
        int index = 0;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j <= i; j++) {
                Card c = deck.get(index++);
                tableau[i].addCard(c);
                c.faceUp = (j == i);
            }
        }
        // Remaining to stock
        for (; index < deck.size(); index++)
            stock.addCard(deck.get(index));
    }

    // Move card from tableau to foundation, or tableau to tableau, waste to tableau/foundation, etc.
    boolean moveTableauToFoundation(int tabIndex, int foundationIndex) {
        TableauPile t = tableau[tabIndex];
        FoundationPile f = foundation[foundationIndex];
        if (t.isEmpty()) return false;
        Card c = t.topCard();
        if (!c.faceUp) return false;
        if (f.canPlace(c)) {
            f.addCard(t.removeTopCard());
            flipTopTableau(tabIndex);
            return true;
        }
        return false;
    }

    boolean moveWasteToFoundation(int foundationIndex) {
        if (waste.isEmpty()) return false;
        Card c = waste.topCard();
        FoundationPile f = foundation[foundationIndex];
        if (f.canPlace(c)) {
            waste.removeTopCard();
            f.addCard(c);
            return true;
        }
        return false;
    }

    boolean moveWasteToTableau(int tabIndex) {
        if (waste.isEmpty()) return false;
        Card c = waste.topCard();
        TableauPile t = tableau[tabIndex];
        if (t.canPlace(c)) {
            waste.removeTopCard();
            t.addCard(c);
            return true;
        }
        return false;
    }

    boolean moveTableauToTableau(int fromIndex, int toIndex, int startCardIndex) {
        TableauPile from = tableau[fromIndex];
        TableauPile to = tableau[toIndex];
        if (from.isEmpty()) return false;

        // Cards sequence from startCardIndex to end must be faceUp and valid sequence
        List<Card> cards = from.getCards();
        if (startCardIndex < 0 || startCardIndex >= cards.size()) return false;
        if (!cards.get(startCardIndex).faceUp) return false;

        // Validate sequence descending rank alternating colors
        for (int i = startCardIndex; i < cards.size() - 1; i++) {
            Card c1 = cards.get(i);
            Card c2 = cards.get(i + 1);
            if (!(c1.rank == c2.rank + 1 && c1.isRed() != c2.isRed()))
                return false;
        }

        Card firstCard = cards.get(startCardIndex);
        if (to.isEmpty()) {
            if (firstCard.rank != 13) return false; // must start with King
        } else {
            Card top = to.topCard();
            if (!(top.rank == firstCard.rank + 1 && top.isRed() != firstCard.isRed() && top.faceUp)) return false;
        }

        // Move sequence cards
        List<Card> toMove = new ArrayList<>(cards.subList(startCardIndex, cards.size()));
        for (int i = cards.size() - 1; i >= startCardIndex; i--) {
            from.removeTopCard();
        }
        to.getCards().addAll(toMove);

        flipTopTableau(fromIndex);
        return true;
    }

    boolean flipTopTableau(int tabIndex) {
        TableauPile t = tableau[tabIndex];
        if (!t.isEmpty()) {
            Card c = t.topCard();
            if (!c.faceUp) {
                c.faceUp = true;
                return true;
            }
        }
        return false;
    }

    // Draw from stock to waste
    boolean drawFromStock() {
        if (stock.isEmpty()) return false;
        Card c = stock.draw();
        c.faceUp = true;
        waste.addCard(c);
        return true;
    }

    // Recycle waste into stock if possible
    boolean recycleStock() {
        if (!stock.canRecycle()) return false;
        stock.recycle(waste.cards);
        return true;
    }

    // Check if game won
    boolean isWon() {
        for (FoundationPile f : foundation) {
            if (f.size() != 13) return false;
        }
        return true;
    }

    public KlondikeModel clone() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (KlondikeModel) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to clone game state", e);
        }
    }
}

// GUI for cards
class CardButton extends JButton {
    Card card;

    CardButton(Card c) {
        this.card = c;
        updateText();
        setMargin(new Insets(0,0,0,0));
        setPreferredSize(new Dimension(60, 90));
        setFont(new Font("SansSerif", Font.BOLD, 18));
    }

    void updateText() {
        setText(card.faceUp ? card.toString() : "X");
        setForeground(card.faceUp ? (card.isRed() ? Color.RED : Color.BLACK) : Color.GRAY);
    }
}

// Main GUI class
public class ks extends JFrame {
    KlondikeModel model;
    Stack<KlondikeModel> undoStack = new Stack<>();
    JPanel stockPanel = new JPanel();
    JPanel wastePanel = new JPanel();
    JPanel foundationPanel = new JPanel();
    JPanel tableauPanel = new JPanel();

    JButton btnDraw = new JButton("Draw");
    JButton btnRecycle = new JButton("Recycle");
    JButton btnHint = new JButton("Hint");
    JButton btnUndo = new JButton("Undo");
    JButton btnNewGame = new JButton("New Game");


    // Track selected cards for moves
    int selectedTableauIndex = -1;
    int selectedCardIndex = -1; // For tableau sequences
    boolean wasteSelected = false;

    String lastMove = null;   // actual move done
    String lastHint = null;   // last hint shown

    private void saveState() {
        undoStack.push(model.clone());
    }
    public ks() {
        model = new KlondikeModel();

        setTitle("Klondike Solitaire");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Top panel with stock, waste, foundation
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(stockPanel);
        topPanel.add(wastePanel);

        foundationPanel.setLayout(new GridLayout(1, 4, 10, 0));
        topPanel.add(foundationPanel);

        add(topPanel, BorderLayout.NORTH);

        // Tableau in center
        tableauPanel.setLayout(new GridLayout(1, 7, 10, 0));
        add(tableauPanel, BorderLayout.CENTER);

        // Bottom control buttons
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(btnDraw);
        bottomPanel.add(btnRecycle);
        bottomPanel.add(btnHint);
        bottomPanel.add(btnUndo);
        bottomPanel.add(btnNewGame);

        add(bottomPanel, BorderLayout.SOUTH);

        btnDraw.addActionListener(e -> {
            saveState();
            if (model.drawFromStock()) {
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "Stock empty. Use Recycle if available.");
            }
        });

        btnRecycle.addActionListener(e -> {
            saveState();
            if (model.recycleStock()) {
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "No more recycles left.");
            }
        });

        btnHint.addActionListener(e -> {
            String hint = getHint();
            JOptionPane.showMessageDialog(this, hint == null ? "No moves found." : "Hint: " + hint);
        });
        
        btnUndo.addActionListener(e -> {
    if (!undoStack.isEmpty()) {
        model = undoStack.pop();
        selectedTableauIndex = -1;
        selectedCardIndex = -1;
        wasteSelected = false;
        refresh();
    } else {
        JOptionPane.showMessageDialog(this, "No moves to undo.");
    }
});

    btnNewGame.addActionListener(e -> {
    model = new KlondikeModel();
    undoStack.clear();
    selectedTableauIndex = -1;
    selectedCardIndex = -1;
    wasteSelected = false;
    refresh();
});



        refresh();
    }

    void refresh() {
        stockPanel.removeAll();
        wastePanel.removeAll();
        foundationPanel.removeAll();
        tableauPanel.removeAll();

        // Stock pile button
        JButton stockBtn = new JButton("Stock (" + model.stock.size() + ")");
        stockBtn.setEnabled(!model.stock.isEmpty());
        stockBtn.addActionListener(e -> {
            if (model.drawFromStock()) refresh();
        });
        stockPanel.add(stockBtn);

        // Waste pile (top card only)
        if (!model.waste.isEmpty()) {
            Card wasteTop = model.waste.topCard();
            CardButton wasteBtn = new CardButton(wasteTop);
            wasteBtn.addActionListener(e -> {
                wasteSelected = !wasteSelected;
                selectedTableauIndex = -1;
                selectedCardIndex = -1;
                refresh();
            });
            if (wasteSelected) wasteBtn.setBackground(Color.YELLOW);
            wastePanel.add(wasteBtn);
        } else {
            wastePanel.add(new JLabel("Waste empty"));
        }

        // Foundations
        for (int i = 0; i < 4; i++) {
            FoundationPile f = model.foundation[i];
            JButton fbtn;
            if (f.isEmpty()) fbtn = new JButton("Empty");
            else fbtn = new CardButton(f.topCard());
            int idx = i;
            fbtn.addActionListener(e -> {
                if (wasteSelected) {
                    saveState();
                    if (model.moveWasteToFoundation(idx)) {
                        wasteSelected = false;
                        refresh();
                    }
                } else if (selectedTableauIndex != -1) {
                    saveState();
                    if (model.moveTableauToFoundation(selectedTableauIndex, idx)) {
                        selectedTableauIndex = -1;
                        selectedCardIndex = -1;
                        refresh();
                    }
                }
            });
            foundationPanel.add(fbtn);
        }

        // Tableau piles using JLayeredPane for overlapping cards
for (int i = 0; i < 7; i++) {
    final int ti = i;
    JLayeredPane layeredPile = new JLayeredPane();
    layeredPile.setPreferredSize(new Dimension(60, 400)); // enough height for stacked cards
    layeredPile.setLayout(null); // important for absolute positioning

    TableauPile t = model.tableau[i];
    List<Card> cards = t.getCards();

    if (cards.isEmpty()) {
        JButton emptyBtn = new JButton("Empty");
        emptyBtn.setBounds(0, 0, 60, 90);
        int vi = i;
        emptyBtn.addActionListener(e -> {
            if (wasteSelected) {
                saveState();
                if (model.moveWasteToTableau(vi)) {
                    wasteSelected = false;
                    refresh();
                }
            } else if (selectedTableauIndex != -1) {
                saveState();
                if (model.moveTableauToTableau(selectedTableauIndex, vi, selectedCardIndex)) {
                    selectedTableauIndex = -1;
                    selectedCardIndex = -1;
                    refresh();
                }
            }
        });
        layeredPile.add(emptyBtn, Integer.valueOf(0));
    } else {
        int yOffset = 0;
        for (int j = 0; j < cards.size(); j++) {
            Card c = cards.get(j);
            CardButton cb = new CardButton(c);
            cb.setBounds(0, yOffset, 60, 90);
            int ui = i, ci = j;

            cb.addActionListener(e -> {
                if (wasteSelected) {
                    saveState();
                    if (model.moveWasteToTableau(ui)) {
                        wasteSelected = false;
                        refresh();
                    }
                } else if (selectedTableauIndex == -1) {
                    if (c.faceUp) {
                        selectedTableauIndex = ui;
                        selectedCardIndex = ci;
                    }
                    refresh();
                } else {
                    saveState();
                    if (model.moveTableauToTableau(selectedTableauIndex, ui, selectedCardIndex)) {
                        selectedTableauIndex = -1;
                        selectedCardIndex = -1;
                        refresh();
                    } else {
                        if (selectedTableauIndex == ui) {
                            selectedTableauIndex = -1;
                            selectedCardIndex = -1;
                            refresh();
                        }
                    }
                }
            });

            if (i == selectedTableauIndex && j == selectedCardIndex) {
                cb.setBackground(Color.CYAN);
            }

            layeredPile.add(cb, Integer.valueOf(j));
            yOffset += c.faceUp ? 50 : 10;
        }
    }

    tableauPanel.add(layeredPile);
}


        revalidate();
        repaint();

        if (model.isWon()) {
            JOptionPane.showMessageDialog(this, "Congratulations! You won!");
        }
    }

    private JButton createPlaceholderButton() {
    JButton empty = new JButton();
    empty.setPreferredSize(new Dimension(60, 90));
    empty.setEnabled(false);
    empty.setBackground(Color.LIGHT_GRAY);
    empty.setOpaque(true);
    empty.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    return empty;
}

    boolean isReverseOfLastMove(String hint, String lastMove) {
    if (hint == null || lastMove == null) return false;

    // Very basic reversals, expand for more detail
    if (hint.equalsIgnoreCase(reverseHint(lastMove))) return true;
    return false;
}

String reverseHint(String move) {
    if (move == null) return null;

    if (move.startsWith("Move waste to tableau ")) {
        String num = move.replace("Move waste to tableau ", "");
        return "Move tableau " + num + " to waste";
    }
    if (move.startsWith("Move tableau") && move.contains("to waste")) {
        String num = move.replace("Move tableau ", "").replace(" to waste", "");
        return "Move waste to tableau " + num;
    }
    if (move.startsWith("Move cards from tableau ")) {
        String[] parts = move.split(" ");
        return "Move cards from tableau " + parts[5] + " to tableau " + parts[3];
    }

    return "";
}


    String getHint() {
    Set<String> visited = new HashSet<>();
    String hint = dfs(model, visited, 0);

    // If hint is null or a reverse of the last move, try again skipping reverse
    if (hint == null || isReverseOfLastMove(hint, lastMove)) {
        visited.clear();
        hint = dfsSkippingReverse(model, visited, 0);
    }

    // Only store hint if valid
    if (hint != null) {
        lastHint = hint;
    }

    return hint;
}

String dfsSkippingReverse(KlondikeModel model, Set<String> visited, int depth) {
    if (depth > 10) return null;

    String hash = model.getStateHash();
    if (visited.contains(hash)) return null;
    visited.add(hash);

    // Same as `dfs()` but skip if it reverses last move
    String[] moves = {
        tryMoveWasteToFoundation(model),
        tryMoveWasteToTableau(model),
        tryMoveTableauToFoundation(model),
        tryMoveTableauToTableau(model),
        tryDrawFromStock(model, visited, depth),
        tryRecycleWaste(model, visited, depth)
    };

    for (String move : moves) {
        if (move != null && !isReverseOfLastMove(move, lastMove)) {
            return move;
        }
    }

    return null;
}
String tryMoveWasteToFoundation(KlondikeModel model) {
    if (!model.waste.isEmpty()) {
        Card c = model.waste.topCard();
        for (int i = 0; i < 4; i++) {
            if (model.foundation[i].canPlace(c)) {
                return "Move waste to foundation " + model.foundation[i].suit;
            }
        }
    }
    return null;
}
String tryMoveWasteToTableau(KlondikeModel model) {
    if (!model.waste.isEmpty()) {
        Card c = model.waste.topCard();
        for (int i = 0; i < 7; i++) {
            if (model.tableau[i].canPlace(c)) {
                return "Move waste to tableau " + (i + 1);
            }
        }
    }
    return null;
}
String tryMoveTableauToFoundation(KlondikeModel model) {
    for (int i = 0; i < 7; i++) {
        TableauPile tab = model.tableau[i];
        if (!tab.isEmpty()) {
            Card top = tab.topCard();
            if (top.faceUp) {
                for (int j = 0; j < 4; j++) {
                    if (model.foundation[j].canPlace(top)) {
                        return "Move tableau " + (i + 1) + " to foundation " + model.foundation[j].suit;
                    }
                }
            }
        }
    }
    return null;
}
String tryMoveTableauToTableau(KlondikeModel model) {
    for (int from = 0; from < 7; from++) {
        List<Card> fromCards = model.tableau[from].getCards();
        for (int i = 0; i < fromCards.size(); i++) {
            if (!fromCards.get(i).faceUp) continue;

            for (int to = 0; to < 7; to++) {
                if (from == to) continue;

                KlondikeModel copy = model.clone();
                if (copy.moveTableauToTableau(from, to, i)) {
                    return "Move cards from tableau " + (from + 1) + " to tableau " + (to + 1);
                }
            }
        }
    }
    return null;
}
String tryDrawFromStock(KlondikeModel model, Set<String> visited, int depth) {
    if (!model.stock.isEmpty()) {
        KlondikeModel copy = model.clone();
        copy.drawFromStock();
        String deeperHint = dfsSkippingReverse(copy, visited, depth + 1);
        if (deeperHint != null) {
            return "Draw card from stock, then: " + deeperHint;
        }
    }
    return null;
}
String tryRecycleWaste(KlondikeModel model, Set<String> visited, int depth) {
    if (model.stock.canRecycle()) {
        KlondikeModel copy = model.clone();
        copy.recycleStock();
        String deeperHint = dfsSkippingReverse(copy, visited, depth + 1);
        if (deeperHint != null) {
            return "Recycle waste to stock, then: " + deeperHint;
        }
    }
    return null;
}


String dfs(KlondikeModel model, Set<String> visited, int depth) {
    if (depth > 10) return null;

    String hash = model.getStateHash();
    if (visited.contains(hash)) return null;
    visited.add(hash);

    // Waste to Foundation
    if (!model.waste.isEmpty()) {
        Card c = model.waste.topCard();
        for (int i = 0; i < 4; i++) {
            if (model.foundation[i].canPlace(c)) {
                String hint = "Move waste to foundation " + model.foundation[i].suit;
                if (!isReverseOfLastMove(hint, lastMove)) return hint;
            }
        }
    }

    // Waste to Tableau
    if (!model.waste.isEmpty()) {
        Card c = model.waste.topCard();
        for (int i = 0; i < 7; i++) {
            if (model.tableau[i].canPlace(c)) {
                String hint = "Move waste to tableau " + (i + 1);
                if (!isReverseOfLastMove(hint, lastMove)) return hint;
            }
        }
    }

    // Tableau to Foundation
    for (int i = 0; i < 7; i++) {
        TableauPile tab = model.tableau[i];
        if (!tab.isEmpty()) {
            Card top = tab.topCard();
            if (top.faceUp) {
                for (int j = 0; j < 4; j++) {
                    if (model.foundation[j].canPlace(top)) {
                        String hint = "Move tableau " + (i + 1) + " to foundation " + model.foundation[j].suit;
                        if (!isReverseOfLastMove(hint, lastMove)) return hint;
                    }
                }
            }
        }
    }

    // Tableau to Tableau
    for (int from = 0; from < 7; from++) {
        List<Card> fromCards = model.tableau[from].getCards();
        for (int i = 0; i < fromCards.size(); i++) {
            if (!fromCards.get(i).faceUp) continue;

            for (int to = 0; to < 7; to++) {
                if (from == to) continue;

                KlondikeModel copy = model.clone();
                if (copy.moveTableauToTableau(from, to, i)) {
                    String hint = "Move cards from tableau " + (from + 1) + " to tableau " + (to + 1);
                    if (!isReverseOfLastMove(hint, lastMove)) return hint;
                }
            }
        }
    }

    // Draw from stock
    if (!model.stock.isEmpty()) {
        KlondikeModel copy = model.clone();
        copy.drawFromStock();
        String deeperHint = dfs(copy, visited, depth + 1);
        if (deeperHint != null) {
            String hint = "Draw card from stock, then: " + deeperHint;
            if (!isReverseOfLastMove(hint, lastMove)) return hint;
        }
    }

    // Recycle waste
    if (model.stock.canRecycle()) {
        KlondikeModel copy = model.clone();
        copy.recycleStock();
        String deeperHint = dfs(copy, visited, depth + 1);
        if (deeperHint != null) {
            String hint = "Recycle waste to stock, then: " + deeperHint;
            if (!isReverseOfLastMove(hint, lastMove)) return hint;
        }
    }

    return null;
}
}