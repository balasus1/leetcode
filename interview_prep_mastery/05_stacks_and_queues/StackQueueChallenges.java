package interview_prep_mastery._05_stacks_and_queues;

import java.util.*;

/**
 * ============================================================================
 * MODULE 05: STACKS & QUEUES - CHALLENGES
 * Topics:
 * - Challenge 1: Generate Binary Numbers from 1 to n using Queue
 * - Challenge 3: Reversing First k Elements of a Queue
 * - Challenge 5: Sort Values in a Stack
 * - Challenge 6: Evaluate Postfix Expressions using Stacks
 * - Challenge 7: Next Greater Element using Stack
 * - Challenge 8: Celebrity Problem using a Stack
 * - Challenge 9: Check for Balanced Parentheses using a Stack
 * ============================================================================
 */
public class StackQueueChallenges {

    // ------------------------------------------------------------------------
    // Challenge 1: Generate Binary Numbers from 1 to n using a Queue (BFS)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n)
     * - Space: O(n) for resulting array
     */
    public static String[] findBin(int n) {
        if (n <= 0) return new String[0];
        String[] result = new String[n];
        Queue<String> queue = new LinkedList<>();
        queue.offer("1");

        for (int i = 0; i < n; i++) {
            String curr = queue.poll();
            result[i] = curr;
            queue.offer(curr + "0");
            queue.offer(curr + "1");
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Challenge 3: Reversing the First k Elements of a Queue
    // ------------------------------------------------------------------------
    /**
     * Algorithm:
     * 1. Push first k elements onto stack.
     * 2. Enqueue popped stack elements to tail.
     * 3. Dequeue remaining (size - k) elements from front and enqueue to tail.
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(k) for stack
     */
    public static void reverseK(Queue<Integer> queue, int k) {
        if (queue == null || k <= 0 || k > queue.size()) return;
        Stack<Integer> stack = new Stack<>();

        for (int i = 0; i < k; i++) {
            stack.push(queue.poll());
        }
        while (!stack.isEmpty()) {
            queue.offer(stack.pop());
        }
        int remaining = queue.size() - k;
        for (int i = 0; i < remaining; i++) {
            queue.offer(queue.poll());
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 5: Sort the Values in a Stack
    // ------------------------------------------------------------------------
    /**
     * Using an auxiliary temporary stack:
     *
     * Complexity:
     * - Time: O(n^2)
     * - Space: O(n) for temp stack
     */
    public static void sortStack(Stack<Integer> stack) {
        if (stack == null || stack.size() <= 1) return;
        Stack<Integer> tempStack = new Stack<>();

        while (!stack.isEmpty()) {
            int current = stack.pop();
            while (!tempStack.isEmpty() && tempStack.peek() > current) {
                stack.push(tempStack.pop());
            }
            tempStack.push(current);
        }

        // Transfer back so smallest is on top
        while (!tempStack.isEmpty()) {
            stack.push(tempStack.pop());
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 6: Evaluate Postfix Expressions using Stacks
    // ------------------------------------------------------------------------
    /**
     * Example: "9 2 1 * - 8 - 4 +" -> 9 - (2*1) - 8 + 4 = 3
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(n)
     */
    public static int evaluatePostfix(String expression) {
        if (expression == null || expression.trim().isEmpty()) return 0;
        Stack<Integer> stack = new Stack<>();
        String[] tokens = expression.trim().split("\\s+");

        for (String token : tokens) {
            if (isOperator(token)) {
                int val2 = stack.pop();
                int val1 = stack.pop();
                stack.push(applyOp(token.charAt(0), val1, val2));
            } else {
                stack.push(Integer.parseInt(token));
            }
        }
        return stack.pop();
    }

    private static boolean isOperator(String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/");
    }

    private static int applyOp(char op, int a, int b) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/': return a / b;
            default: return 0;
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 7: Next Greater Element using Monotonic Stack
    // ------------------------------------------------------------------------
    /**
     * Monotonic decreasing stack traversed backwards.
     *
     * Complexity:
     * - Time: O(n) (Each index pushed and popped at most once)
     * - Space: O(n)
     */
    public static int[] nextGreaterElement(int[] arr) {
        if (arr == null) return new int[0];
        int n = arr.length;
        int[] result = new int[n];
        Stack<Integer> stack = new Stack<>();

        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && stack.peek() <= arr[i]) {
                stack.pop();
            }
            result[i] = stack.isEmpty() ? -1 : stack.peek();
            stack.push(arr[i]);
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Challenge 8: Solve a Celebrity Problem using a Stack
    // ------------------------------------------------------------------------
    /**
     * Celebrity Definition: Knows nobody, but everybody knows them.
     * Matrix party[i][j] = 1 if i knows j, else 0.
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(n) for elimination stack
     */
    public static int findCelebrity(int[][] party, int numPeople) {
        if (party == null || numPeople <= 0) return -1;
        Stack<Integer> stack = new Stack<>();
        for (int i = 0; i < numPeople; i++) {
            stack.push(i);
        }

        // Step 1: Elimination phase
        while (stack.size() > 1) {
            int a = stack.pop();
            int b = stack.pop();
            if (party[a][b] == 1) {
                // a knows b => a cannot be celebrity, b might be
                stack.push(b);
            } else {
                // a does NOT know b => b cannot be celebrity, a might be
                stack.push(a);
            }
        }

        if (stack.isEmpty()) return -1;
        int candidate = stack.pop();

        // Step 2: Verification phase
        for (int i = 0; i < numPeople; i++) {
            if (i != candidate) {
                // Candidate should not know i, and i must know candidate
                if (party[candidate][i] == 1 || party[i][candidate] == 0) {
                    return -1;
                }
            }
        }
        return candidate;
    }

    // ------------------------------------------------------------------------
    // Challenge 9: Check for Balanced Parentheses using a Stack
    // ------------------------------------------------------------------------
    public static boolean isBalanced(String exp) {
        if (exp == null) return true;
        Stack<Character> stack = new Stack<>();
        for (char c : exp.toCharArray()) {
            if (c == '(' || c == '{' || c == '[') {
                stack.push(c);
            } else if (c == ')' || c == '}' || c == ']') {
                if (stack.isEmpty()) return false;
                char top = stack.pop();
                if ((c == ')' && top != '(') ||
                    (c == '}' && top != '{') ||
                    (c == ']' && top != '[')) {
                    return false;
                }
            }
        }
        return stack.isEmpty();
    }

    // ------------------------------------------------------------------------
    // Test Suite for Stack & Queue Challenges
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING STACK & QUEUE CHALLENGES TEST SUITE ");
        System.out.println("=================================================");

        // 1. Binary numbers 1 to n
        String[] bins = findBin(5);
        assert Arrays.equals(bins, new String[]{"1", "10", "11", "100", "101"});

        // 2. Reverse first k elements of queue
        Queue<Integer> q = new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        reverseK(q, 5);
        assert new ArrayList<>(q).equals(Arrays.asList(5, 4, 3, 2, 1, 6, 7, 8, 9, 10));

        // 3. Sort values in stack
        Stack<Integer> st = new Stack<>();
        st.push(23); st.push(60); st.push(12); st.push(42); st.push(4);
        sortStack(st);
        assert st.pop() == 4 && st.pop() == 12 && st.pop() == 23;

        // 4. Postfix Evaluation
        assert evaluatePostfix("9 2 1 * - 8 - 4 +") == 3;

        // 5. Next Greater Element
        int[] nge = nextGreaterElement(new int[]{4, 5, 2, 25});
        assert Arrays.equals(nge, new int[]{5, 25, 25, -1});

        // 6. Celebrity Problem
        int[][] party = {
                {0, 1, 1, 0},
                {1, 0, 1, 1},
                {0, 0, 0, 0}, // Person 2 knows nobody
                {0, 1, 1, 0}
        };
        assert findCelebrity(party, 4) == 2;

        // 7. Balanced Parentheses
        assert isBalanced("{[()]}");
        assert !isBalanced("{[(])}");

        System.out.println(" STACK & QUEUE CHALLENGES ALL PASSED!");
        System.out.println("=================================================");
    }
}
