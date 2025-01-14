import java.util.LinkedList;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A simple work queue implementation based on the IBM developerWorks article by
 * Brian Goetz. It is up to the user of this class to keep track of whether
 * there is any pending work remaining.
 *
 * @see <a href="https://www.ibm.com/developerworks/library/j-jtp0730/">
 * Java Theory and Practice: Thread Pools and Work Queues</a>
 */
public class WorkQueue {

	/**
	 * Pool of worker threads that will wait in the background until work is
	 * available.
	 */
	private final Worker[] workers;

	/** Queue of pending work requests. */
	private final LinkedList<Runnable> queue;

	/** Used to signal the queue should be shutdown. */
	private volatile boolean shutdown;

	/** The default number of threads to use when not specified. */
	public static final int DEFAULT = 5;

	/** Logger used for this class. */
	private static final Logger log = LogManager.getLogger();
	
	/**
	 * Starts a work queue with the default number of threads.
	 *
	 * @see #WorkQueue(int)
	 */
	public WorkQueue() {
		this(DEFAULT);
	}

	/**
	 * Starts a work queue with the specified number of threads.
	 *
	 * @param threads number of worker threads; should be greater than 1
	 */
	public WorkQueue(int threads) {
		this.queue = new LinkedList<Runnable>();
		this.workers = new Worker[threads];

		shutdown = false;

		// start the threads so they are waiting in the background
		for (int i = 0; i < threads; i++) {
			workers[i] = new Worker();
			workers[i].start();
		}
		
		log.debug("Work queue initialized with {} worker threads.", workers.length);
	}

	/**
	 * Adds a work request to the queue. A thread will process this request when
	 * available.
	 *
	 * @param task work request (in the form of a {@link Runnable} object)
	 */
	public void execute(Runnable task) {
		synchronized (queue) {
			queue.addLast(task);
			queue.notifyAll();
		}
	}

	/**
	 * Asks the queue to shutdown. Any unprocessed work will not be finished, but
	 * threads in-progress will not be interrupted.
	 */
	public void shutdown() {
		// safe to do unsynchronized due to volatile keyword
		shutdown = true;

		log.debug("Work queue triggering shutdown...");
		synchronized (queue) {
			queue.notifyAll();
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
	private class Worker extends Thread {
		/**
		 * Initializes a worker thread with a custom name.
		 */
		public Worker() {
			setName("Worker" + getName());
		}

		@Override
		public void run() {
			Runnable task = null;

			try {
				while (true) {
					synchronized (queue) {
						while (queue.isEmpty() && !shutdown) {
							log.debug("Work queue worker waiting...");
							queue.wait();
						}
	
						// exit while for one of two reasons:
						// (a) queue has work, or (b) shutdown has been called
	
						if (shutdown) {
							log.debug("Work queue worker shutting down...");
							break;
						}
						else {
							task = queue.removeFirst();
						}
					}
	
					try {
						log.debug("Work queue worker found work.");
						task.run();
					}
					catch (RuntimeException e) {
						// catch runtime exceptions to avoid leaking threads
						System.err.println("Warning: Work queue encountered an exception while running.");
						log.catching(Level.DEBUG, e);
					}
				}
			}
			catch (InterruptedException e) {
				// interrupts will cause the workers to terminate immediately,
				// regardless of whether all work was completed
				System.err.println("Warning: Work queue interrupted while waiting.");
				log.catching(Level.DEBUG, e);
				Thread.currentThread().interrupt();
			}
		}
	}
}
