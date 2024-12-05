import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

class ChessGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChessBoard::new);
    }
}

class ChessBoard extends JFrame {
    private JButton[][] board = new JButton[8][8]; // Tabuleiro 8x8
    private String currentPlayer = "White"; // Jogador atual
    private JButton selectedPiece = null; // Peça selecionada
    private int selectedRow = -1, selectedCol = -1; // Posição da peça selecionada
    private JLabel statusLabel; // Label para status do jogo
    private boolean[][] hasMoved = new boolean[8][8]; // Controle de movimentos (roque)
    
    public ChessBoard() {
        setTitle("Jogo de Xadrez");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        statusLabel = new JLabel("Vez do jogador: " + currentPlayer);
        add(statusLabel, BorderLayout.NORTH);

        JPanel boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(8, 8)); // Layout em grade para o tabuleiro

        initializeBoard(boardPanel);
        add(boardPanel, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        JMenuItem saveItem = new JMenuItem("Salvar Jogo");
        JMenuItem loadItem = new JMenuItem("Carregar Jogo");
        JMenuItem restartItem = new JMenuItem("Reiniciar Jogo");
        JMenuItem exitItem = new JMenuItem("Sair");
        JMenuItem difficultyItem = new JMenuItem("Selecionar Dificuldade");

        saveItem.addActionListener(e -> saveGame());
        loadItem.addActionListener(e -> loadGame());
        restartItem.addActionListener(e -> restartGame());
        exitItem.addActionListener(e -> System.exit(0));
        difficultyItem.addActionListener(e -> selectDifficulty());

        menu.add(saveItem);
        menu.add(loadItem);
        menu.add(restartItem);
        menu.add(difficultyItem);
        menu.add(exitItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        setVisible(true);
    }

    private void selectDifficulty() {
        String[] options = {"Fácil", "Normal", "Difícil"};
        String difficulty = (String) JOptionPane.showInputDialog(null, "Escolha a dificuldade:", "Dificuldade",
                JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        if (difficulty != null) {
            System.out.println("Dificuldade selecionada: " + difficulty);
        }
    }

    private void restartGame() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                board[row][col].setText("");
                hasMoved[row][col] = false;
            }
        }
        setupPieces();
        currentPlayer = "White";
        statusLabel.setText("Vez do jogador: " + currentPlayer);
    }

    private void loadGame() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("chess_game.ser"))) {
            String[][] boardState = (String[][]) ois.readObject();
            currentPlayer = (String) ois.readObject();
            hasMoved = (boolean[][]) ois.readObject();

            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    board[row][col].setText(boardState[row][col]);
                }
            }
            statusLabel.setText("Vez do jogador: " + currentPlayer);
            JOptionPane.showMessageDialog(this, "Jogo carregado com sucesso!");
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar o jogo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveGame() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("chess_game.ser"))) {
            oos.writeObject(boardToString());
            oos.writeObject(currentPlayer);
            oos.writeObject(hasMoved);
            JOptionPane.showMessageDialog(this, "Jogo salvo com sucesso!");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar o jogo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String[][] boardToString() {
        String[][] boardState = new String[8][8];
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boardState[row][col] = board[row][col].getText();
            }
        }
        return boardState;
    }

    private void initializeBoard(JPanel boardPanel) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JButton button = new JButton();
                button.setBackground((row + col) % 2 == 0 ? Color.WHITE : Color.GRAY); // Cor alternada
                button.setActionCommand(row + "," + col); // Identificador da posição
                button.addActionListener(new MoveListener());
                board[row][col] = button;
                boardPanel.add(button);
            }
        }

        setupPieces();
    }

    private void setupPieces() {
        // Peões
        for (int col = 0; col < 8; col++) {
            board[1][col].setText("♟"); // Peões pretos
            board[6][col].setText("♙"); // Peões brancos
        }

        // Torres, Cavalos, Bispos
        board[0][0].setText("♜");
        board[0][7].setText("♜");
        board[7][0].setText("♖");
        board[7][7].setText("♖");

        board[0][1].setText("♞");
        board[0][6].setText("♞");
        board[7][1].setText("♘");
        board[7][6].setText("♘");

        board[0][2].setText("♝");
        board[0][5].setText("♝");
        board[7][2].setText("♗");
        board[7][5].setText("♗");

        // Rainha e Rei
        board[0][3].setText("♛");
        board[0][4].setText("♚");
        board[7][3].setText("♕");
        board[7][4].setText("♔");
    }

    private class MoveListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton clickedButton = (JButton) e.getSource();
            String[] pos = clickedButton.getActionCommand().split(",");
            int row = Integer.parseInt(pos[0]);
            int col = Integer.parseInt(pos[1]);

            if (selectedPiece == null && !clickedButton.getText().isEmpty()) {
                // Selecionar peça
                if (isPlayerPiece(clickedButton.getText())) {
                    selectedPiece = clickedButton;
                    selectedRow = row;
                    selectedCol = col;
                    clickedButton.setBackground(Color.YELLOW); // Destacar peça selecionada
                }
            } else if (selectedPiece != null) {
                // Tentar mover a peça
                if (isValidMove(selectedRow, selectedCol, row, col)) {
                    // Captura
                    if (!clickedButton.getText().isEmpty()) {
                        String capturedPiece = clickedButton.getText();
                        System.out.println("Peça capturada: " + capturedPiece);
                    }

                    // Mover a peça
                    clickedButton.setText(selectedPiece.getText());
                    selectedPiece.setText("");
                    hasMoved[selectedRow][selectedCol] = true;

                    resetBoardColors();
                    switchPlayer();
                } else {
                    JOptionPane.showMessageDialog(null, "Movimento inválido!", "Erro", JOptionPane.ERROR_MESSAGE);
                }
                selectedPiece = null;
            }
        }

        private void switchPlayer() {
            currentPlayer = currentPlayer.equals("White") ? "Black" : "White";
            statusLabel.setText("Vez do jogador: " + currentPlayer);
        }

        private boolean isPlayerPiece(String piece) {
            return (currentPlayer.equals("White") && "♙♖♘♗♕♔".contains(piece)) ||
                   (currentPlayer.equals("Black") && "♟♜♞♝♛♚".contains(piece));
        }

        private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
            String piece = board[fromRow][fromCol].getText();

            switch (piece) {
                case "♙": // Peão branco
                    return (toRow == fromRow - 1 && toCol == fromCol && board[toRow][toCol].getText().isEmpty()) || // Movimento simples
                           (toRow == fromRow - 1 && Math.abs(toCol - fromCol) == 1 && !board[toRow][toCol].getText().isEmpty() && isBlackPiece(board[toRow][toCol].getText())) || // Captura
                           (toRow == fromRow - 2 && fromRow == 6 && toCol == fromCol && board[toRow][toCol].getText().isEmpty()); // Movimento duplo
                case "♟": // Peão preto
                    return (toRow == fromRow + 1 && toCol == fromCol && board[toRow][toCol].getText().isEmpty()) || // Movimento simples
                           (toRow == fromRow + 1 && Math.abs(toCol - fromCol) == 1 && !board[toRow][toCol].getText().isEmpty() && isWhitePiece(board[toRow][toCol].getText())) || // Captura
                           (toRow == fromRow + 2 && fromRow == 1 && toCol == fromCol && board[toRow][toCol].getText().isEmpty()); // Movimento duplo
                case "♖": // Torre
                case "♜": // Torre
                    return isValidRookMove(fromRow, fromCol, toRow, toCol);
                case "♗": // Bispo
                case "♝": // Bispo
                    return isValidBishopMove(fromRow, fromCol, toRow, toCol);
                case "♘": // Cavalo
                case "♞": // Cavalo
                    return isValidKnightMove(fromRow, fromCol, toRow, toCol);
                case "♕": // Rainha
                case "♛": // Rainha
                    return isValidQueenMove(fromRow, fromCol, toRow, toCol);
                case "♔": // Rei
                case "♚": // Rei
                    return isValidKingMove(fromRow, fromCol, toRow, toCol);
                default:
                    return false;
            }
        }

        private boolean isValidRookMove(int fromRow, int fromCol, int toRow, int toCol) {
            if (fromRow != toRow && fromCol != toCol) return false; // Movimento não é reto
            return isPathClear(fromRow, fromCol, toRow, toCol);
        }

        private boolean isValidBishopMove(int fromRow, int fromCol, int toRow, int toCol) {
            if (Math.abs(fromRow - toRow) != Math.abs(fromCol - toCol)) return false; // Movimento não é diagonal
            return isPathClear(fromRow, fromCol, toRow, toCol);
        }

        private boolean isValidKnightMove(int fromRow, int fromCol, int toRow, int toCol) {
            return (Math.abs(fromRow - toRow) == 2 && Math.abs(fromCol - toCol) == 1) ||
                   (Math.abs(fromRow - toRow) == 1 && Math.abs(fromCol - toCol) == 2);
        }

        private boolean isValidQueenMove(int fromRow, int fromCol, int toRow, int toCol) {
            return isValidRookMove(fromRow, fromCol, toRow, toCol) || isValidBishopMove(fromRow, fromCol, toRow, toCol);
        }

        private boolean isValidKingMove(int fromRow, int fromCol, int toRow, int toCol) {
            return Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1;
        }

        private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
            int rowDirection = Integer.compare(toRow, fromRow);
            int colDirection = Integer.compare(toCol, fromCol);

            int r = fromRow + rowDirection;
            int c = fromCol + colDirection;

            while (r != toRow || c != toCol) {
                if (!board[r][c].getText().isEmpty()) {
                    return false; // Há uma peça no caminho
                }
                r += rowDirection;
                c += colDirection;
            }
            return true; // Caminho livre
        }

        private boolean isBlackPiece(String piece) {
            return "♟♜♞♝♛♚".contains(piece);
        }

        private boolean isWhitePiece(String piece) {
            return "♙♖♘♗♕♔".contains(piece);
        }

        private void resetBoardColors() {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    board[row][col].setBackground((row + col) % 2 == 0 ? Color.WHITE : Color.GRAY);
                }
            }
        }
    }
}
