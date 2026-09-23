package interview_prep_mastery._05_stacks_and_queues;

import java.util.*;

/**
 * ============================================================================
 * MODULE 05: STACKS & QUEUES - IMPLEMENTATIONS
 * Topics: Array/LL Stack, Queue, Queue using Stacks, Stack using Queues,
 * Two Stacks in one Array, Min Stack in O(1).
 * ============================================================================
 */
public class StackQueueImplementations {

    // ------------------------------------------------------------------------
    // 1. Array-Based Stack
    // ------------------------------------------------------------------------
    public static class ArrayStack {
        private int[] arr;
        private int top;
        private int capacity;

        public ArrayStack(int capacity) {
            this.capacity = capacity;
            this.arr = new int[capacity];
            this.top = -1;
        }

        public void push(int val) {
            if (top == capacity - 1) throw new IllegalStateException("Stack Overflow");
            arr[++top] = val;
        }

        public int pop() {
            if (isEmpty()) throw new EmptyStackException();
            return arr[top--];
        }

        public int peek() {
            if (isEmpty()) throw new EmptyStackException();
            return arr[top];
        }

        public boolean isEmpty() {
            return top == -1;
        }

        public int size() {
            return top + 1;
        }
    }

    // ------------------------------------------------------------------------
    // 2. Circular Array-Based Queue
    // ------------------------------------------------------------------------
    public static class ArrayQueue {
        private int[] arr;
        private int front, rear, count, capacity;

        public ArrayQueue(int capacity) {
            this.capacity = capacity;
            this.arr = new int[capacity];
            this.front = 0;
            this.rear = -1;
            this.count = 0;
        }

        public void offer(int val) {
            if (count == capacity) throw new IllegalStateException("Queue Overflow");
            rear = (rear + 1) % capacity;
            arr[rear] = val;
            count++;
        }

        public int poll() {
            if (isEmpty()) throw new NoSuchElementException("Queue is empty");
            int val = arr[front];
            front = (front + 1) % capacity;
            count--;
            return val;
        }

        public int peek() {
            if (isEmpty()) throw new NoSuchElementException("Queue is empty");
            return arr[front];
        }

        public boolean isEmpty() {
            return count == 0;
        }

        public int size() {
            return count;
        }
    }

    // ------------------------------------------------------------------------
    // 3. Implement Queue Using Stacks (Amortized O(1))
    // ------------------------------------------------------------------------
    public static class QueueUsingStacks {
        private Stack<Integer> inStack = new Stack<>();
        private Stack<Integer> outStack = new Stack<>();

        public void offer(int x) {
            inStack.push(x);
        }

        public int poll() {
            shiftStacks();
            if (outStack.isEmpty()) throw new NoSuchElementException();
            return outStack.pop();
        }

        public int peek() {
            shiftStacks();
            if (outStack.isEmpty()) throw new NoSuchElementException();
            return outStack.peek();
        }

        public boolean isEmpty() {
            return inStack.isEmpty() && outStack.isEmpty();
        }

        private void shiftStacks() {
            if (outStack.isEmpty()) {
                while (!inStack.isEmpty()) {
                    outStack.push(inStack.pop());
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // 4. Implement Stack Using Queues
    // ------------------------------------------------------------------------
    public static class StackUsingQueues {
        private Queue<Integer> queue = new LinkedList<>();

        public void push(int x) {
            queue.offer(x);
            int size = queue.size();
            // Rotate previous elements behind x
            for (int i = 0; i < size - 1; i++) {
                queue.offer(queue.poll());
            }
        }

        public int pop() {
            if (queue.isEmpty()) throw new EmptyStackException();
            return queue.poll();
        }

        public int peek() {
            if (queue.isEmpty()) throw new EmptyStackException();
            return queue.peek();
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    // ------------------------------------------------------------------------
    // 5. Challenge 2: Implement Two Stacks Using One Array
    // ------------------------------------------------------------------------
    /**
     * Stack 1 grows from index 0 -> right.
     * Stack 2 grows from index capacity - 1 -> left.
     */
    public static class TwoStacks {
        private int[] arr;
        private int top1;
        private int top2;
        private int capacity;

        public TwoStacks(int capacity) {
            this.capacity = capacity;
            this.arr = new int[capacity];
            this.top1 = -1;
            this.top2 = capacity;
        }

        public void push1(int val) {
            if (top1 < top2 - 1) {
                arr[++top1] = val;
            } else {
                throw new IllegalStateException("Stack 1 Overflow");
            }
        }

        public void push2(int val) {
            if (top1 < top2 - 1) {
                arr[--top2] = val;
            } else {
                throw new IllegalStateException("Stack 2 Overflow");
            }
        }

        public int pop1() {
            if (top1 >= 0) {
                return arr[top1--];
            }
            throw new EmptyStackException();
        }

        public int pop2() {
            if (top2 < capacity) {
                return arr[top2++];
            }
            throw new EmptyStackException();
        }
    }

    // ------------------------------------------------------------------------
    // 6. Challenge 10: MinStack in O(1) Time & O(1) Extra Space (Value Encoding)
    // ------------------------------------------------------------------------
    /**
     * Invariant: If pushed val < min, store 2 * val - min into stack and update min = val.
     * On pop, if top < min, original val was min, and restored min = 2 * min - top.
     */
    public static class MinStack {
        private Stack<Long> stack = new Stack<>();
        private long min;

        public void push(int val) {
            if (stack.isEmpty()) {
                stack.push((long) val);
                min = val;
            } else if (val < min) {
                // Encode previous min
                stack.push(2L * val - min);
                min = val;
            } else {
                stack.push((long) val);
            }
        }

        public void pop() {
            if (stack.isEmpty()) throw new EmptyStackException();
            long top = stack.pop();
            if (top < min) {
                // Restore previous min
                min = 2 * min - top;
            }
        }

        public int top() {
            if (stack.isEmpty()) throw new EmptyStackException();
            long top = stack.peek();
            if (top < min) {
                return (int) min;
            }
            return (int) top;
        }

        public int getMin() {
            if (stack.isEmpty()) throw new EmptyStackException();
            return (int) min;
        }
    }

    // ------------------------------------------------------------------------
    // Test Suite for Implementations
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING STACK & QUEUE IMPLEMENTATIONS TEST ");
        System.out.println("=================================================");

        // 1. Array Stack
        ArrayStack stack = new ArrayStack(5);
        stack.push(10); stack.push(20);
        assert stack.pop() == 20;
        assert stack.peek() == 10;

        // 2. Array Queue
        ArrayQueue queue = new ArrayQueue(5);
        queue.offer(1); queue.offer(2); queue.offer(3);
        assert queue.poll() == 1;
        assert queue.peek() == 2;

        // 3. Queue using Stacks
        QueueUsingStacks qStacks = new QueueUsingStacks();
        qStacks.offer(100); qStacks.offer(200);
        assert qStacks.poll() == 100;
        assert qStacks.peek() == 200;

        // 4. Stack using Queues
        StackUsingQueues sQueues = new StackUsingQueues();
        sQueues.push(5); sQueues.push(10);
        assert sQueues.pop() == 10;
        assert sQueues.peek() == 5;

        // 5. Two Stacks in One Array
        TwoStacks twoStacks = new TwoStacks(6);
        twoStacks.push1(1); twoStacks.push1(2);
        twoStacks.push2(9); twoStacks.push2(8);
        assert twoStacks.pop1() == 2;
        assert twoStacks.pop2() == 8;

        // 6. Min Stack
        MinStack minStack = new MinStack();
        minStack.push(3);
        minStack.push(5);
        assert minStack.getMin() == 3;
        minStack.push(2);
        minStack.push(1);
        assert minStack.getMin() == 1;
        minStack.pop();
        assert minStack.getMin() == 2;
        minStack.pop();
        assert minStack.getMin() == 3;

        System.out.println(" STACK & QUEUE IMPLEMENTATIONS ALL PASSED!");
        System.out.println("=================================================");
    }
}
