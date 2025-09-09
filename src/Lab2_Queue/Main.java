package Lab2_Queue;

/*
* Создать очередь сообщений, в которую пишут N потоков (количество
потоков задается через args), и читают N потоков, использовать только
пакет java.util.concurrent. Имена потоков должны быть осмыслены,
использовать конструкции с wait/notify запрещено.
*/

import java.util.concurrent.*;

class MessageSender implements Runnable {
    private final BlockingQueue<String> queue;

    public MessageSender(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (Main.running) {
                String msg = generateMessage();
                queue.put(msg);
                System.out.println(Thread.currentThread().getName()
                + " отправил " + msg);
                Thread.sleep(ThreadLocalRandom.current().nextInt(800,1200));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String generateMessage() {
        String[] varieties = {
                " : всем привет!",
                " : однако, хорошая погода сегодня.",
                " : нет ничего лучше, чем холодная кола...",
                " : айда с нами, на заре выходим :)"};
        return varieties[ThreadLocalRandom.current().nextInt(varieties.length)];
    }
}

class MessageHandler implements Runnable {
    private final BlockingQueue<String> queue;

    MessageHandler(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (Main.running || !queue.isEmpty()) {
                String msg = queue.poll(1, TimeUnit.SECONDS);
                if (msg != null) {
                    System.out.println(Thread.currentThread().getName() +
                            "  получил  " + msg);
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1200,1800));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(Thread.currentThread().getName() + " завершил работу");
    }
}

public class Main {
    public static volatile boolean running = true;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Количество потоков не задано (передайте значение через {args})!");
            return;
        }

        final int count_of_threads = Integer.parseInt(args[0]);
        System.out.println("Количество потоков = " + count_of_threads);
        System.out.println("--------");

        BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        for (int i = 1; i <= count_of_threads; i++) {
            Thread SenderThread = new Thread(new MessageSender(queue), "Отправитель-" + i);
            SenderThread.start();
        }

        for (int i = 1; i <= count_of_threads; i++) {
            Thread HandlerThread = new Thread(new MessageHandler(queue), "Получатель-" + i);
            HandlerThread.start();
        }

        try {
            Thread.sleep(5000);
            running = false;
            System.out.println("--------");
            System.out.println("Происходит завершение работы...");
            System.out.println("--------");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
