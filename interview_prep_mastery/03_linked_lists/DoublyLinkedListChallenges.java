package interview_prep_mastery._03_linked_lists;

import java.util.*;

/**
 * ============================================================================
 * MODULE 03: LINKED LISTS - DOUBLY LINKED LIST & PALINDROME (CHALLENGE 11)
 * ============================================================================
 */
public class DoublyLinkedListChallenges {

    // Doubly Linked List Node Definition
    public static class DoublyNode {
        public int data;
        public DoublyNode prev;
        public DoublyNode next;

        public DoublyNode(int data) {
            this.data = data;
            this.prev = null;
            this.next = null;
        }
    }

    public static class DoublyLinkedList {
        public DoublyNode head;
        public DoublyNode tail;
        public int size;

        public DoublyLinkedList() {
            this.head = null;
            this.tail = null;
            this.size = 0;
        }

        public void insertAtHead(int data) {
            DoublyNode newNode = new DoublyNode(data);
            if (head == null) {
                head = tail = newNode;
            } else {
                newNode.next = head;
                head.prev = newNode;
                head = newNode;
            }
            size++;
        }

        public void insertAtTail(int data) {
            DoublyNode newNode = new DoublyNode(data);
            if (tail == null) {
                head = tail = newNode;
            } else {
                tail.next = newNode;
                newNode.prev = tail;
                tail = newNode;
            }
            size++;
        }

        public boolean deleteByValue(int data) {
            if (head == null) return false;
            DoublyNode curr = head;
            while (curr != null && curr.data != data) {
                curr = curr.next;
            }
            if (curr == null) return false; // Not found

            if (curr == head) {
                head = head.next;
                if (head != null) head.prev = null;
                else tail = null;
            } else if (curr == tail) {
                tail = tail.prev;
                if (tail != null) tail.next = null;
                else head = null;
            } else {
                curr.prev.next = curr.next;
                curr.next.prev = curr.prev;
            }
            size--;
            return true;
        }

        // Challenge 11: Find if Doubly Linked-List is a Palindrome
        /**
         * Probing Questions / Assumptions:
         * - Can we leverage the tail pointer to avoid O(n) extra space?
         * - Yes! Two pointers moving towards the center (head moves forward, tail moves backward).
         *
         * Complexity:
         * - Time: O(n)
         * - Space: O(1) in-place auxiliary
         */
        public boolean isPalindrome() {
            if (head == null || head == tail) return true;
            DoublyNode left = head;
            DoublyNode right = tail;
            while (left != null && right != null && left != right && left.prev != right) {
                if (left.data != right.data) {
                    return false;
                }
                left = left.next;
                right = right.prev;
            }
            return true;
        }

        public List<Integer> toForwardList() {
            List<Integer> list = new ArrayList<>();
            DoublyNode curr = head;
            while (curr != null) {
                list.add(curr.data);
                curr = curr.next;
            }
            return list;
        }

        public List<Integer> toBackwardList() {
            List<Integer> list = new ArrayList<>();
            DoublyNode curr = tail;
            while (curr != null) {
                list.add(curr.data);
                curr = curr.prev;
            }
            return list;
        }
    }

    // ------------------------------------------------------------------------
    // Test Suite for Doubly Linked List Challenges
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING DOUBLY LINKED LIST TEST SUITE ");
        System.out.println("=================================================");

        DoublyLinkedList dll = new DoublyLinkedList();
        dll.insertAtTail(1);
        dll.insertAtTail(2);
        dll.insertAtTail(3);
        dll.insertAtTail(2);
        dll.insertAtTail(1);

        assert dll.toForwardList().equals(Arrays.asList(1, 2, 3, 2, 1));
        assert dll.toBackwardList().equals(Arrays.asList(1, 2, 3, 2, 1));

        // Challenge 11: Palindrome Check
        assert dll.isPalindrome() : "Expected odd-length palindrome to be true";

        dll.insertAtTail(9);
        assert !dll.isPalindrome() : "Expected non-palindrome to be false";

        dll.deleteByValue(9);
        assert dll.isPalindrome();

        // Even length palindrome test
        DoublyLinkedList evenDll = new DoublyLinkedList();
        evenDll.insertAtTail(10);
        evenDll.insertAtTail(20);
        evenDll.insertAtTail(20);
        evenDll.insertAtTail(10);
        assert evenDll.isPalindrome() : "Expected even-length palindrome to be true";

        System.out.println(" DOUBLY LINKED LIST & PALINDROME CHALLENGE 11 PASSED!");
        System.out.println("=================================================");
    }
}
