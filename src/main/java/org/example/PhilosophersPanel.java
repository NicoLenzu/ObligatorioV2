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
    private JLabel[] philosopherLabels;
    private JPanel[] forkPanels;
    private JTextArea logArea;
    private JPanel tablePanel;

    private Semaphore[] forks;
    private Philosopher[] philosophers;
    private Thread[] philosopherThreads;
    private boolean isRunning = false;
    private Random random = new Random();

    // Estados de los palillos
    private boolean[] forkInUse = new boolean[NUM_PHILOSOPHERS];

    public PhilosophersPanel() {
        initializeComponents();
        setupUI();
    }

    private void initializeComponents() {
        startButton = new JButton("Iniciar Simulación");
        stopButton = new JButton("Detener Simulación");
        stopButton.setEnabled(false);

        philosopherLabels = new JLabel[NUM_PHILOSOPHERS];
        forkPanels = new JPanel[NUM_PHILOSOPHERS];

        String[] names = {"Aristóteles", "Platón", "Sócrates", "Kant", "Descartes"};

        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            philosopherLabels[i] = new JLabel("<html><center>" + names[i] + "<br>Pensando</center></html>", SwingConstants.CENTER);
            philosopherLabels[i].setOpaque(true);
            philosopherLabels[i].setBackground(new Color(158, 158, 158));
            philosopherLabels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            philosopherLabels[i].setPreferredSize(new Dimension(100, 80));
            philosopherLabels[i].setFont(new Font("Arial", Font.BOLD, 12));

            // Crear paneles personalizados para los palillos
            forkPanels[i] = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int width = getWidth();
                    int height = getHeight();

                    // Dibujar el palillo como un rectángulo marrón
                    g2d.setColor(new Color(139, 69, 19)); // Marrón
                    g2d.fillRoundRect(width/4, height/6, width/2, height*2/3, 3, 3);

                    // Borde más oscuro
                    g2d.setColor(new Color(101, 67, 33));
                    g2d.setStroke(new BasicStroke(1));
                    g2d.drawRoundRect(width/4, height/6, width/2, height*2/3, 3, 3);
                }
            };
            forkPanels[i].setOpaque(true);
            forkPanels[i].setBackground(Color.LIGHT_GRAY);
            forkPanels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            forkPanels[i].setPreferredSize(new Dimension(30, 30));

            forkInUse[i] = false;
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
            tablePanel.add(forkPanels[i]);
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
            forkPanels[i].setBounds(forkX, forkY, 30, 30);
        }

        tablePanel.repaint();
    }

    private void updateForkDisplay(int forkIndex, boolean inUse) {
        SwingUtilities.invokeLater(() -> {
            forkInUse[forkIndex] = inUse;
            if (inUse) {
                forkPanels[forkIndex].setBackground(new Color(255, 182, 193)); // Rosa claro para indicar uso
                forkPanels[forkIndex].setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            } else {
                forkPanels[forkIndex].setBackground(Color.LIGHT_GRAY);
                forkPanels[forkIndex].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            }
            forkPanels[forkIndex].repaint();
        });
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
                updateForkDisplay(i, false);
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
                });
                updateForkDisplay(i, false);
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
            // El palillo izquierdo del filósofo i es el palillo i
            // El palillo derecho del filósofo i es el palillo (i+1) % NUM_PHILOSOPHERS
            // Pero visualmente, el palillo i está entre el filósofo i y el filósofo (i+1)

            // Para el filósofo i:
            // - Su palillo izquierdo es el palillo (i-1+NUM_PHILOSOPHERS) % NUM_PHILOSOPHERS
            // - Su palillo derecho es el palillo i
            int leftFork = (id - 1 + NUM_PHILOSOPHERS) % NUM_PHILOSOPHERS;
            int rightFork = id;
            // Ordeno los palillos a agarrar
            int firstFork = Math.min(leftFork, rightFork);
            int secondFork = Math.max(leftFork, rightFork);

            SwingUtilities.invokeLater(() -> {
                philosopherLabels[id].setText("<html><center>" + names[id] + "<br>Hambriento</center></html>");
                philosopherLabels[id].setBackground(Color.ORANGE);
            });

            log(names[id] + " tiene hambre y busca palillos");

            // Agarro los palillos en orden
            forks[firstFork].acquire();
            updateForkDisplay(firstFork, true);
            log(names[id] + " tomó el palillo " + (firstFork + 1) + " (a su " +
                    (firstFork == leftFork ? "izquierda" : "derecha") + ")");

            forks[secondFork].acquire();
            updateForkDisplay(secondFork, true);
            log(names[id] + " tomó el palillo " + (secondFork + 1) + " (a su " +
                    (secondFork == leftFork ? "izquierda" : "derecha") + ")");

            // Comer
            SwingUtilities.invokeLater(() -> {
                philosopherLabels[id].setText("<html><center>" + names[id] + "<br>Comiendo</center></html>");
                philosopherLabels[id].setBackground(new Color(76, 175, 80));
            });

            log(names[id] + " está comiendo con palillos " + (leftFork + 1) + " y " + (rightFork + 1));
            Thread.sleep(1500 + random.nextInt(2500));

            // Dejo palillos
            forks[firstFork].release();
            updateForkDisplay(firstFork, false);
            forks[secondFork].release();
            updateForkDisplay(secondFork, false);


            log(names[id] + " terminó de comer y liberó los palillos " + (leftFork + 1) + " y " + (rightFork + 1));
        }
    }
}