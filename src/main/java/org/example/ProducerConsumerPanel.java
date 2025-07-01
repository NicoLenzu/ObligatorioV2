package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumerPanel extends JPanel {
    private static final int BUFFER_SIZE = 10;
    private JButton startButton, stopButton;
    private JLabel[] bufferLabels;
    private JLabel producerStatusLabel, consumerStatusLabel, bufferInfoLabel;
    private JTextArea logArea;
    private JSlider producerSpeedSlider, consumerSpeedSlider;

    private Buffer buffer;
    private Producer producer;
    private Consumer consumer;
    private Thread producerThread, consumerThread;
    private boolean isRunning = false;

    public ProducerConsumerPanel() {
        buffer = new Buffer();
        initializeComponents();
        setupUI();
    }

    private void initializeComponents() {
        startButton = new JButton("Iniciar Simulación");
        stopButton = new JButton("Detener Simulación");
        stopButton.setEnabled(false);

        bufferLabels = new JLabel[BUFFER_SIZE];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            bufferLabels[i] = new JLabel("", SwingConstants.CENTER);
            bufferLabels[i].setOpaque(true);
            bufferLabels[i].setBackground(Color.LIGHT_GRAY);
            bufferLabels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            bufferLabels[i].setPreferredSize(new Dimension(50, 50));
            bufferLabels[i].setFont(new Font("Arial", Font.BOLD, 12));
        }

        producerStatusLabel = new JLabel("Productor: Detenido");
        consumerStatusLabel = new JLabel("Consumidor: Detenido");
        bufferInfoLabel = new JLabel("Buffer: 0/10 items");

        logArea = new JTextArea(10, 30);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        producerSpeedSlider = new JSlider(100, 2000, 1000);
        consumerSpeedSlider = new JSlider(100, 2000, 1000);

        producerSpeedSlider.setBorder(BorderFactory.createTitledBorder("Velocidad Productor (ms)"));
        consumerSpeedSlider.setBorder(BorderFactory.createTitledBorder("Velocidad Consumidor (ms)"));
    }

    private void setupUI() {
        setLayout(new BorderLayout());

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(producerSpeedSlider);
        controlPanel.add(consumerSpeedSlider);

        // Buffer Visualization
        JPanel bufferPanel = new JPanel(new GridLayout(1, BUFFER_SIZE, 5, 5));
        bufferPanel.setBorder(BorderFactory.createTitledBorder("Buffer Circular"));
        for (int i = 0; i < BUFFER_SIZE; i++) {
            JPanel cellPanel = new JPanel(new BorderLayout());
            cellPanel.add(new JLabel(String.valueOf(i), SwingConstants.CENTER), BorderLayout.NORTH);
            cellPanel.add(bufferLabels[i], BorderLayout.CENTER);
            bufferPanel.add(cellPanel);
        }

        // Status Panel
        JPanel statusPanel = new JPanel(new GridLayout(3, 1));
        statusPanel.add(producerStatusLabel);
        statusPanel.add(consumerStatusLabel);
        statusPanel.add(bufferInfoLabel);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(bufferPanel, BorderLayout.NORTH);
        mainPanel.add(statusPanel, BorderLayout.CENTER);

        // Log Panel
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));

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

            buffer.reset();
            updateBufferDisplay();
            logArea.setText("");

            producer = new Producer();
            consumer = new Consumer();

            producerThread = new Thread(producer);
            consumerThread = new Thread(consumer);

            producerThread.start();
            consumerThread.start();

            log("Simulación iniciada");
        }
    }

    private void stopSimulation() {
        if (isRunning) {
            isRunning = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            if (producerThread != null) producerThread.interrupt();
            if (consumerThread != null) consumerThread.interrupt();

            SwingUtilities.invokeLater(() -> {
                producerStatusLabel.setText("Productor: Detenido");
                consumerStatusLabel.setText("Consumidor: Detenido");
            });

            log("Simulación detenida");
        }
    }

    private void updateBufferDisplay() {
        // Capturar el estado del buffer de forma thread-safe
        buffer.mutex.lock();
        int[] itemsCopy = new int[BUFFER_SIZE];
        System.arraycopy(buffer.items, 0, itemsCopy, 0, BUFFER_SIZE);
        int currentIn = buffer.in;
        int currentOut = buffer.out;
        int currentCount = buffer.count;
        buffer.mutex.unlock();

        SwingUtilities.invokeLater(() -> {
            // Resetear todos los labels
            for (int i = 0; i < BUFFER_SIZE; i++) {
                bufferLabels[i].setText("");
                bufferLabels[i].setBackground(Color.LIGHT_GRAY);
                bufferLabels[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            }

            // Mostrar elementos ocupados en el buffer circular
            if (currentCount > 0) {
                int current = currentOut;
                for (int i = 0; i < currentCount; i++) {
                    int pos = (current + i) % BUFFER_SIZE;
                    if (itemsCopy[pos] != 0) {
                        bufferLabels[pos].setText(String.valueOf(itemsCopy[pos]));
                        bufferLabels[pos].setBackground(new Color(76, 175, 80)); // Verde para ocupado
                    }
                }
            }

            // Destacar posición de inserción (in) y extracción (out)
            if (currentCount < BUFFER_SIZE) {
                bufferLabels[currentIn].setBorder(BorderFactory.createLineBorder(Color.BLUE, 3)); // Azul para próxima inserción
            }
            if (currentCount > 0) {
                bufferLabels[currentOut].setBorder(BorderFactory.createLineBorder(Color.RED, 3)); // Rojo para próxima extracción
            }

            // Actualizar información del buffer
            bufferInfoLabel.setText(String.format("Buffer: %d/%d items (IN=%d, OUT=%d)",
                    currentCount, BUFFER_SIZE, currentIn, currentOut));
        });
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(String.format("[%d] %s\n", System.currentTimeMillis() % 10000, message));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private class Buffer {
        private int[] items = new int[BUFFER_SIZE];
        private int in = 0, out = 0, count = 0;
        private Semaphore empty = new Semaphore(BUFFER_SIZE);
        private Semaphore full = new Semaphore(0);
        private ReentrantLock mutex = new ReentrantLock();

        public void produce(int item) throws InterruptedException {
            empty.acquire(); // Esperar a que haya espacio
            mutex.lock();
            try {
                items[in] = item; //<---- Donde insertar el item
                log(String.format("Productor insertó item %d en posición %d", item, in));
                in = (in + 1) % BUFFER_SIZE;
                count++;
            } finally {
                mutex.unlock();
            }
            full.release(); // Señalar que hay un nuevo item
            updateBufferDisplay(); // Actualizar después de liberar el lock
        }

        public int consume() throws InterruptedException {
            full.acquire(); // Esperar a que haya items
            mutex.lock();
            int item;
            try {
                item = items[out];
                items[out] = 0; // Limpiar la posición
                log(String.format("Consumidor extrajo item %d de posición %d", item, out));
                out = (out + 1) % BUFFER_SIZE;
                count--;
            } finally {
                mutex.unlock();
            }
            empty.release(); // Señalar que hay espacio libre
            updateBufferDisplay(); // Actualizar después de liberar el lock
            return item;
        }

        public void reset() {
            mutex.lock();
            try {
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    items[i] = 0;
                }
                in = out = count = 0;
                empty = new Semaphore(BUFFER_SIZE);
                full = new Semaphore(0);
            } finally {
                mutex.unlock();
            }
        }
    }

    private class Producer implements Runnable {
        private int itemCount = 1;

        @Override
        public void run() {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    SwingUtilities.invokeLater(() ->
                            producerStatusLabel.setText("Productor: Produciendo item " + itemCount));

                    // Simular tiempo de producción
                    Thread.sleep(producerSpeedSlider.getValue() / 3);

                    buffer.produce(itemCount);//<-----
                    log("Productor creó item: " + itemCount);

                    SwingUtilities.invokeLater(() ->
                            producerStatusLabel.setText("Productor: Item " + itemCount + " producido"));

                    itemCount++;//<-----

                    // Pausa después de producir
                    Thread.sleep(producerSpeedSlider.getValue() / 3);
                } catch (InterruptedException e) {
                    break;
                }
            }
            SwingUtilities.invokeLater(() ->
                    producerStatusLabel.setText("Productor: Detenido"));
        }
    }

    private class Consumer implements Runnable {
        @Override
        public void run() {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    SwingUtilities.invokeLater(() ->
                            consumerStatusLabel.setText("Consumidor: Esperando item"));

                    int item = buffer.consume();

                    SwingUtilities.invokeLater(() ->
                            consumerStatusLabel.setText("Consumidor: Procesando item " + item));

                    log("Consumidor procesó item: " + item);

                    // Simular tiempo de procesamiento
                    Thread.sleep(consumerSpeedSlider.getValue());
                } catch (InterruptedException e) {
                    break;
                }
            }
            SwingUtilities.invokeLater(() ->
                    consumerStatusLabel.setText("Consumidor: Detenido"));
        }
    }
}