import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CustomReadWriteLock {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition canWrite = lock.newCondition();
    private final Condition canRead = lock.newCondition();
    private int readers = 0;
    private boolean isWriting = false;

    public void readLock() throws InterruptedException {
        lock.lock();
        try {
            while (isWriting) {
                canRead.await();
            }
            readers++;
        } finally {
            lock.unlock();
        }
    }

    public void readUnlock() {
        lock.lock();
        try {
            readers--;
            if (readers == 0) {
                canWrite.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    public void writeLock() throws InterruptedException {
        lock.lock();
        try {
            while (isWriting || readers > 0) {
                canWrite.await();
            }
            isWriting = true;
        } finally {
            lock.unlock();
        }
    }

    public void writeUnlock() {
        lock.lock();
        try {
            isWriting = false;
            canWrite.signal(); // Prioriza escritores
            canRead.signalAll(); // Notifica leitores
        } finally {
            lock.unlock();
        }
    }
}