package Lab1_Supervisor;

/*
* Создать супервизор (управляющую программу), которая контролирует
исполнение абстрактной программы.
Абстрактная программа работает в отдельном потоке и является
классом с полем перечисляемого типа, который отражает ее состояние:
    ● UNKNOWN – перед первым запуском
    ● STOPPING – остановлена
    ● RUNNING – работает
    ● FATAL ERROR – критическая ошибка
и имеет поток-демон случайного состояния, который в заданном
интервале меняет её состояние на случайное.
У супервизора должны быть методы остановки и запуска
абстрактной программы, которые меняют ее состояние. Супервизор
является потоком, который циклически опрашивает абстрактную
программу, и если ее состояние STOPPING, то перезапускает ее. Если
состояние FATAL ERROR, то работа абстрактной программы
завершается супервизором. Все изменения состояний должны
сопровождаться
соответствующими сообщениями в консоли.
Супервизор не должен пропустить ни одного статуса абстрактной
программы. Использовать конструкции с wait/notify.
*/

import java.util.Random;

enum ProgramStatus {
    UNKNOWN,
    STOPPING,
    RUNNING,
    FATAL_ERROR
}

class AbstractProgram {

    private ProgramStatus status;
    private volatile boolean running;
    private volatile Thread daemonThread;

    public AbstractProgram() {
        this.status = ProgramStatus.UNKNOWN;
        this.running = false;
    }

    public synchronized ProgramStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(ProgramStatus status) {
        if (this.status != status) { // Чтобы не было повторений состояний
            System.out.println("• Состояние изменено с " + this.status + " на: " + status);
            this.status = status;
            notifyAll(); // Уведомляем другие потоки о смене состояния
        }
    }

    @Override
    public String toString() {
        return "Lab1_supervisor.AbstractProgram{" +
                "status=" + status +
                '}';
    }

    public synchronized void startRandomThread() {
        if (running) return;

        running = true;
        daemonThread = new Thread(() -> {
            while (running) {
                synchronized (this) {
                    try {
                        wait(2000);
                        if (running) {
                            ProgramStatus[] states = ProgramStatus.values();
                            setStatus(states[new Random().nextInt(states.length)]);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Флаг прерывания
                        break;
                    }
                }
            }
        });
        daemonThread.setDaemon(true); // Устанавливаем поток, как демона
        daemonThread.start();
        setStatus(ProgramStatus.RUNNING);
    }

    public synchronized void stopProgram() {
        if (!running) return;

        running = false;
        if (daemonThread != null) {
            daemonThread.interrupt();
        }

        if (status != ProgramStatus.STOPPING) {
            setStatus(ProgramStatus.STOPPING);
        } else {
            notifyAll();
        }
    }

    public synchronized void FatalError() {
        running = false;
        if (daemonThread != null) {
            daemonThread.interrupt();
        }
        notifyAll(); // Уведомляем ожидающие потоки
    }
}

class Supervisor {
    private final AbstractProgram abstractProgram;
    private volatile boolean supervising;
    private Thread supervisorThread;

    public Supervisor(AbstractProgram abstractProgram) {
        this.abstractProgram = abstractProgram;
        this.supervising = false;
    }

    public void start() {
        if (supervising) return;

        supervising = true;
        abstractProgram.startRandomThread();
        startMonitoring();
    }

    public void stop() {
        supervising = false;
        if (supervisorThread != null) {
            supervisorThread.interrupt();
        }
        abstractProgram.FatalError();
    }

    private void startMonitoring() {
        supervisorThread = new Thread(() -> {
           while (supervising) {
               synchronized (abstractProgram) {
                   try {
                       ProgramStatus previousState = abstractProgram.getStatus();
                       abstractProgram.wait(); // Ожидаем изменения состояния программы
                       ProgramStatus currentState = abstractProgram.getStatus();

                       if (currentState == previousState) {
                           continue;
                       }

                       System.out.print("/ SUPERVISOR -> состояние: " + currentState);

                       switch (currentState) {
                           case STOPPING -> {
                               System.out.println(" -> Перезапуск программы...");
                               abstractProgram.wait(500);
                               abstractProgram.startRandomThread();
                           }
                           case FATAL_ERROR -> {
                               System.out.println(" -> Программа завершена с критической ошибкой!");
                               stop();
                               return;
                           }
                           case RUNNING -> System.out.println(" -> программа работает нормально.");
                           case UNKNOWN -> System.out.println(" -> неизвестное состояние.");
                       }
                   } catch (InterruptedException e) {
                       Thread.currentThread().interrupt(); // Флаг прерывания
                       break;
                   }
               }
           }
        });

        supervisorThread.start();
    }
}

public class Main {
    public static void main(String[] args) {
        AbstractProgram abstractProgram = new AbstractProgram();
        Supervisor supervisor = new Supervisor(abstractProgram);

        supervisor.start();

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        supervisor.stop();

        System.out.println("-------");
        System.out.println("Программа завершена");
    }
}