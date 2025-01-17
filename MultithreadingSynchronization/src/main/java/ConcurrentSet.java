import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * A thread-safe version of {@link IndexedSet} using a read/write lock.
 *
 * @param <E> element type
 * @see IndexedSet
 * @see ReadWriteLock
 * @see ReentrantReadWriteLock
 */
public class ConcurrentSet<E> extends IndexedSet<E> {

	/** The lock object to use. */
	private ReentrantReadWriteLock lock;

	/**
	 * Initializes an unsorted synchronized indexed set.
	 */
	public ConcurrentSet() {
		this(false);
	}

	/**
	 * Initializes a sorted or unsorted synchronized index set depending on the
	 * parameter.
	 *
	 * @param sorted if true, will initialize a sorted set
	 */
	public ConcurrentSet(boolean sorted) {
		super(sorted);

		lock = new ReentrantReadWriteLock();
	}

	@Override
	public boolean add(E element) {
		lock.writeLock().lock();

		try {
			return super.add(element);
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public boolean addAll(Collection<E> elements) {
		lock.writeLock().lock();

		try {
			return super.addAll(elements);
		}
		finally {
			lock.writeLock().unlock();
		}
	}
	
	@Override
	public boolean addAll(IndexedSet<E> elements) {
		lock.writeLock().lock();

		try {
			return super.addAll(elements);
		}
		finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public int size() {
		lock.readLock().lock();

		try {
			return super.size();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public boolean contains(E element) {
		lock.readLock().lock();

		try {
			return super.contains(element);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public E get(int index) {
		lock.readLock().lock();

		try {
			return super.get(index);
		}
		finally {
			lock.readLock().unlock();
		}
	}
	
	@Override
	public E first() throws NoSuchElementException {
		lock.readLock().lock();

		try {
			return super.first();
		}
		finally {
			lock.readLock().unlock();
		}
	}
	
	@Override
	public E last() throws NoSuchElementException {
		lock.readLock().lock();

		try {
			return super.last();
		}
		finally {
			lock.readLock().unlock();
		}
	}	

	@Override
	public IndexedSet<E> copy(boolean sorted) {
		lock.readLock().lock();
	
		try {
			return super.copy(sorted);
		}
		finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public String toString() {
		lock.readLock().lock();

		try {
			return super.toString();
		}
		finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Demonstrates this class.
	 * 
	 * @param args unused
	 */
	public static void main(String[] args) {
		Method[] singleMethods = IndexedSet.class.getDeclaredMethods();
		Method[] threadMethods = ConcurrentSet.class.getDeclaredMethods();
				
		List<String> expected = Arrays.stream(singleMethods)
				.filter(method -> Modifier.isPublic(method.getModifiers()))
				.filter(method -> !Modifier.isStatic(method.getModifiers()))
				.map(method -> method.getName())
				.sorted()
				.collect(Collectors.toList());

		List<String> actual = Arrays.stream(threadMethods)
				.filter(method -> Modifier.isPublic(method.getModifiers()))
				.filter(method -> !Modifier.isStatic(method.getModifiers()))
				.map(method -> method.getName())
				.sorted()
				.collect(Collectors.toList());
		
		System.out.println("Original Methods:");
		System.out.println(expected);
		
		System.out.println();
		System.out.println("Overridden Methods:");
		System.out.println(actual);
	}	
}
