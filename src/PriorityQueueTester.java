import java.util.Arrays;
import java.util.Random;

public class PriorityQueueTester {

	public static void main(String[] args) {
		final int ITEMS_TO_TEST_WITH = 500;
		Item[] items = new Item[ITEMS_TO_TEST_WITH];
		Random rand = new Random();
        for (int i = 0; i < items.length; i++) {
            // by pigeon hole conjecture, there will be some elements with the same priority
            items[i] = new Item(rand.nextInt(items.length - 10));
        }
        PriorityQueue<Item> p = new PriorityQueue<>();
        assert p.dequeue() == null : "dequeueing empty queue should return null";
        // enqueue a single element and ensure dequeueing works as expected
        p.enqueue(items[0]);
        assert p.dequeue() == items[0] : "dequeueing should return first element";
        assert p.dequeue() == null : "dequeueing after exhaust should return null";
        
        for (int i = 0; i < items.length; i++) {
            p.enqueue(items[i]);
        }
        Arrays.sort(items);
        for (int i = 0; i < items.length; i++) {
            assert p.dequeue() == items[i] : "dequeue order should be equal to a stable sort";
        }
        assert p.dequeue() == null : "dequeueing after exhaust should return null";
        System.out.println("Priority queue tests successful.");
	}
	
	private static class Item implements Comparable<Item> {
        int priority;

        Item(int priority) {
            this.priority = priority;
        }

        public int compareTo(Item o) {
            return priority - o.priority;
        };
    }

}
