import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Semaphore;

public class Main {
    private static final int MAX_CHAIRS = 5;
    private static Semaphore chairs = new Semaphore(MAX_CHAIRS);
    private static Semaphore barberChair = new Semaphore(1);
    private static Semaphore customerReady = new Semaphore(0);
    private static Semaphore barberReady = new Semaphore(0);

    private static JLabel barberStatusLabel;
    private static JLabel[] chairStatusLabels = new JLabel[MAX_CHAIRS];

    static {
        for (int i = 0; i < MAX_CHAIRS; i++) {
            chairStatusLabels[i] = new JLabel("Свободно");
            chairStatusLabels[i].setForeground(Color.GREEN);
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Barbershop");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLayout(new BorderLayout());

                JPanel centerPanel = new JPanel();
                centerPanel.setLayout(new BorderLayout());

                barberStatusLabel = new JLabel("Парикмахер дрыхнет");
                centerPanel.add(barberStatusLabel, BorderLayout.CENTER);
                JPanel chairsPanel = new JPanel();
                chairsPanel.setLayout(new GridLayout(1, MAX_CHAIRS));

                // Инициализация пятого кресла
                chairStatusLabels[MAX_CHAIRS - 1] = new JLabel("Свободно");
                chairStatusLabels[MAX_CHAIRS - 1].setForeground(Color.GREEN);
                chairsPanel.add(chairStatusLabels[MAX_CHAIRS - 1]);

                for (int i = 0; i < MAX_CHAIRS - 1; i++) {
                    chairStatusLabels[i] = new JLabel("Свободно");
                    chairStatusLabels[i].setForeground(Color.GREEN);
                    chairsPanel.add(chairStatusLabels[i]);
                }
                centerPanel.add(chairsPanel, BorderLayout.NORTH);

                frame.add(centerPanel, BorderLayout.CENTER);
                frame.setSize(400, 200);
                frame.setVisible(true);
            }
        });

        Thread barberThread = new Barber();
        barberThread.start();

        for (int i = 1; i <= 15; i++) {
            Thread customerThread = new Customer();
            customerThread.setName(Integer.toString(i));
            customerThread.start();

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    static class Customer extends Thread {
        @Override
        public void run() {
            if (chairs.tryAcquire()) {
                System.out.println("Посетитель " + this.getName() + " ждет в кресле.");
                int chairIndex = occupyChair(this.getName());
                if (chairIndex != -1) {
                    customerReady.release();
                    try {
                        barberReady.acquire();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    System.out.println("Посетителя " + this.getName() + " стригут.");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    barberChair.release();
                    releaseChair(chairIndex);
                    System.out.println("Посетитель " + this.getName() + " убегает.");
                } else {
                    System.out.println("Посетитель " + this.getName() + " не может найти свободное место и проваливает.");
                }
            } else {
                System.out.println("Посетитель " + this.getName() + " не может найти свободное место и проваливает.");
            }
        }
    }

    static class Barber extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    setBarberStatusLabel("Парикмахер дрыхнет.");
                    customerReady.acquire();
                    barberReady.release();

                    barberChair.acquire();
                    setBarberStatusLabel("Парикмахер стрижет посетителя.");
                    Thread.sleep(3000);

                    barberChair.release();
                    setBarberStatusLabel("Парикмахер дрыхнет.");
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void setChairStatus(final int chairIndex, final String status) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                chairStatusLabels[chairIndex].setText(status);
                chairStatusLabels[chairIndex].setForeground(status.contains("Занято") ? Color.RED : Color.GREEN);
            }
        });
    }

    private static int occupyChair(String customerName) {
        for (int i = 0; i < MAX_CHAIRS; i++) {
            if (chairStatusLabels[i] != null && chairStatusLabels[i].getText().equals("Свободно")) {
                setChairStatus(i, "Занято: " + customerName);
                return i;
            }
        }
        return -1; // Вернуть -1, если нет доступных кресел
    }

    private static void releaseChair(int chairIndex) {
        setChairStatus(chairIndex, "Свободно");
        chairs.release();
    }

    private static void setBarberStatusLabel(final String status) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                barberStatusLabel.setText(status);
            }
        });
    }
}
