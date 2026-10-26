import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * ThreadSync - Thread Synchronization and Thread Priority Simulator
 * Demonstrates Java multithreading, synchronization, and thread priority concepts.
 */
public class ThreadSyncApp extends JFrame {

    // ─── Shared Resources ────────────────────────────────────────────────────
    private final SharedCounter syncCounter   = new SharedCounter();
    private final SharedCounter unsyncCounter = new SharedCounter();

    // ─── Thread Management ───────────────────────────────────────────────────
    private final List<WorkerThread> activeWorkers = new ArrayList<>();
    private volatile boolean simulationRunning = false;
    private ExecutorService executor;

    // ─── Statistics ──────────────────────────────────────────────────────────
    private final AtomicInteger completedTasks = new AtomicInteger(0);
    private final AtomicInteger pendingTasks   = new AtomicInteger(0);

    // ─── UI Components ───────────────────────────────────────────────────────
    // Stats cards
    private JLabel lblCompleted, lblPending, lblWorkers, lblSyncStatus;

    // Worker table
    private DefaultTableModel workerTableModel;
    private JTable workerTable;

    // Controls
    private JSpinner workerCountSpinner;
    private JSpinner taskCountSpinner;
    private JButton btnStartSync, btnStartUnsync, btnStop, btnReset;

    // Counter displays
    private JLabel lblSyncCounterVal, lblUnsyncCounterVal, lblExpectedVal;

    // Progress
    private JProgressBar progressBar;

    // Log
    private JTextArea logArea;

    // Colors
    private static final Color BG_DARK      = new Color(13,  17,  37);
    private static final Color BG_CARD      = new Color(22,  30,  58);
    private static final Color BG_TABLE     = new Color(18,  24,  48);
    private static final Color ACCENT_BLUE  = new Color(64, 156, 255);
    private static final Color ACCENT_VIOLET= new Color(139, 92, 246);
    private static final Color ACCENT_GREEN = new Color(52, 211, 153);
    private static final Color ACCENT_RED   = new Color(248, 113, 113);
    private static final Color ACCENT_AMBER = new Color(251, 191, 36);
    private static final Color TEXT_PRIMARY = new Color(226, 232, 240);
    private static final Color TEXT_MUTED   = new Color(100, 116, 139);

    // ─── Constructor ─────────────────────────────────────────────────────────
    public ThreadSyncApp() {
        super("ThreadSync — Thread Synchronization & Priority Simulator");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                stopSimulation();
                System.exit(0);
            }
        });
        setSize(1200, 820);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        buildUI();
        log("ThreadSync ready. Configure workers and click Start.");
        log("TIP: Thread priority is a scheduling HINT — not a guarantee of order.");
        updateStatsCards();
    }

    // ─── UI Builder ──────────────────────────────────────────────────────────
    private void buildUI() {
        add(buildHeader(),      BorderLayout.NORTH);
        add(buildCenter(),      BorderLayout.CENTER);
        add(buildStatusBar(),   BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, ACCENT_BLUE));
        header.setPreferredSize(new Dimension(0, 70));

        JLabel title = styledLabel("⚡ ThreadSync", 22, Font.BOLD, ACCENT_BLUE);
        title.setBorder(new EmptyBorder(0, 20, 0, 0));

        JLabel sub = styledLabel("Thread Synchronization & Priority Simulator  |  Java Multithreading Demo", 12, Font.PLAIN, TEXT_MUTED);
        sub.setBorder(new EmptyBorder(0, 20, 0, 0));

        JPanel left = new JPanel(new GridLayout(2, 1));
        left.setOpaque(false);
        left.add(title); left.add(sub);
        header.add(left, BorderLayout.CENTER);

        JLabel hint = styledLabel("Viva Ready  ✓", 11, Font.PLAIN, ACCENT_GREEN);
        hint.setBorder(new EmptyBorder(0, 0, 0, 20));
        hint.setHorizontalAlignment(SwingConstants.RIGHT);
        header.add(hint, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(BG_DARK);
        center.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Left column: controls + stats + counter
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(BG_DARK);
        left.setPreferredSize(new Dimension(320, 0));

        left.add(buildStatsCards());
        left.add(Box.createVerticalStrut(10));
        left.add(buildControls());
        left.add(Box.createVerticalStrut(10));
        left.add(buildCounterPanel());
        left.add(Box.createVerticalGlue());

        // Right column: table + log
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setBackground(BG_DARK);

        right.add(buildWorkerTable(), BorderLayout.CENTER);
        right.add(buildLogPanel(),    BorderLayout.SOUTH);

        center.add(left,  BorderLayout.WEST);
        center.add(right, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildStatsCards() {
        JPanel panel = card("📊 Live Statistics");
        panel.setLayout(new GridLayout(2, 2, 8, 8));

        lblCompleted = statLabel("0", "Completed", ACCENT_GREEN);
        lblPending   = statLabel("0", "Pending",   ACCENT_AMBER);
        lblWorkers   = statLabel("0", "Workers",   ACCENT_BLUE);
        lblSyncStatus= statLabel("—", "Status",    ACCENT_VIOLET);

        panel.add(statCard(lblCompleted, "Completed", ACCENT_GREEN));
        panel.add(statCard(lblPending,   "Pending",   ACCENT_AMBER));
        panel.add(statCard(lblWorkers,   "Workers",   ACCENT_BLUE));
        panel.add(statCard(lblSyncStatus,"Status",    ACCENT_VIOLET));
        return panel;
    }

    private JPanel buildControls() {
        JPanel panel = card("⚙ Configuration & Controls");
        panel.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill   = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0; g.weightx = 0.5;
        panel.add(label12("Worker Threads:"), g);
        g.gridx = 1;
        workerCountSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 10, 1));
        styleSpinner(workerCountSpinner);
        panel.add(workerCountSpinner, g);

        g.gridx = 0; g.gridy = 1;
        panel.add(label12("Tasks per Worker:"), g);
        g.gridx = 1;
        taskCountSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 200, 10));
        styleSpinner(taskCountSpinner);
        panel.add(taskCountSpinner, g);

        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        panel.add(new JSeparator(), g);

        g.gridy = 3; g.gridwidth = 2;
        btnStartSync = actionButton("▶ Start (Synchronized)", ACCENT_GREEN);
        btnStartSync.addActionListener(e -> startSimulation(true));
        panel.add(btnStartSync, g);

        g.gridy = 4;
        btnStartUnsync = actionButton("▶ Start (Unsynchronized)", ACCENT_AMBER);
        btnStartUnsync.addActionListener(e -> startSimulation(false));
        panel.add(btnStartUnsync, g);

        g.gridy = 5; g.gridwidth = 1; g.gridx = 0;
        btnStop = actionButton("■ Stop", ACCENT_RED);
        btnStop.setEnabled(false);
        btnStop.addActionListener(e -> stopSimulation());
        panel.add(btnStop, g);

        g.gridx = 1;
        btnReset = actionButton("↺ Reset", TEXT_MUTED);
        btnReset.addActionListener(e -> resetSimulation());
        panel.add(btnReset, g);

        g.gridx = 0; g.gridy = 6; g.gridwidth = 2;
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Idle");
        progressBar.setForeground(ACCENT_BLUE);
        progressBar.setBackground(BG_TABLE);
        progressBar.setBorderPainted(false);
        progressBar.setPreferredSize(new Dimension(0, 22));
        panel.add(progressBar, g);

        return panel;
    }

    private JPanel buildCounterPanel() {
        JPanel panel = card("🔢 Counter Demonstration");
        panel.setLayout(new GridLayout(4, 1, 4, 4));

        panel.add(label12("Synchronized Counter (expected = correct):"));
        lblSyncCounterVal = styledLabel("0", 28, Font.BOLD, ACCENT_GREEN);
        lblSyncCounterVal.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblSyncCounterVal);

        panel.add(label12("Unsynchronized Counter (may show race condition):"));
        lblUnsyncCounterVal = styledLabel("0", 28, Font.BOLD, ACCENT_RED);
        lblUnsyncCounterVal.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblUnsyncCounterVal);

        lblExpectedVal = styledLabel("Expected: —", 11, Font.ITALIC, TEXT_MUTED);
        lblExpectedVal.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblExpectedVal);

        return panel;
    }

    private JPanel buildWorkerTable() {
        JPanel panel = card("🧵 Worker Thread Monitor");
        panel.setLayout(new BorderLayout());

        String[] cols = {"Thread Name","ID","Priority","State","Tasks Done","Progress"};
        workerTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 5 ? Integer.class : String.class;
            }
        };

        workerTable = new JTable(workerTableModel);
        workerTable.setBackground(BG_TABLE);
        workerTable.setForeground(TEXT_PRIMARY);
        workerTable.setGridColor(new Color(30, 40, 70));
        workerTable.setRowHeight(30);
        workerTable.setSelectionBackground(new Color(40, 60, 100));
        workerTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        workerTable.getTableHeader().setBackground(BG_CARD);
        workerTable.getTableHeader().setForeground(ACCENT_BLUE);
        workerTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        // Priority column color renderer
        workerTable.getColumnModel().getColumn(2).setCellRenderer(new PriorityRenderer());
        // State column color renderer
        workerTable.getColumnModel().getColumn(3).setCellRenderer(new StateRenderer());
        // Progress bar renderer
        workerTable.getColumnModel().getColumn(5).setCellRenderer(new ProgressRenderer());

        JScrollPane scroll = new JScrollPane(workerTable);
        scroll.setBackground(BG_TABLE);
        scroll.getViewport().setBackground(BG_TABLE);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLogPanel() {
        JPanel panel = card("📋 Event Log");
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(0, 180));

        logArea = new JTextArea();
        logArea.setBackground(new Color(10, 14, 30));
        logArea.setForeground(ACCENT_GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setEditable(false);
        logArea.setLineWrap(true);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(logArea.getBackground());

        JButton btnClear = new JButton("Clear");
        btnClear.setBackground(BG_CARD);
        btnClear.setForeground(TEXT_MUTED);
        btnClear.setBorderPainted(false);
        btnClear.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnClear.addActionListener(e -> logArea.setText(""));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        top.setOpaque(false);
        top.add(btnClear);

        panel.add(top,    BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(30, 40, 70)));
        bar.add(styledLabel("Java " + System.getProperty("java.version"), 11, Font.PLAIN, TEXT_MUTED));
        bar.add(styledLabel("| Processors: " + Runtime.getRuntime().availableProcessors(), 11, Font.PLAIN, TEXT_MUTED));
        bar.add(styledLabel("| Priority range: " + Thread.MIN_PRIORITY + "–" + Thread.MAX_PRIORITY, 11, Font.PLAIN, TEXT_MUTED));
        bar.add(styledLabel("| Thread priority is a HINT, not a guarantee", 11, Font.ITALIC, ACCENT_AMBER));
        return bar;
    }

    // ─── Simulation Logic ────────────────────────────────────────────────────
    private void startSimulation(boolean synchronized_mode) {
        if (simulationRunning) return;
        simulationRunning = true;

        int workerCount = (int) workerCountSpinner.getValue();
        int taskCount   = (int) taskCountSpinner.getValue();
        int totalTasks  = workerCount * taskCount;

        completedTasks.set(0);
        pendingTasks.set(totalTasks);
        syncCounter.reset();
        unsyncCounter.reset();

        // UI state
        btnStartSync.setEnabled(false);
        btnStartUnsync.setEnabled(false);
        btnStop.setEnabled(true);
        progressBar.setValue(0);
        progressBar.setString("0 / " + totalTasks);
        lblSyncStatus.setText(synchronized_mode ? "SYNC" : "UNSYNC");
        lblSyncStatus.setForeground(synchronized_mode ? ACCENT_GREEN : ACCENT_RED);
        lblExpectedVal.setText("Expected: " + totalTasks);

        workerTableModel.setRowCount(0);
        activeWorkers.clear();

        executor = Executors.newFixedThreadPool(workerCount + 2);

        log("─── Starting " + (synchronized_mode ? "SYNCHRONIZED" : "UNSYNCHRONIZED")
                + " simulation: " + workerCount + " workers × " + taskCount + " tasks ───");

        for (int i = 0; i < workerCount; i++) {
            int priority = 1 + (i % 10);   // priorities 1–10 cycling
            WorkerThread wt = new WorkerThread(
                    "Worker-" + (i + 1), priority, taskCount,
                    synchronized_mode, totalTasks);
            activeWorkers.add(wt);
            wt.setDaemon(true);
            workerTableModel.addRow(new Object[]{
                    wt.getName(), "—", priority, "READY", "0", 0
            });
        }

        updateStatsCards();

        // Start all workers via executor
        for (WorkerThread wt : activeWorkers) {
            executor.submit(wt);
        }

        // Monitor thread
        executor.submit(this::monitorSimulation);
    }

    private void monitorSimulation() {
        while (simulationRunning) {
            boolean anyAlive = false;
            for (WorkerThread wt : activeWorkers) {
                if (!wt.isFinished()) { anyAlive = true; break; }
            }
            if (!anyAlive) {
                simulationRunning = false;
                SwingUtilities.invokeLater(this::onSimulationComplete);
                return;
            }
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }
    }

    private void stopSimulation() {
        simulationRunning = false;
        for (WorkerThread wt : activeWorkers) wt.cancel();
        if (executor != null) executor.shutdownNow();
        SwingUtilities.invokeLater(() -> {
            btnStartSync.setEnabled(true);
            btnStartUnsync.setEnabled(true);
            btnStop.setEnabled(false);
            lblSyncStatus.setText("STOPPED");
            lblSyncStatus.setForeground(ACCENT_RED);
            progressBar.setString("Stopped");
            log("■ Simulation stopped by user.");
        });
    }

    private void resetSimulation() {
        if (simulationRunning) stopSimulation();
        SwingUtilities.invokeLater(() -> {
            completedTasks.set(0);
            pendingTasks.set(0);
            syncCounter.reset(); unsyncCounter.reset();
            workerTableModel.setRowCount(0);
            progressBar.setValue(0); progressBar.setString("Idle");
            lblSyncCounterVal.setText("0");
            lblUnsyncCounterVal.setText("0");
            lblExpectedVal.setText("Expected: —");
            lblSyncStatus.setText("—"); lblSyncStatus.setForeground(ACCENT_VIOLET);
            updateStatsCards();
            log("↺ Reset complete.");
        });
    }

    private void onSimulationComplete() {
        btnStartSync.setEnabled(true);
        btnStartUnsync.setEnabled(true);
        btnStop.setEnabled(false);
        progressBar.setValue(100);
        progressBar.setString("Complete!");
        lblSyncStatus.setText("DONE");
        lblSyncStatus.setForeground(ACCENT_GREEN);

        int syncVal   = syncCounter.getValue();
        int unsyncVal = unsyncCounter.getValue();
        int expected  = completedTasks.get() + (int) pendingTasks.get();

        log("─── Simulation complete ───");
        log("Synchronized counter   : " + syncVal);
        log("Unsynchronized counter : " + unsyncVal);
        log("Expected value         : " + (int) taskCountSpinner.getValue() * activeWorkers.size());

        if (unsyncVal != syncVal)
            log("⚠ Race condition detected! Unsync = " + unsyncVal + " ≠ expected. This is normal.");
        else
            log("ℹ Unsync matched expected this run — race conditions are non-deterministic.");
    }

    // Called by workers to report progress
    void reportTaskDone(WorkerThread wt, int tasksDone, int totalForThisWorker,
                        boolean syncMode, long threadId) {
        completedTasks.incrementAndGet();
        pendingTasks.decrementAndGet();

        // Update both counters
        if (syncMode)  syncCounter.incrementSynchronized();
        else           unsyncCounter.incrementUnsynchronized();

        SwingUtilities.invokeLater(() -> {
            // Update table row
            for (int r = 0; r < workerTableModel.getRowCount(); r++) {
                if (workerTableModel.getValueAt(r, 0).equals(wt.getName())) {
                    workerTableModel.setValueAt(String.valueOf(threadId), r, 1);
                    workerTableModel.setValueAt(wt.isFinished() ? "DONE" : "RUNNING", r, 3);
                    workerTableModel.setValueAt(String.valueOf(tasksDone), r, 4);
                    int pct = (tasksDone * 100) / totalForThisWorker;
                    workerTableModel.setValueAt(pct, r, 5);
                    break;
                }
            }

            // Update counter labels
            lblSyncCounterVal.setText(String.valueOf(syncCounter.getValue()));
            lblUnsyncCounterVal.setText(String.valueOf(unsyncCounter.getValue()));

            // Update progress bar
            int total = (int) workerCountSpinner.getValue() * (int) taskCountSpinner.getValue();
            int done  = completedTasks.get();
            int pct   = total > 0 ? (done * 100 / total) : 0;
            progressBar.setValue(pct);
            progressBar.setString(done + " / " + total);

            updateStatsCards();
        });
    }

    private void updateStatsCards() {
        SwingUtilities.invokeLater(() -> {
            lblCompleted.setText(String.valueOf(completedTasks.get()));
            lblPending.setText(String.valueOf(Math.max(0, pendingTasks.get())));
            lblWorkers.setText(String.valueOf(activeWorkers.size()));
        });
    }

    void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            String ts = String.format("[%tT] ", new Date());
            logArea.append(ts + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    // ─── Shared Counter ──────────────────────────────────────────────────────
    static class SharedCounter {
        private int count = 0;

        /** Thread-safe increment using synchronized method. */
        synchronized void incrementSynchronized() {
            count++;
        }

        /** Deliberately unsynchronized — may suffer lost updates (race condition). */
        void incrementUnsynchronized() {
            // Read-modify-write is NOT atomic here. Between reading count and
            // writing count+1, another thread may also read the old value.
            int tmp = count;
            // Tiny sleep makes race condition more visible
            try { Thread.sleep(0, 1); } catch (InterruptedException ignored) {}
            count = tmp + 1;
        }

        synchronized int getValue() { return count; }
        synchronized void reset()   { count = 0; }
    }

    // ─── Worker Thread ───────────────────────────────────────────────────────
    class WorkerThread extends Thread {
        private final int taskCount;
        private final boolean syncMode;
        private final int totalSimTasks;
        private volatile boolean cancelled = false;
        private volatile boolean finished  = false;
        private int tasksDone = 0;

        WorkerThread(String name, int priority, int taskCount, boolean syncMode, int totalSim) {
            super(name);
            setPriority(Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY, priority)));
            this.taskCount     = taskCount;
            this.syncMode      = syncMode;
            this.totalSimTasks = totalSim;
        }

        @Override
        public void run() {
            log(getName() + " started | priority=" + getPriority()
                    + " | mode=" + (syncMode ? "SYNC" : "UNSYNC"));

            for (int i = 0; i < taskCount && !cancelled && simulationRunning; i++) {
                // Simulate work
                try { Thread.sleep(10 + (int)(Math.random() * 20)); }
                catch (InterruptedException e) { break; }

                tasksDone++;
                reportTaskDone(this, tasksDone, taskCount, syncMode, getId());
            }
            finished = true;
            log(getName() + " finished | tasks=" + tasksDone + " | priority=" + getPriority());
        }

        void cancel()          { cancelled = true; interrupt(); }
        boolean isFinished()   { return finished; }
        int getTasksDone()     { return tasksDone; }
    }

    // ─── Custom Renderers ────────────────────────────────────────────────────
    static class PriorityRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            try {
                int p = Integer.parseInt(v.toString());
                if      (p >= 8) setForeground(new Color(248, 113, 113));
                else if (p >= 5) setForeground(new Color(251, 191,  36));
                else             setForeground(new Color( 52, 211, 153));
                setBackground(sel ? new Color(40, 60, 100) : new Color(18, 24, 48));
            } catch (NumberFormatException ignored) {}
            return this;
        }
    }

    static class StateRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            setHorizontalAlignment(SwingConstants.CENTER);
            String state = v == null ? "" : v.toString();
            switch (state) {
                case "RUNNING": setForeground(new Color(52,  211, 153)); break;
                case "DONE":    setForeground(new Color(64,  156, 255)); break;
                case "READY":   setForeground(new Color(251, 191,  36)); break;
                default:        setForeground(new Color(100, 116, 139)); break;
            }
            setBackground(sel ? new Color(40, 60, 100) : new Color(18, 24, 48));
            return this;
        }
    }

    static class ProgressRenderer extends JProgressBar implements TableCellRenderer {
        ProgressRenderer() {
            super(0, 100);
            setStringPainted(true);
            setBorderPainted(false);
            setBackground(new Color(18, 24, 48));
            setForeground(new Color(64, 156, 255));
        }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int r, int c) {
            int val = v instanceof Integer ? (Integer) v : 0;
            setValue(val);
            setString(val + "%");
            return this;
        }
    }

    // ─── UI Helpers ──────────────────────────────────────────────────────────
    private JPanel card(String title) {
        JPanel p = new JPanel();
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, ACCENT_BLUE),
            new EmptyBorder(10, 12, 10, 12)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private JPanel statCard(JLabel value, String label, Color color) {
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setBackground(new Color(BG_CARD.getRed(), BG_CARD.getGreen(), BG_CARD.getBlue()));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        value.setFont(new Font("SansSerif", Font.BOLD, 22));
        value.setForeground(color);
        value.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 10));
        lbl.setForeground(TEXT_MUTED);
        p.add(value); p.add(lbl);
        return p;
    }

    private JLabel statLabel(String text, String label, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 22));
        l.setForeground(color);
        return l;
    }

    private JLabel styledLabel(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private JLabel label12(String text) { return styledLabel(text, 12, Font.PLAIN, TEXT_PRIMARY); }

    private JButton actionButton(String text, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(BG_CARD);
        b.setForeground(fg);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg.darker(), 1),
            new EmptyBorder(6, 12, 6, 12)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleSpinner(JSpinner s) {
        s.setBackground(BG_TABLE);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setBackground(BG_TABLE);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setForeground(TEXT_PRIMARY);
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setCaretColor(TEXT_PRIMARY);
    }

    // ─── Entry Point ─────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ThreadSyncApp().setVisible(true));
    }
}
