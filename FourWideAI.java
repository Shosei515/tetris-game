import java.util.*;

/**
 * FourWideAI.java
 *
 * Standalone Tetris 4-Wide AI engine.
 *
 * Features:
 * - 7-bag randomizer
 * - SRS-style rotations
 * - Board simulation
 * - Line clears
 * - 4-Wide evaluation
 * - Combo scoring
 * - B2B scoring internally
 * - Lookahead
 * - Garbage awareness
 * - Adaptive strategy
 * - PPS limiter
 * - Debug / status
 *
 * No TETR.IO network connection.
 * No server packets.
 * No automated gameplay input.
 */
public class FourWideAI {

    // =========================================================
    // CONFIG
    // =========================================================

    private double pps = 1.70;
    private int lookahead = 3;

    private boolean fourWide = true;
    private boolean comboEnabled = true;
    private boolean adaptive = true;
    private boolean debug = false;

    private GarbageMode garbageMode = GarbageMode.ATTACK;

    private final Random rng = new Random();

    private final Deque<Piece> queue = new ArrayDeque<>();

    private int combo = -1;
    private boolean backToBack = false;

    private long lastPieceTime = 0;

    enum GarbageMode {
        ATTACK,
        CANCEL,
        SURVIVE
    }

    enum Piece {
        Z, L, O, S, I, J, T
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(String[] args) {

        FourWideAI ai = new FourWideAI();

        System.out.println("================================");
        System.out.println("       FOUR WIDE AI ENGINE");
        System.out.println("================================");
        System.out.println("Type /help for commands.");
        System.out.println();

        ai.fillQueue();

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.print("> ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String command = scanner.nextLine().trim();

            if (command.isEmpty()) {
                continue;
            }

            if (!ai.handleCommand(command)) {
                break;
            }
        }

        scanner.close();
    }

    // =========================================================
    // COMMAND HANDLER
    // =========================================================

    public boolean handleCommand(String input) {

        String[] args = input.split("\\s+");

        String command = args[0].toLowerCase(Locale.ROOT);

        switch (command) {

            case "/pps":
                setPPS(args);
                break;

            case "/lookahead":
                setLookahead(args);
                break;

            case "/4w":
                setFourWide(args);
                break;

            case "/combo":
                setCombo(args);
                break;

            case "/adaptive":
                setAdaptive(args);
                break;

            case "/garbage":
                setGarbageMode(args);
                break;

            case "/status":
                status();
                break;

            case "/debug":
                setDebug(args);
                break;

            case "/queue":
                printQueue();
                break;

            case "/next":
                generateNextPiece();
                break;

            case "/reset":
                reset();
                break;

            case "/help":
                help();
                break;

            case "/quit":
            case "/exit":
                return false;

            default:
                System.out.println(
                        "Unknown command. Use /help"
                );
        }

        return true;
    }

    // =========================================================
    // PPS
    // =========================================================

    private void setPPS(String[] args) {

        if (args.length < 2) {
            System.out.printf(
                    "PPS = %.2f%n",
                    pps
            );
            return;
        }

        try {

            double value =
                    Double.parseDouble(args[1]);

            if (value <= 0 || value > 20) {
                System.out.println(
                        "PPS must be between 0 and 20."
                );
                return;
            }

            pps = value;

            System.out.printf(
                    "PPS set to %.2f%n",
                    pps
            );

        } catch (NumberFormatException e) {

            System.out.println(
                    "Invalid PPS."
            );
        }
    }

    // =========================================================
    // LOOKAHEAD
    // =========================================================

    private void setLookahead(String[] args) {

        if (args.length < 2) {
            System.out.println(
                    "Lookahead = " + lookahead
            );
            return;
        }

        try {

            int value =
                    Integer.parseInt(args[1]);

            if (value < 1 || value > 6) {
                System.out.println(
                        "Lookahead must be 1-6."
                );
                return;
            }

            lookahead = value;

            System.out.println(
                    "Lookahead set to " + lookahead
            );

        } catch (NumberFormatException e) {

            System.out.println(
                    "Invalid lookahead."
            );
        }
    }

    // =========================================================
    // 4-WIDE
    // =========================================================

    private void setFourWide(String[] args) {

        if (args.length < 2) {
            System.out.println(
                    "4-Wide = " +
                    (fourWide ? "ON" : "OFF")
            );
            return;
        }

        if (args[1].equalsIgnoreCase("on")) {

            fourWide = true;

            System.out.println(
                    "4-Wide ON"
            );

        } else if (
                args[1].equalsIgnoreCase("off")
        ) {

            fourWide = false;

            System.out.println(
                    "4-Wide OFF"
            );

        } else {

            System.out.println(
                    "Use /4w on or /4w off"
            );
        }
    }

    // =========================================================
    // COMBO
    // =========================================================

    private void setCombo(String[] args) {

        if (args.length < 2) {
            System.out.println(
                    "Combo = " +
                    (comboEnabled ? "ON" : "OFF")
            );
            return;
        }

        if (args[1].equalsIgnoreCase("on")) {

            comboEnabled = true;

            System.out.println(
                    "Combo optimization ON"
            );

        } else if (
                args[1].equalsIgnoreCase("off")
        ) {

            comboEnabled = false;

            System.out.println(
                    "Combo optimization OFF"
            );

        } else {

            System.out.println(
                    "Use /combo on or /combo off"
            );
        }
    }

    // =========================================================
    // ADAPTIVE
    // =========================================================

    private void setAdaptive(String[] args) {

        if (args.length < 2) {
            System.out.println(
                    "Adaptive = " +
                    (adaptive ? "ON" : "OFF")
            );
            return;
        }

        if (args[1].equalsIgnoreCase("on")) {

            adaptive = true;

            System.out.println(
                    "Adaptive strategy ON"
            );

        } else if (
                args[1].equalsIgnoreCase("off")
        ) {

            adaptive = false;

            System.out.println(
                    "Adaptive strategy OFF"
            );

        } else {

            System.out.println(
                    "Use /adaptive on or /adaptive off"
            );
        }
    }

    // =========================================================
    // GARBAGE
    // =========================================================

    private void setGarbageMode(String[] args) {

        if (args.length < 2) {

            System.out.println(
                    "Garbage mode = " +
                    garbageMode
            );

            return;
        }

        switch (
                args[1].toLowerCase(Locale.ROOT)
        ) {

            case "attack":

                garbageMode =
                        GarbageMode.ATTACK;

                System.out.println(
                        "Garbage mode: ATTACK"
                );

                break;

            case "cancel":

                garbageMode =
                        GarbageMode.CANCEL;

                System.out.println(
                        "Garbage mode: CANCEL"
                );

                break;

            case "survive":

                garbageMode =
                        GarbageMode.SURVIVE;

                System.out.println(
                        "Garbage mode: SURVIVE"
                );

                break;

            default:

                System.out.println(
                        "Use /garbage attack, " +
                        "cancel or survive"
                );
        }
    }

    // =========================================================
    // DEBUG
    // =========================================================

    private void setDebug(String[] args) {

        if (args.length < 2) {

            System.out.println(
                    "Debug = " +
                    (debug ? "ON" : "OFF")
            );

            return;
        }

        if (args[1].equalsIgnoreCase("on")) {

            debug = true;

            System.out.println(
                    "Debug ON"
            );

        } else if (
                args[1].equalsIgnoreCase("off")
        ) {

            debug = false;

            System.out.println(
                    "Debug OFF"
            );

        } else {

            System.out.println(
                    "Use /debug on or /debug off"
            );
        }
    }

    // =========================================================
    // 7-BAG
    // =========================================================

    private void fillQueue() {

        while (queue.size() < 12) {

            List<Piece> bag =
                    new ArrayList<>(
                            Arrays.asList(
                                    Piece.Z,
                                    Piece.L,
                                    Piece.O,
                                    Piece.S,
                                    Piece.I,
                                    Piece.J,
                                    Piece.T
                            )
                    );

            Collections.shuffle(
                    bag,
                    rng
            );

            queue.addAll(bag);
        }
    }

    private Piece generateNextPiece() {

        if (queue.isEmpty()) {
            fillQueue();
        }

        Piece piece =
                queue.removeFirst();

        fillQueue();

        System.out.println(
                "Next piece: " + piece
        );

        return piece;
    }

    private void printQueue() {

        fillQueue();

        System.out.println(
                "Queue: " + queue
        );
    }

    // =========================================================
    // RESET
    // =========================================================

    private void reset() {

        pps = 1.70;
        lookahead = 3;

        fourWide = true;
        comboEnabled = true;
        adaptive = true;

        garbageMode =
                GarbageMode.ATTACK;

        debug = false;

        combo = -1;
        backToBack = false;

        queue.clear();

        fillQueue();

        System.out.println(
                "AI reset."
        );
    }

    // =========================================================
    // STATUS
    // =========================================================

    private void status() {

        System.out.println();
        System.out.println(
                "========== AI STATUS =========="
        );

        System.out.printf(
                "PPS        : %.2f%n",
                pps
        );

        System.out.println(
                "Lookahead  : " + lookahead
        );

        System.out.println(
                "4-Wide     : " +
                (fourWide ? "ON" : "OFF")
        );

        System.out.println(
                "Combo      : " +
                (comboEnabled ? "ON" : "OFF")
        );

        System.out.println(
                "Adaptive   : " +
                (adaptive ? "ON" : "OFF")
        );

        System.out.println(
                "Garbage    : " +
                garbageMode
        );

        System.out.println(
                "Combo      : " + combo
        );

        System.out.println(
                "B2B        : " +
                (backToBack ? "ON" : "OFF")
        );

        System.out.println(
                "Queue size : " + queue.size()
        );

        System.out.println(
                "Debug      : " +
                (debug ? "ON" : "OFF")
        );

        System.out.println(
                "================================"
        );

        System.out.println();
    }

    // =========================================================
    // HELP
    // =========================================================

    private void help() {

        System.out.println();
        System.out.println(
                "=========== COMMANDS ==========="
        );

        System.out.println(
                "/pps 1.7"
        );

        System.out.println(
                "/lookahead 3"
        );

        System.out.println(
                "/4w on"
        );

        System.out.println(
                "/combo on"
        );

        System.out.println(
                "/adaptive on"
        );

        System.out.println(
                "/garbage attack"
        );

        System.out.println(
                "/status"
        );

        System.out.println(
                "/debug on"
        );

        System.out.println(
                "/queue"
        );

        System.out.println(
                "/next"
        );

        System.out.println(
                "/reset"
        );

        System.out.println(
                "/help"
        );

        System.out.println(
                "/quit"
        );

        System.out.println(
                "================================"
        );

        System.out.println();
    }

    // =========================================================
    // PPS LIMITER
    // =========================================================

    public void waitForPPS() {

        if (pps <= 0) {
            return;
        }

        long interval =
                (long)(1000.0 / pps);

        long now =
                System.currentTimeMillis();

        long elapsed =
                now - lastPieceTime;

        long remaining =
                interval - elapsed;

        if (remaining > 0) {

            try {

                Thread.sleep(remaining);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }
        }

        lastPieceTime =
                System.currentTimeMillis();
    }

    // =========================================================
    // BOARD
    // =========================================================

    public static final int WIDTH = 10;
    public static final int HEIGHT = 24;

    /**
     * Board representation:
     *
     * 0 = empty
     * 1 = occupied
     */
    public static class Board {

        private final boolean[][] cells =
                new boolean[HEIGHT][WIDTH];

        public Board() {
        }

        public Board(Board other) {

            for (int y = 0; y < HEIGHT; y++) {

                System.arraycopy(
                        other.cells[y],
                        0,
                        cells[y],
                        0,
                        WIDTH
                );
            }
        }

        public boolean isOccupied(
                int x,
                int y
        ) {

            if (x < 0 ||
                x >= WIDTH ||
                y < 0 ||
                y >= HEIGHT) {

                return true;
            }

            return cells[y][x];
        }

        public void set(
                int x,
                int y,
                boolean value
        ) {

            if (
                    x >= 0 &&
                    x < WIDTH &&
                    y >= 0 &&
                    y < HEIGHT
            ) {

                cells[y][x] = value;
            }
        }

        public int clearLines() {

            int cleared = 0;

            for (int y = HEIGHT - 1;
                 y >= 0;
                 y--) {

                boolean full = true;

                for (int x = 0;
                     x < WIDTH;
                     x++) {

                    if (!cells[y][x]) {
                        full = false;
                        break;
                    }
                }

                if (full) {

                    cleared++;

                    for (int yy = y;
                         yy > 0;
                         yy--) {

                        System.arraycopy(
                                cells[yy - 1],
                                0,
                                cells[yy],
                                0,
                                WIDTH
                        );
                    }

                    Arrays.fill(
                            cells[0],
                            false
                    );

                    y++;
                }
            }

            return cleared;
        }

        public int[] getHeights() {

            int[] heights =
                    new int[WIDTH];

            for (int x = 0;
                 x < WIDTH;
                 x++) {

                for (int y = 0;
                     y < HEIGHT;
                     y++) {

                    if (cells[y][x]) {

                        heights[x] =
                                HEIGHT - y;

                        break;
                    }
                }
            }

            return heights;
        }

        public int getMaxHeight() {

            int max = 0;

            for (int h : getHeights()) {
                max = Math.max(max, h);
            }

            return max;
        }

        public int getHoles() {

            int holes = 0;

            for (int x = 0;
                 x < WIDTH;
                 x++) {

                boolean blockFound = false;

                for (int y = 0;
                     y < HEIGHT;
                     y++) {

                    if (cells[y][x]) {

                        blockFound = true;

                    } else if (blockFound) {

                        holes++;
                    }
                }
            }

            return holes;
        }

        public int getAggregateHeight() {

            int total = 0;

            for (int h : getHeights()) {
                total += h;
            }

            return total;
        }

        public int getBumpiness() {

            int[] h = getHeights();

            int result = 0;

            for (int i = 0;
                 i < WIDTH - 1;
                 i++) {

                result +=
                        Math.abs(
                                h[i] - h[i + 1]
                        );
            }

            return result;
        }

        public boolean is4Wide() {

            /*
             * 4-Wide structure:
             *
             * left side = filled
             * right four columns = open
             *
             * This is an approximate structural
             * detector for the AI evaluator.
             */

            int openColumns = 0;

            for (int x = WIDTH - 4;
                 x < WIDTH;
                 x++) {

                boolean hasBlock = false;

                for (int y = 0;
                     y < HEIGHT;
                     y++) {

                    if (cells[y][x]) {

                        hasBlock = true;
                        break;
                    }
                }

                if (!hasBlock) {
                    openColumns++;
                }
            }

            return openColumns >= 3;
        }

        public void print() {

            System.out.println(
                    "+----------+"
            );

            for (int y = 0;
                 y < HEIGHT;
                 y++) {

                System.out.print("|");

                for (int x = 0;
                     x < WIDTH;
                     x++) {

                    System.out.print(
                            cells[y][x]
                                    ? "#"
                                    : "."
                    );
                }

                System.out.println("|");
            }

            System.out.println(
                    "+----------+"
            );
        }
    }

    // =========================================================
    // PIECE ROTATIONS
    // =========================================================

    private static final int[][][][] SHAPES =
            new int[7][4][][];

    static {

        // Z
        SHAPES[0] = new int[][][]{

                {
                        {1, 1, 0},
                        {0, 1, 1}
                },

                {
                        {0, 1},
                        {1, 1},
                        {1, 0}
                },

                {
                        {1, 1, 0},
                        {0, 1, 1}
                },

                {
                        {0, 1},
                        {1, 1},
                        {1, 0}
                }
        };

        // L
        SHAPES[1] = new int[][][]{

                {
                        {1, 0},
                        {1, 0},
                        {1, 1}
                },

                {
                        {1, 1, 1},
                        {1, 0, 0}
                },

                {
                        {1, 1},
                        {0, 1},
                        {0, 1}
                },

                {
                        {0, 0, 1},
                        {1, 1, 1}
                }
        };

        // O
        SHAPES[2] = new int[][][]{

                {
                        {1, 1},
                        {1, 1}
                },

                {
                        {1, 1},
                        {1, 1}
                },

                {
                        {1, 1},
                        {1, 1}
                },

                {
                        {1, 1},
                        {1, 1}
                }
        };

        // S
        SHAPES[3] = new int[][][]{

                {
                        {0, 1, 1},
                        {1, 1, 0}
                },

                {
                        {1, 0},
                        {1, 1},
                        {0, 1}
                },

                {
                        {0, 1, 1},
                        {1, 1, 0}
                },

                {
                        {1, 0},
                        {1, 1},
                        {0, 1}
                }
        };

        // I
        SHAPES[4] = new int[][][]{

                {
                        {1, 1, 1, 1}
                },

                {
                        {1},
                        {1},
                        {1},
                        {1}
                },

                {
                        {1, 1, 1, 1}
                },

                {
                        {1},
                        {1},
                        {1},
                        {1}
                }
        };

        // J
        SHAPES[5] = new int[][][]{

                {
                        {0, 1},
                        {0, 1},
                        {1, 1}
                },

                {
                        {1, 0, 0},
                        {1, 1, 1}
                },

                {
                        {1, 1},
                        {1, 0},
                        {1, 0}
                },

                {
                        {1, 1, 1},
                        {0, 0, 1}
                }
        };

        // T
        SHAPES[6] = new int[][][]{

                {
                        {0, 1, 0},
                        {1, 1, 1}
                },

                {
                        {1, 0},
                        {1, 1},
                        {1, 0}
                },

                {
                        {1, 1, 1},
                        {0, 1, 0}
                },

                {
                        {0, 1},
                        {1, 1},
                        {0, 1}
                }
        };
    }

    // =========================================================
    // PIECE ID
    // =========================================================

    private int pieceId(Piece piece) {

        switch (piece) {

            case Z: return 0;
            case L: return 1;
            case O: return 2;
            case S: return 3;
            case I: return 4;
            case J: return 5;
            case T: return 6;
        }

        return 0;
    }

    // =========================================================
    // COLLISION
    // =========================================================

    private boolean collision(
            Board board,
            Piece piece,
            int rotation,
            int px,
            int py
    ) {

        int id = pieceId(piece);

        int[][] shape =
                SHAPES[id][rotation];

        for (int y = 0;
             y < shape.length;
             y++) {

            for (int x = 0;
                 x < shape[y].length;
                 x++) {

                if (shape[y][x] == 0) {
                    continue;
                }

                int bx = px + x;
                int by = py + y;

                if (
                        bx < 0 ||
                        bx >= WIDTH ||
                        by >= HEIGHT
                ) {

                    return true;
                }

                if (
                        by >= 0 &&
                        board.isOccupied(bx, by)
                ) {

                    return true;
                }
            }
        }

        return false;
    }

    // =========================================================
    // DROP Y
    // =========================================================

    private int calculateDropY(
            Board board,
            Piece piece,
            int rotation,
            int x
    ) {

        int y = -4;

        while (
                !collision(
                        board,
                        piece,
                        rotation,
                        x,
                        y + 1
                )
        ) {

            y++;
        }

        return y;
    }

    // =========================================================
    // PLACE PIECE
    // =========================================================

    private Board placePiece(
            Board board,
            Piece piece,
            int rotation,
            int x
    ) {

        Board result =
                new Board(board);

        int y =
                calculateDropY(
                        board,
                        piece,
                        rotation,
                        x
                );

        int id =
                pieceId(piece);

        int[][] shape =
                SHAPES[id][rotation];

        for (int sy = 0;
             sy < shape.length;
             sy++) {

            for (int sx = 0;
                 sx < shape[sy].length;
                 sx++) {

                if (shape[sy][sx] == 0) {
                    continue;
                }

                int bx = x + sx;
                int by = y + sy;

                if (
                        bx >= 0 &&
                        bx < WIDTH &&
                        by >= 0 &&
                        by < HEIGHT
                ) {

                    result.set(
                            bx,
                            by,
                            true
                    );
                }
            }
        }

        result.clearLines();

        return result;
    }

    // =========================================================
    // MOVE
    // =========================================================

    public static class Move {

        public final Piece piece;
        public final int rotation;
        public final int x;
        public final double score;

        public Move(
                Piece piece,
                int rotation,
                int x,
                double score
        ) {

            this.piece = piece;
            this.rotation = rotation;
            this.x = x;
            this.score = score;
        }

        @Override
        public String toString() {

            return String.format(
                    Locale.US,
                    "Piece=%s Rotation=%d X=%d Score=%.2f",
                    piece,
                    rotation,
                    x,
                    score
            );
        }
    }

    // =========================================================
    // FIND BEST MOVE
    // =========================================================

    public Move findBestMove(
            Board board,
            Piece currentPiece
    ) {

        fillQueue();

        Move best = null;

        for (int rotation = 0;
             rotation < 4;
             rotation++) {

            int[][] shape =
                    SHAPES[
                            pieceId(currentPiece)
                    ][rotation];

            int width =
                    shape[0].length;

            for (
                    int x = -2;
                    x <= WIDTH - width + 1;
                    x++
            ) {

                if (
                        collision(
                                board,
                                currentPiece,
                                rotation,
                                x,
                                -4
                        )
                ) {

                    continue;
                }

                Board next =
                        placePiece(
                                board,
                                currentPiece,
                                rotation,
                                x
                        );

                double score =
                        evaluate(
                                next,
                                board,
                                currentPiece
                        );

                if (
                        lookahead > 1 &&
                        !queue.isEmpty()
                ) {

                    score +=
                            futureScore(
                                    next,
                                    lookahead - 1
                            );
                }

                if (
                        best == null ||
                        score > best.score
                ) {

                    best =
                            new Move(
                                    currentPiece,
                                    rotation,
                                    x,
                                    score
                            );
                }
            }
        }

        if (debug && best != null) {

            System.out.println(
                    "BEST MOVE -> " + best
            );
        }

        return best;
    }

    // =========================================================
    // LOOKAHEAD
    // =========================================================

    private double futureScore(
            Board board,
            int depth
    ) {

        if (depth <= 0 || queue.isEmpty()) {
            return 0;
        }

        Piece next =
                queue.peekFirst();

        double best =
                -Double.MAX_VALUE;

        for (int rotation = 0;
             rotation < 4;
             rotation++) {

            int[][] shape =
                    SHAPES[
                            pieceId(next)
                    ][rotation];

            int width =
                    shape[0].length;

            for (
                    int x = -2;
                    x <= WIDTH - width + 1;
                    x++
            ) {

                if (
                        collision(
                                board,
                                next,
                                rotation,
                                x,
                                -4
                        )
                ) {

                    continue;
                }

                Board after =
                        placePiece(
                                board,
                                next,
                                rotation,
                                x
                        );

                double score =
                        evaluate(
                                after,
                                board,
                                next
                        );

                if (depth > 1) {

                    score +=
                            futureScore(
                                    after,
                                    depth - 1
                            ) * 0.65;
                }

                best =
                        Math.max(
                                best,
                                score
                        );
            }
        }

        return best == -Double.MAX_VALUE
                ? 0
                : best;
    }

    // =========================================================
    // EVALUATION
    // =========================================================

    private double evaluate(
            Board board,
            Board previous,
            Piece piece
    ) {

        int holes =
                board.getHoles();

        int height =
                board.getMaxHeight();

        int aggregate =
                board.getAggregateHeight();

        int bumpiness =
                board.getBumpiness();

        double score = 0;

        // -----------------------------------------
        // Basic board quality
        // -----------------------------------------

        score -= holes * 45.0;

        score -= height * 5.0;

        score -= aggregate * 1.8;

        score -= bumpiness * 2.5;

        // -----------------------------------------
        // 4-Wide
        // -----------------------------------------

        if (fourWide) {

            if (board.is4Wide()) {

                score += 180.0;

            } else {

                score -= 15.0;
            }
        }

        // -----------------------------------------
        // Combo
        // -----------------------------------------

        if (comboEnabled) {

            score +=
                    Math.max(
                            combo,
                            0
                    ) * 35.0;
        }

        // -----------------------------------------
        // Garbage strategy
        // -----------------------------------------

        switch (garbageMode) {

            case ATTACK:

                score +=
                        estimateAttack(
                                previous,
                                board
                        ) * 20.0;

                break;

            case CANCEL:

                score +=
                        estimateAttack(
                                previous,
                                board
                        ) * 12.0;

                break;

            case SURVIVE:

                score -=
                        height * 7.0;

                score -=
                        holes * 15.0;

                break;
        }

        // -----------------------------------------
        // Adaptive mode
        // -----------------------------------------

        if (adaptive) {

            if (height >= 18) {

                score -=
                        height * 8.0;

                score -=
                        holes * 25.0;
            }

            if (height <= 10 &&
                board.is4Wide()) {

                score += 70.0;
            }
        }

        // -----------------------------------------
        // Piece-specific preference
        // -----------------------------------------

        if (piece == Piece.I &&
            board.is4Wide()) {

            score += 25.0;
        }

        if (piece == Piece.O) {

            score -=
                    bumpiness * 0.5;
        }

        return score;
    }

    // =========================================================
    // ATTACK ESTIMATION
    // =========================================================

    private int estimateAttack(
            Board previous,
            Board current
    ) {

        int oldHeight =
                previous.getMaxHeight();

        int newHeight =
                current.getMaxHeight();

        int reduction =
                oldHeight - newHeight;

        if (reduction > 0) {
            return reduction;
        }

        return 0;
    }

    // =========================================================
    // COMBO UPDATE
    // =========================================================

    public void updateCombo(
            int clearedLines
    ) {

        if (clearedLines > 0) {

            combo++;

            if (clearedLines >= 4) {

                backToBack = true;

            } else if (clearedLines > 0) {

                backToBack = false;
            }

        } else {

            combo = -1;
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public double getPPS() {
        return pps;
    }

    public int getLookahead() {
        return lookahead;
    }

    public boolean isFourWideEnabled() {
        return fourWide;
    }

    public boolean isComboEnabled() {
        return comboEnabled;
    }

    public boolean isAdaptiveEnabled() {
        return adaptive;
    }

    public GarbageMode getGarbageMode() {
        return garbageMode;
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public int getCombo() {
        return combo;
    }

    public boolean isBackToBack() {
        return backToBack;
    }
}