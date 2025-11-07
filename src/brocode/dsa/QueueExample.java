package brocode.dsa;

import java.util.LinkedList;
import java.util.Queue;

public class QueueExample {
    static void main(String[] args) {
        Queue<String> queue = new LinkedList<>();
        queue.offer("a");
        queue.offer("b");
        queue.offer("c");
        System.out.println(queue.poll());
        System.out.println(queue.contains("c"));
    }
}
