package interview_prep_mastery._03_linked_lists;

import java.util.*;

/**
 * ============================================================================
 * MODULE 03: LINKED LISTS - ADVANCED CHALLENGES
 * Topics: Intersection Point, Rotate List, Reverse Alt K Nodes, Add Two Numbers,
 * Reverse Sub-list, Reverse every K-element Sub-list, Merge K Sorted Lists,
 * Kth Smallest in M Sorted Lists.
 * ============================================================================
 */
public class AdvancedLinkedListChallenges {

    public static class ListNode {
        public int val;
        public ListNode next;

        public ListNode(int val) {
            this.val = val;
            this.next = null;
        }
    }

    // ------------------------------------------------------------------------
    // 1. Intersection Point of Two Lists
    // ------------------------------------------------------------------------
    /**
     * Two-Pointer length alignment:
     * When ptrA reaches end, switch to headB; when ptrB reaches end, switch to headA.
     * They meet at intersection node or both become null in at most (L1 + L2) steps.
     *
     * Complexity:
     * - Time: O(m + n)
     * - Space: O(1)
     */
    public static ListNode getIntersectionNode(ListNode headA, ListNode headB) {
        if (headA == null || headB == null) return null;
        ListNode pA = headA;
        ListNode pB = headB;
        while (pA != pB) {
            pA = (pA == null) ? headB : pA.next;
            pB = (pB == null) ? headA : pB.next;
        }
        return pA;
    }

    // ------------------------------------------------------------------------
    // 2. Rotate a Linked List by K Places
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n)
     * - Space: O(1)
     */
    public static ListNode rotateRight(ListNode head, int k) {
        if (head == null || head.next == null || k <= 0) return head;
        int length = 1;
        ListNode tail = head;
        while (tail.next != null) {
            tail = tail.next;
            length++;
        }
        k = k % length;
        if (k == 0) return head;

        // Make circular temporarily
        tail.next = head;
        int stepsToNewTail = length - k;
        ListNode newTail = head;
        for (int i = 1; i < stepsToNewTail; i++) {
            newTail = newTail.next;
        }
        ListNode newHead = newTail.next;
        newTail.next = null; // Break circle
        return newHead;
    }

    // ------------------------------------------------------------------------
    // 3. Reverse Alternative K Nodes in a Singly Linked List
    // ------------------------------------------------------------------------
    /**
     * Reverses k nodes, skips next k nodes, recursively or iteratively.
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(1)
     */
    public static ListNode reverseAlternateKNodes(ListNode head, int k) {
        if (head == null || k <= 1) return head;
        ListNode curr = head;
        ListNode prev = null;
        ListNode next = null;
        int count = 0;

        // Reverse first k nodes
        while (curr != null && count < k) {
            next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
            count++;
        }

        // Now head is the tail of reversed sub-list
        if (head != null) {
            head.next = curr;
        }

        // Skip next k nodes
        count = 0;
        while (curr != null && count < k - 1) {
            curr = curr.next;
            count++;
        }

        // Recursively call for remaining list
        if (curr != null) {
            curr.next = reverseAlternateKNodes(curr.next, k);
        }

        return prev;
    }

    // ------------------------------------------------------------------------
    // 4. Add Two Integers Represented by Linked Lists
    // ------------------------------------------------------------------------
    /**
     * Digits stored in reverse order (e.g. 2 -> 4 -> 3 represents 342).
     *
     * Complexity:
     * - Time: O(max(N, M))
     * - Space: O(max(N, M)) for resulting list
     */
    public static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;
        int carry = 0;

        while (l1 != null || l2 != null || carry != 0) {
            int sum = carry;
            if (l1 != null) {
                sum += l1.val;
                l1 = l1.next;
            }
            if (l2 != null) {
                sum += l2.val;
                l2 = l2.next;
            }
            carry = sum / 10;
            curr.next = new ListNode(sum % 10);
            curr = curr.next;
        }
        return dummy.next;
    }

    // ------------------------------------------------------------------------
    // 5. Reverse a Sub-list (from position left to right, 1-indexed)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n)
     * - Space: O(1)
     */
    public static ListNode reverseBetween(ListNode head, int left, int right) {
        if (head == null || left >= right) return head;
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode prev = dummy;

        for (int i = 1; i < left; i++) {
            prev = prev.next;
        }

        ListNode curr = prev.next;
        for (int i = 0; i < right - left; i++) {
            ListNode next = curr.next;
            curr.next = next.next;
            next.next = prev.next;
            prev.next = next;
        }
        return dummy.next;
    }

    // ------------------------------------------------------------------------
    // 6. Reverse Every K-element Sub-list (K-Group Reversal)
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n)
     * - Space: O(1)
     */
    public static ListNode reverseKGroup(ListNode head, int k) {
        if (head == null || k <= 1) return head;
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        ListNode groupPrev = dummy;

        while (true) {
            ListNode kth = getKthNode(groupPrev, k);
            if (kth == null) break;
            ListNode groupNext = kth.next;

            // Reverse group
            ListNode prev = groupNext;
            ListNode curr = groupPrev.next;
            while (curr != groupNext) {
                ListNode tmp = curr.next;
                curr.next = prev;
                prev = curr;
                curr = tmp;
            }

            ListNode tmp = groupPrev.next;
            groupPrev.next = kth;
            groupPrev = tmp;
        }
        return dummy.next;
    }

    private static ListNode getKthNode(ListNode curr, int k) {
        while (curr != null && k > 0) {
            curr = curr.next;
            k--;
        }
        return curr;
    }

    // ------------------------------------------------------------------------
    // 7. Merge K Sorted Lists
    // ------------------------------------------------------------------------
    /**
     * Min-Heap approach:
     * Time: O(N log K) where N is total nodes and K is number of lists.
     * Space: O(K) for priority queue.
     */
    public static ListNode mergeKLists(ListNode[] lists) {
        if (lists == null || lists.length == 0) return null;
        PriorityQueue<ListNode> minHeap = new PriorityQueue<>(Comparator.comparingInt(a -> a.val));

        for (ListNode node : lists) {
            if (node != null) {
                minHeap.offer(node);
            }
        }

        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;
        while (!minHeap.isEmpty()) {
            ListNode smallest = minHeap.poll();
            curr.next = smallest;
            curr = curr.next;
            if (smallest.next != null) {
                minHeap.offer(smallest.next);
            }
        }
        return dummy.next;
    }

    // ------------------------------------------------------------------------
    // 8. Kth Smallest Number in M Sorted Lists
    // ------------------------------------------------------------------------
    public static class NodeElement {
        int val;
        int listIndex;
        int elementIndex;

        public NodeElement(int val, int listIndex, int elementIndex) {
            this.val = val;
            this.listIndex = listIndex;
            this.elementIndex = elementIndex;
        }
    }

    /**
     * Time: O(K log M) where M is number of sorted lists
     * Space: O(M) for min-heap
     */
    public static int findKthSmallestInMSortedLists(List<List<Integer>> lists, int k) {
        if (lists == null || lists.isEmpty() || k <= 0) return -1;
        PriorityQueue<NodeElement> minHeap = new PriorityQueue<>(Comparator.comparingInt(a -> a.val));

        for (int i = 0; i < lists.size(); i++) {
            if (lists.get(i) != null && !lists.get(i).isEmpty()) {
                minHeap.offer(new NodeElement(lists.get(i).get(0), i, 0));
            }
        }

        int count = 0;
        int result = -1;
        while (!minHeap.isEmpty()) {
            NodeElement curr = minHeap.poll();
            result = curr.val;
            count++;
            if (count == k) return result;

            if (curr.elementIndex + 1 < lists.get(curr.listIndex).size()) {
                minHeap.offer(new NodeElement(
                        lists.get(curr.listIndex).get(curr.elementIndex + 1),
                        curr.listIndex,
                        curr.elementIndex + 1
                ));
            }
        }
        return result;
    }

    // Helpers
    public static ListNode buildList(int[] values) {
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;
        for (int val : values) {
            curr.next = new ListNode(val);
            curr = curr.next;
        }
        return dummy.next;
    }

    public static List<Integer> toJavaList(ListNode head) {
        List<Integer> result = new ArrayList<>();
        ListNode curr = head;
        while (curr != null) {
            result.add(curr.val);
            curr = curr.next;
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Test Suite for Advanced Linked Lists
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING ADVANCED LINKED LIST TEST SUITE ");
        System.out.println("=================================================");

        // 1. Intersection of Two Lists
        ListNode common = buildList(new int[]{8, 4, 5});
        ListNode headA = buildList(new int[]{4, 1});
        headA.next.next = common;
        ListNode headB = buildList(new int[]{5, 6, 1});
        headB.next.next.next = common;
        assert getIntersectionNode(headA, headB) == common;

        // 2. Rotate List
        ListNode rotList = buildList(new int[]{1, 2, 3, 4, 5});
        ListNode rotated = rotateRight(rotList, 2);
        assert toJavaList(rotated).equals(Arrays.asList(4, 5, 1, 2, 3));

        // 3. Reverse Alternate K Nodes
        ListNode altList = buildList(new int[]{1, 2, 3, 4, 5, 6, 7, 8});
        ListNode altRes = reverseAlternateKNodes(altList, 2);
        assert toJavaList(altRes).equals(Arrays.asList(2, 1, 3, 4, 6, 5, 7, 8));

        // 4. Add Two Numbers
        ListNode l1 = buildList(new int[]{2, 4, 3}); // 342
        ListNode l2 = buildList(new int[]{5, 6, 4}); // 465
        ListNode sumList = addTwoNumbers(l1, l2); // 807 -> [7, 0, 8]
        assert toJavaList(sumList).equals(Arrays.asList(7, 0, 8));

        // 5. Reverse Sub-list
        ListNode subList = buildList(new int[]{1, 2, 3, 4, 5});
        ListNode subRev = reverseBetween(subList, 2, 4);
        assert toJavaList(subRev).equals(Arrays.asList(1, 4, 3, 2, 5));

        // 6. Reverse K Group
        ListNode kList = buildList(new int[]{1, 2, 3, 4, 5});
        ListNode kRev = reverseKGroup(kList, 2);
        assert toJavaList(kRev).equals(Arrays.asList(2, 1, 4, 3, 5));

        // 7. Merge K Sorted Lists
        ListNode[] kArr = new ListNode[]{
                buildList(new int[]{2, 6, 8}),
                buildList(new int[]{3, 6, 7}),
                buildList(new int[]{1, 3, 4})
        };
        ListNode merged = mergeKLists(kArr);
        assert toJavaList(merged).equals(Arrays.asList(1, 2, 3, 3, 4, 6, 6, 7, 8));

        // 8. Kth Smallest in M Sorted Lists
        List<List<Integer>> mLists = Arrays.asList(
                Arrays.asList(2, 6, 8),
                Arrays.asList(3, 6, 7),
                Arrays.asList(1, 3, 4)
        );
        assert findKthSmallestInMSortedLists(mLists, 5) == 4;

        System.out.println(" ADVANCED LINKED LIST CHALLENGES ALL PASSED!");
        System.out.println("=================================================");
    }
}
