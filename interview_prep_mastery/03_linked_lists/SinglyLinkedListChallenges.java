package interview_prep_mastery._03_linked_lists;

import java.util.*;

/**
 * ============================================================================
 * MODULE 03: LINKED LISTS - SINGLY LINKED LIST CHALLENGES (1 to 10)
 * ============================================================================
 */
public class SinglyLinkedListChallenges {

    // Node Definition
    public static class Node {
        public int data;
        public Node next;

        public Node(int data) {
            this.data = data;
            this.next = null;
        }
    }

    public static class SinglyLinkedList {
        public Node head;

        public SinglyLinkedList() {
            this.head = null;
        }

        // Challenge 1: Insertion at End
        /**
         * Probing Questions / Assumptions:
         * - What if list is empty? (New node becomes head)
         * - Time: O(n) without tail pointer, O(1) with tail pointer
         * - Space: O(1)
         */
        public void insertAtEnd(int value) {
            Node newNode = new Node(value);
            if (head == null) {
                head = newNode;
                return;
            }
            Node curr = head;
            while (curr.next != null) {
                curr = curr.next;
            }
            curr.next = newNode;
        }

        public void insertAtHead(int value) {
            Node newNode = new Node(value);
            newNode.next = head;
            head = newNode;
        }

        // Challenge 2: Search in Singly Linked List
        /**
         * Time: O(n), Space: O(1)
         */
        public boolean searchNode(int value) {
            Node curr = head;
            while (curr != null) {
                if (curr.data == value) return true;
                curr = curr.next;
            }
            return false;
        }

        // Challenge 3: Deletion by Value
        /**
         * Probing Questions / Assumptions:
         * - What if head contains the target value? (Update head to head.next)
         * - What if value does not exist? (No-op, return false)
         *
         * Time: O(n), Space: O(1)
         */
        public boolean deleteByValue(int value) {
            if (head == null) return false;
            if (head.data == value) {
                head = head.next;
                return true;
            }
            Node prev = head;
            Node curr = head.next;
            while (curr != null) {
                if (curr.data == value) {
                    prev.next = curr.next;
                    return true;
                }
                prev = curr;
                curr = curr.next;
            }
            return false;
        }

        // Challenge 4: Find Length of Linked List
        /**
         * Time: O(n), Space: O(1) iterative
         */
        public int length() {
            int count = 0;
            Node curr = head;
            while (curr != null) {
                count++;
                curr = curr.next;
            }
            return count;
        }

        // Challenge 5: Reverse a Linked List (In-Place)
        /**
         * 3-pointer iterative reversal: prev, curr, next.
         * Time: O(n), Space: O(1)
         */
        public void reverse() {
            Node prev = null;
            Node curr = head;
            while (curr != null) {
                Node nextNode = curr.next;
                curr.next = prev;
                prev = curr;
                curr = nextNode;
            }
            head = prev;
        }

        // Challenge 6: Detect Loop in a Linked List (Floyd's Cycle Algorithm)
        /**
         * Fast & Slow Pointers:
         * Time: O(n), Space: O(1)
         */
        public boolean detectLoop() {
            Node slow = head;
            Node fast = head;
            while (fast != null && fast.next != null) {
                slow = slow.next;
                fast = fast.next.next;
                if (slow == fast) return true;
            }
            return false;
        }

        // Challenge 7: Find Middle Node of a Linked List
        /**
         * Time: O(n), Space: O(1)
         */
        public Node findMiddle() {
            if (head == null) return null;
            Node slow = head;
            Node fast = head;
            while (fast != null && fast.next != null) {
                slow = slow.next;
                fast = fast.next.next;
            }
            return slow;
        }

        // Challenge 8: Remove Duplicates from a Linked List
        /**
         * Using HashSet for unsorted list:
         * Time: O(n), Space: O(n)
         */
        public void removeDuplicates() {
            if (head == null) return;
            Set<Integer> seen = new HashSet<>();
            Node curr = head;
            Node prev = null;
            while (curr != null) {
                if (seen.contains(curr.data)) {
                    prev.next = curr.next;
                } else {
                    seen.add(curr.data);
                    prev = curr;
                }
                curr = curr.next;
            }
        }

        // Challenge 10: Return the Nth Node from End
        /**
         * Two Pointers with N distance gap:
         * Time: O(n), Space: O(1)
         */
        public Node getNthFromEnd(int n) {
            if (head == null || n <= 0) return null;
            Node fast = head;
            for (int i = 0; i < n; i++) {
                if (fast == null) return null; // n is larger than list length
                fast = fast.next;
            }
            Node slow = head;
            while (fast != null) {
                slow = slow.next;
                fast = fast.next;
            }
            return slow;
        }

        // Helper: Convert to List for easy assertion
        public List<Integer> toList() {
            List<Integer> list = new ArrayList<>();
            Node curr = head;
            while (curr != null) {
                list.add(curr.data);
                curr = curr.next;
            }
            return list;
        }
    }

    // Challenge 9: Union and Intersection of Two Linked Lists
    /**
     * Union and Intersection using Set:
     * Time: O(n + m), Space: O(n + m)
     */
    public static SinglyLinkedList union(SinglyLinkedList list1, SinglyLinkedList list2) {
        SinglyLinkedList result = new SinglyLinkedList();
        Set<Integer> set = new LinkedHashSet<>();
        Node curr = (list1 != null) ? list1.head : null;
        while (curr != null) {
            set.add(curr.data);
            curr = curr.next;
        }
        curr = (list2 != null) ? list2.head : null;
        while (curr != null) {
            set.add(curr.data);
            curr = curr.next;
        }
        for (int val : set) {
            result.insertAtEnd(val);
        }
        return result;
    }

    public static SinglyLinkedList intersection(SinglyLinkedList list1, SinglyLinkedList list2) {
        SinglyLinkedList result = new SinglyLinkedList();
        if (list1 == null || list2 == null) return result;
        Set<Integer> set1 = new HashSet<>();
        Node curr = list1.head;
        while (curr != null) {
            set1.add(curr.data);
            curr = curr.next;
        }
        Set<Integer> intersectSet = new LinkedHashSet<>();
        curr = list2.head;
        while (curr != null) {
            if (set1.contains(curr.data)) {
                intersectSet.add(curr.data);
            }
            curr = curr.next;
        }
        for (int val : intersectSet) {
            result.insertAtEnd(val);
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Test Suite for Singly Linked List Challenges
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING SINGLY LINKED LIST TEST SUITE ");
        System.out.println("=================================================");

        SinglyLinkedList list = new SinglyLinkedList();
        list.insertAtEnd(10);
        list.insertAtEnd(20);
        list.insertAtEnd(30);
        list.insertAtHead(5);

        // Verify structure: 5 -> 10 -> 20 -> 30
        assert list.toList().equals(Arrays.asList(5, 10, 20, 30));
        assert list.length() == 4;
        assert list.searchNode(20);
        assert !list.searchNode(99);

        // Delete node
        list.deleteByValue(10);
        assert list.toList().equals(Arrays.asList(5, 20, 30));

        // Middle node
        assert list.findMiddle().data == 20;

        // Reverse
        list.reverse();
        assert list.toList().equals(Arrays.asList(30, 20, 5));

        // Nth from end
        assert list.getNthFromEnd(2).data == 20;

        // Duplicates
        list.insertAtEnd(20);
        list.insertAtEnd(5);
        list.removeDuplicates();
        assert list.toList().equals(Arrays.asList(30, 20, 5));

        // Union & Intersection
        SinglyLinkedList l1 = new SinglyLinkedList();
        l1.insertAtEnd(15); l1.insertAtEnd(22); l1.insertAtEnd(8);
        SinglyLinkedList l2 = new SinglyLinkedList();
        l2.insertAtEnd(7); l2.insertAtEnd(14); l2.insertAtEnd(22);

        SinglyLinkedList unionList = union(l1, l2);
        assert unionList.toList().equals(Arrays.asList(15, 22, 8, 7, 14));

        SinglyLinkedList interList = intersection(l1, l2);
        assert interList.toList().equals(Collections.singletonList(22));

        // Loop detection
        assert !list.detectLoop();
        // Create intentional cycle
        list.head.next.next.next = list.head; // 30 -> 20 -> 5 -> 30
        assert list.detectLoop();

        System.out.println(" SINGLY LINKED LIST CHALLENGES 1-10 ALL PASSED!");
        System.out.println("=================================================");
    }
}
