package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.Semaphore;
import java.util.Random;

public class PhilosophersPanel extends JPanel {
    private static final int NUM_PHILOSOPHERS = 5;
    private JButton startButton, stopButton;
    private JLabel[] philosopherLabels, forkLabels;
    private JTextArea logArea;
    private JPanel tablePanel;

    private Semaphore[] forks;
    private Philosopher[] philosophers;
    private Thread[] philosopherThreads;
    private boolean isRunning = false;
    private Random random = new Random();

    public PhilosophersPanel() {
        initializeComponents();
        setupUI();
    }

    private void initializeComponents() {
        startButton = new JButton("Iniciar Simulación");
        stopButton = new JButton("Detener Simulación");
        stopButton.setEnabled(false);

        philosopherLabels = new JLabel[NUM_PHILOSOPHERS];
        forkLabels = new JLabel[NUM_PHILOSOPHERS];

        String[] names = {"Aristóteles", "Platón", "Sócrates", "Kant", "Descartes"};

        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            philosopherLabels[i] = new JLabel("<html><center>" + names[i] + "<br>Pensando</center></html>", SwingConstants.CENTER);
            philosopherLabels[i].setOpaque(true);
            philosopherLabels[i].setBackground(new Color(158, 158, 158));
            philosopherLabels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            philosopherLabels[i].setPreferredSize(new Dimension(100, 80));
            philosopherLabels[i].setFont(new Font("Arial", Font.BOLD, 12));

            forkLabels[i] = new JLabel("🍴", SwingConstants.CENTER);
            forkLabels[i].setOpaque(true);
            forkLabels[i].setBackground(Color.LIGHT_GRAY);
            forkLabels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            forkLabels[i].setPreferredSize(new Dimension(30, 30));
            forkLabels[i].setFont(new Font("Arial", Font.PLAIN, 16));
        }

        logArea = new JTextArea(12, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        forks = new Semaphore[NUM_PHILOSOPHERS];
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            forks[i] = new Semaphore(1);
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        // Table Panel (Circular arrangement)
        tablePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw table - usando coordenadas relativas
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int tableRadius = 70;

                // Mesa principal
                g2d.setColor(new Color(139, 69, 19));
                g2d.fillOval(centerX - tableRadius, centerY - tableRadius,
                        tableRadius * 2, tableRadius * 2);
                g2d.setColor(new Color(101, 67, 33));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(centerX - tableRadius, centerY - tableRadius,
                        tableRadius * 2, tableRadius * 2);

                // Borde decorativo
                g2d.setColor(new Color(160, 82, 45));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawOval(centerX - tableRadius + 5, centerY - tableRadius + 5,
                        (tableRadius - 5) * 2, (tableRadius - 5) * 2);
            }
        };
        tablePanel.setLayout(null);
        tablePanel.setPreferredSize(new Dimension(400, 400));
        tablePanel.setBackground(Color.WHITE);

        // Añadir todos los componentes al panel
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            tablePanel.add(philosopherLabels[i]);
            tablePanel.add(forkLabels[i]);
        }

        // Listener para reposicionar cuando cambie el tamaño del panel
        tablePanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repositionComponents();
            }
        });

        // Log Panel
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));

        add(controlPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(logScrollPane, BorderLayout.SOUTH);

        // Event Listeners
        startButton.addActionListener(e -> startSimulation());
        stopButton.addActionListener(e -> stopSimulation());
    }

    // Método para reposicionar filósofos y tenedores usando coordenadas relativas
    private void repositionComponents() {
        if (tablePanel.getWidth() <= 0 || tablePanel.getHeight() <= 0) return;

        // Usar coordenadas relativas al centro actual del panel
        int centerX = tablePanel.getWidth() / 2;
        int centerY = tablePanel.getHeight() / 2;
        int philRadius = 140;  // Radio para filósofos
        int forkRadius = 100;  // Radio para tenedores

        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            // Ángulo para cada filósofo (empezando desde arriba)
            double philAngle = 2 * Math.PI * i / NUM_PHILOSOPHERS - Math.PI / 2;

            // Position philosopher - usando coordenadas relativas
            int philX = (int) (centerX + philRadius * Math.cos(philAngle) - 50);
            int philY = (int) (centerY + philRadius * Math.sin(philAngle) - 40);
            philosopherLabels[i].setBounds(philX, philY, 100, 80);

            // Position fork (entre el filósofo actual y el siguiente)
            double forkAngle = 2 * Math.PI * (i + 0.5) / NUM_PHILOSOPHERS - Math.PI / 2;
            int forkX = (int) (centerX + forkRadius * Math.cos(forkAngle) - 15);
            int forkY = (int) (centerY + forkRadius * Math.sin(forkAngle) - 15);
            forkLabels[i].setBounds(forkX, forkY, 30, 30);
        }

        tablePanel.repaint();
    }

    private void startSimulation() {
        if (!isRunning) {
            isRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            logArea.setText("");

            // Asegurar posicionamiento correcto al iniciar
            SwingUtilities.invokeLater(() -> repositionComponents());

            // Reset forks
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                forks[i] = new Semaphore(1);
                SwingUtilities.invokeLater(() -> {
                    for (int j = 0; j < NUM_PHILOSOPHERS; j++) {
                        forkLabels[j].setBackground(Color.LIGHT_GRAY);
                    }
                });
            }

            philosophers = new Philosopher[NUM_PHILOSOPHERS];
            philosopherThreads = new Thread[NUM_PHILOSOPHERS];

            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                philosophers[i] = new Philosopher(i);
                philosopherThreads[i] = new Thread(philosophers[i]);
                philosopherThreads[i].start();
            }

            log("Simulación de los Filósofos Comensales iniciada");
        }
    }

    private void stopSimulation() {
        if (isRunning) {
            isRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            if (philosopherThreads != null) {
                for (Thread thread : philosopherThreads) {
                    if (thread != null) thread.interrupt();
                }
            }

            String[] names = {"Aristóteles", "Platón", "Sócrates", "Kant", "Descartes"};
            for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
                final int index = i;
                SwingUtilities.invokeLater(() -> {
                    philosopherLabels[index].setText("<html><center>" + names[index] + "<br>Detenido</center></html>");
                    philosopherLabels[index].setBackground(new Color(158, 158, 158));
                    forkLabels[index].setBackground(Color.LIGHT_GRAY);
                });
            }

            log("Simulación detenida");
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(String.format("[%d] %s\n", System.currentTimeMillis() % 10000, message));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private class Philosopher implements Runnable {
        private int id;
        private String[] names = {"Aristóteles", "Platón", "Sócrates", "Kant", "Descartes"};

        public Philosopher(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    think();
                    eat();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void think() throws InterruptedException {
            SwingUtilities.invokeLater(() -> {
                philosopherLabels[id].setText("<html><center>" + names[id] + "<br>Pensando</center></html>");
                philosopherLabels[id].setBackground(new Color(158, 158, 158));
            });

            log(names[id] + " está pensando");
            Thread.sleep(2000 + random.nextInt(3000));
        }

        private void eat() throws InterruptedException {
            int leftFork = id;
            int rightFork = (id + 1) % NUM_PHILOSOPHERS;

            // Prevent deadlock by ordering fork acquisition
            int firstFork = Math.min(leftFork, rightFork);
            int secondFork = Math.max(leftFork, rightFork);

            SwingUtilities.invokeLater(() -> {
                philosopherLabels[id].setText("<html><center>" + names[id] + "<br>Hambriento</center></html>");
                philosopherLabels[id].setBackground(Color.ORANGE);
            });

            log(names[id] + " tiene hambre y busca tenedores");

            // Acquire forks in order
            forks[firstFork].acquire();
            SwingUtilities.invokeLater(() -> {
                forkLabels[firstFork].setBackground(Color.RED);
            });
            log(names[id] + " tomó el tenedor " + (firstFork + 1));

            forks[secondFork].acquire();
            SwingUtilities.invokeLater(() -> {
                forkLabels[secondFork].setBackground(Color.RED);
            });
            log(names[id] + " tomó el tenedor " + (secondFork + 1));

            // Eating
            SwingUtilities.invokeLater(() -> {
                philosopherLabels[id].setText("<html><center>" + names[id] + "<br>Comiendo</center></html>");
                philosopherLabels[id].setBackground(new Color(76, 175, 80));
            });

            log(names[id] + " está comiendo");
            Thread.sleep(1500 + random.nextInt(2500));

            // Release forks
            forks[firstFork].release();
            forks[secondFork].release();

            SwingUtilities.invokeLater(() -> {
                forkLabels[firstFork].setBackground(Color.LIGHT_GRAY);
                forkLabels[secondFork].setBackground(Color.LIGHT_GRAY);
            });

            log(names[id] + " terminó de comer y liberó los tenedores");
        }
    }
}