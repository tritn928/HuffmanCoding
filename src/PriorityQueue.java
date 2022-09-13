import java.util.Iterator;
import java.util.LinkedList;

/*  Student information for assignment:
*
*  On MY honor, Tri Nguyen, this programming assignment is MY own work
*  and I have not provided this code to any other student.
*
*  Number of slip days used: 2
*
*  Student 1 (Student whose turnin account is being used)
*  UTEID: ttn2797
*  email address: tritn928@gmail.com
*  Grader name: Nina
*
*/

/**
 * Priority Queue using a linked list as the storage data structure. Elements
 * enqueued to the priority queue are sorted by their priority. The front of the
 * queue contains the item with the highest priority.
 */
public class PriorityQueue<E extends Comparable<? super E>> {

	// LinkedList container, front of the list is the front of the queue
	private LinkedList<E> queue;

	// Constructor for an empty queue
	public PriorityQueue() {
		queue = new LinkedList<E>();
	}

	// Enqueues an element into the priority queue.
	// pre: data != null
	public void enqueue(E data) {
		if (data == null)
			throw new IllegalArgumentException("PriorityQueue enqueue error. data is null");

		int indexToAdd = 0;
		Iterator<E> it = queue.iterator();
		while (it.hasNext()) {
			// find the spot to add in to queue
			int compareVal = data.compareTo(it.next());
			if (compareVal >= 0) {
				indexToAdd++;
			}
		}
		queue.add(indexToAdd, data);
	}

	// Returns the highest priority element of the queue or null if empty
	// Does not remove the item
	public E peek() {
		if (size() == 0)
			return null;
		return queue.getFirst();
	}

	// Removes and returns the highest priority element of the queue
	// pre: size > 0
	public E dequeue() {
		if (size() == 0)
			return null;
		return queue.removeFirst();
	}
	
	// Returns the size of the queue
	public int size() {
		return queue.size();
	}
}
