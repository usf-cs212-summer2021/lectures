import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple work queue implementation based on the IBM developerWorks article by
 * Brian Goetz. This version uses locks and conditions from the concurrent
 * package. However, it is still up to the user of this class to keep track of
 * whether there is any pending work remaining.
 *
 * @see <a href="https://www.ibm.com/developerworks/library/j-jtp0730/">
 * Java Theory and Practice: Thread Pools and Work Queues</a>
 */
public class ConcurrentWorkQueue {

	/**
	 * Pool of worker threads that will wait in the background until work is
	 * available.
	 */
	private final PoolWorker[] workers;

	/** Queue of pending work requests. */
	private final LinkedList<Runnable> queue;

	/** Used to signal the queue should be shutdown. */
	private volatile boolean shutdown;

	/** The default number of threads to use when not specified. */
	public static final int DEFAULT = 5;

	/** The lock object to protect access to the queue. */
	private final Lock lock;

	/** The condition controlling when threads are active. */
	private final Condition hasWork;

	/**
	 * Starts a work queue with the default number of threads.
	 *
	 * @see #ConcurrentWorkQueue(int)
	 */
	public ConcurrentWorkQueue() {
		this(DEFAULT);
	}

	/**
	 * Starts a work queue with the specified number of threads.
	 *
	 * @param threads number of worker threads; should be greater than 1
	 */
	public ConcurrentWorkQueue(int threads) {
		this.queue = new LinkedList<Runnable>();
		this.workers = new PoolWorker[threads];

		shutdown = false;

		lock = new ReentrantLock();
		hasWork = lock.newCondition();

		// start the threads so they are waiting in the background
		for (int i = 0; i < threads; i++) {
			workers[i] = new PoolWorker();
			workers[i].start();
		}
	}

	/**
	 * Adds a work request to the queue. A thread will process this request when
	 * available.
	 *
	 * @param r work request (in the form of a {@link Runnable} object)
	 */
	public void execute(Runnable r) {
		lock.lock();

		try {
			queue.addLast(r);
			hasWork.signalAll();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Asks the queue to shutdown. Any unprocessed work will not be finished, but
	 * threads in-progress will not be interrupted.
	 */
	public void shutdown() {
		// safe to do unsynchronized due to volatile keyword
		shutdown = true;

		// still need to signal our threads to wake them up
		lock.lock();

		try {
			hasWork.signalAll();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Returns the number of worker threads being used by the work queue.
	 *
	 * @return number of worker threads
	 */
	public int size() {
		return workers.length;
	}

	/**
	 * Waits until work is available in the work queue. When work is found, will
	 * remove the work from the queue and run it. If a shutdown is detected, will
	 * exit instead of grabbing new work from the queue. These threads will
	 * continue running in the background until a shutdown is requested.
	 */
	private class PoolWorker extends Thread {

		@Override
		public void run() {
			Runnable r = null;

			try {
				while (true) {
					lock.lock();

					try {
						while (queue.isEmpty() && !shutdown) {
							hasWork.await();
						}
		
						// exit while for one of two reasons:
						// (a) queue has work, or (b) shutdown has been called
	
						if (shutdown) {
							break;
						}
						else {
							r = queue.removeFirst();
						}
					}
					finally {
						lock.unlock();
					}
	
					try {
						r.run();
					}
					catch (RuntimeException ex) {
						// catch runtime exceptions to avoid leaking threads
						System.err.println("Warning: Work queue encountered an exception while running.");
					}
				}
			}
			catch (InterruptedException e) {
				System.err.println("Warning: Work queue interrupted while waiting.");
				Thread.currentThread().interrupt();
			}
		}
	}
}
