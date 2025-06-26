package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Semaphore;
import java.util.Random;

public class ReadersWritersPanel extends JPanel {
    private JButton startButton, stopButton;
    private JTextArea databaseArea, logArea;
    private JLabel[] readerLabels, writerLabels;
    private JSlider readerCountSlider, writerCountSlider;
    
    private Database database;
    private Thread[] readerThreads, writerThreads;
    private Reader[] readers;
    private Writer[] writers;
    private boolean isRunning = false;
    private Random random = new Random();

    public ReadersWritersPanel() {
        database = new Database();
        initializeComponents();
        setupUI();
    }

    private void initializeComponents() {
        startButton = new JButton("Iniciar Simulación");
        stopButton = new JButton("Detener Simulación");
        stopButton.setEnabled(false);
        
        databaseArea = new JTextArea(8, 30);
        databaseArea.setEditable(false);
        databaseArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        databaseArea.setText("Base de Datos Vacía");
        databaseArea.setBackground(new Color(240, 240, 240));
        
        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        readerCountSlider = new JSlider(1, 5, 3);
        writerCountSlider = new JSlider(1, 3, 2);
        
        readerCountSlider.setBorder(BorderFactory.createTitledBorder("Número de Lectores"));
        writerCountSlider.setBorder(BorderFactory.createTitledBorder("Número de Escritores"));
        
        readerLabels = new JLabel[5];
        writerLabels = new JLabel[3];
        
        for (int i = 0; i < 5; i++) {
            readerLabels[i] = new JLabel("Lector " + (i + 1) + ": Inactivo");
            readerLabels[i].setOpaque(true);
            readerLabels[i].setBackground(Color.LIGHT_GRAY);
            readerLabels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }
        
        for (int i = 0; i < 3; i++) {
            writerLabels[i] = new JLabel("Escritor " + (i + 1) + ": Inactivo");
            writerLabels[i].setOpaque(true);
            writerLabels[i].setBackground(Color.LIGHT_GRAY);
            writerLabels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        
        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(readerCountSlider);
        controlPanel.add(writerCountSlider);
        
        // Status Panel
        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        
        JPanel readersPanel = new JPanel(new GridLayout(5, 1));
        readersPanel.setBorder(BorderFactory.createTitledBorder("Lectores"));
        for (JLabel label : readerLabels) {
            readersPanel.add(label);
        }
        
        JPanel writersPanel = new JPanel(new GridLayout(3, 1));
        writersPanel.setBorder(BorderFactory.createTitledBorder("Escritores"));
        for (JLabel label : writerLabels) {
            writersPanel.add(label);
        }
        
        statusPanel.add(readersPanel);
        statusPanel.add(writersPanel);
        
        // Database Panel
        JScrollPane databaseScrollPane = new JScrollPane(databaseArea);
        databaseScrollPane.setBorder(BorderFactory.createTitledBorder("Base de Datos"));
        
        // Log Panel
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));
        
        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(statusPanel, BorderLayout.WEST);
        mainPanel.add(databaseScrollPane, BorderLayout.CENTER);
        
        add(controlPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        add(logScrollPane, BorderLayout.SOUTH);
        
        // Event Listeners
        startButton.addActionListener(e -> startSimulation());
        stopButton.addActionListener(e -> stopSimulation());
    }

    private void startSimulation() {
        if (!isRunning) {
            isRunning = true;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            
            database.reset();
            logArea.setText("");
            
            int readerCount = readerCountSlider.getValue();
            int writerCount = writerCountSlider.getValue();
            
            readers = new Reader[readerCount];
            writers = new Writer[writerCount];
            readerThreads = new Thread[readerCount];
            writerThreads = new Thread[writerCount];
            
            for (int i = 0; i < readerCount; i++) {
                readers[i] = new Reader(i);
                readerThreads[i] = new Thread(readers[i]);
                readerThreads[i].start();
            }
            
            for (int i = 0; i < writerCount; i++) {
                writers[i] = new Writer(i);
                writerThreads[i] = new Thread(writers[i]);
                writerThreads[i].start();
            }
            
            log("Simulación iniciada con " + readerCount + " lectores y " + writerCount + " escritores");
        }
    }

    private void stopSimulation() {
        if (isRunning) {
            isRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            
            if (readerThreads != null) {
                for (Thread thread : readerThreads) {
                    if (thread != null) thread.interrupt();
                }
            }
            
            if (writerThreads != null) {
                for (Thread thread : writerThreads) {
                    if (thread != null) thread.interrupt();
                }
            }
            
            for (JLabel label : readerLabels) {
                label.setText(label.getText().split(":")[0] + ": Inactivo");
                label.setBackground(Color.LIGHT_GRAY);
            }
            
            for (JLabel label : writerLabels) {
                label.setText(label.getText().split(":")[0] + ": Inactivo");
                label.setBackground(Color.LIGHT_GRAY);
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

    private class Database {
        private StringBuilder data = new StringBuilder("Base de Datos Inicial\n");
        private int readerCount = 0;
        private Semaphore readCountMutex = new Semaphore(1);
        private Semaphore writeMutex = new Semaphore(1);

        public void read(int readerId) throws InterruptedException {
            readCountMutex.acquire();
            readerCount++;
            if (readerCount == 1) {
                writeMutex.acquire();
            }
            readCountMutex.release();

            // Reading
            SwingUtilities.invokeLater(() -> {
                readerLabels[readerId].setText("Lector " + (readerId + 1) + ": Leyendo");
                readerLabels[readerId].setBackground(new Color(76, 175, 80));
                databaseArea.setText(data.toString());
            });
            
            Thread.sleep(1000 + random.nextInt(2000));

            readCountMutex.acquire();
            readerCount--;
            if (readerCount == 0) {
                writeMutex.release();
            }
            readCountMutex.release();
        }

        public void write(int writerId, String newData) throws InterruptedException {
            writeMutex.acquire();

            SwingUtilities.invokeLater(() -> {
                writerLabels[writerId].setText("Escritor " + (writerId + 1) + ": Escribiendo");
                writerLabels[writerId].setBackground(new Color(244, 67, 54));
            });

            data.append(newData).append("\n");
            
            SwingUtilities.invokeLater(() -> {
                databaseArea.setText(data.toString());
            });
            
            Thread.sleep(2000 + random.nextInt(3000));

            writeMutex.release();
        }

        public void reset() {
            data = new StringBuilder("Base de Datos Inicial\n");
            readerCount = 0;
            readCountMutex = new Semaphore(1);
            writeMutex = new Semaphore(1);
            SwingUtilities.invokeLater(() -> databaseArea.setText(data.toString()));
        }
    }

    private class Reader implements Runnable {
        private int id;

        public Reader(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    SwingUtilities.invokeLater(() -> {
                        readerLabels[id].setText("Lector " + (id + 1) + ": Esperando");
                        readerLabels[id].setBackground(Color.YELLOW);
                    });
                    
                    database.read(id);
                    log("Lector " + (id + 1) + " terminó de leer");
                    
                    SwingUtilities.invokeLater(() -> {
                        readerLabels[id].setText("Lector " + (id + 1) + ": Descansando");
                        readerLabels[id].setBackground(Color.LIGHT_GRAY);
                    });
                    
                    Thread.sleep(2000 + random.nextInt(3000));
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private class Writer implements Runnable {
        private int id;
        private int writeCount = 1;

        public Writer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    SwingUtilities.invokeLater(() -> {
                        writerLabels[id].setText("Escritor " + (id + 1) + ": Esperando");
                        writerLabels[id].setBackground(Color.YELLOW);
                    });
                    
                    String newData = "Escritor " + (id + 1) + " - Entrada #" + writeCount++;
                    database.write(id, newData);
                    log("Escritor " + (id + 1) + " escribió: " + newData);
                    
                    SwingUtilities.invokeLater(() -> {
                        writerLabels[id].setText("Escritor " + (id + 1) + ": Descansando");
                        writerLabels[id].setBackground(Color.LIGHT_GRAY);
                    });
                    
                    Thread.sleep(3000 + random.nextInt(4000));
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }
}
