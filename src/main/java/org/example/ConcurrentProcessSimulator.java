package org.example;

import javax.swing.*;
import java.awt.*;

public class ConcurrentProcessSimulator extends JFrame {
    private JTabbedPane tabbedPane;
    private ProducerConsumerPanel producerConsumerPanel;
    private ReadersWritersPanel readersWritersPanel;
    private PhilosophersPanel philosophersPanel;

    public ConcurrentProcessSimulator() {
        setTitle("Simulador de Procesos Concurrentes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        initializeComponents();
        setupUI();
    }

    private void initializeComponents() {
        tabbedPane = new JTabbedPane();
        producerConsumerPanel = new ProducerConsumerPanel();
        readersWritersPanel = new ReadersWritersPanel();
        philosophersPanel = new PhilosophersPanel();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        
        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(63, 81, 181));
        headerPanel.setPreferredSize(new Dimension(0, 60));
        JLabel titleLabel = new JLabel("Simulador de Procesos Concurrentes");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel);
        
        // Tabs
        tabbedPane.addTab("Productor-Consumidor", producerConsumerPanel);
        tabbedPane.addTab("Lectores-Escritores", readersWritersPanel);
        tabbedPane.addTab("Filósofos Comensales", philosophersPanel);
        
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ConcurrentProcessSimulator().setVisible(true);
        });
    }
}
