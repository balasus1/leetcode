package interview_prep_mastery._09_hash_tables;

import java.util.*;

/**
 * ============================================================================
 * MODULE 09: HASH TABLES - CHALLENGES 1 TO 10
 * ============================================================================
 */
public class HashTableChallenges {

    // ------------------------------------------------------------------------
    // Challenge 1: Find whether an array is a subset of another array
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n + m)
     * - Space: O(n)
     */
    public static boolean isSubset(int[] arr1, int[] arr2) {
        if (arr2 == null || arr2.length == 0) return true;
        if (arr1 == null) return false;
        Set<Integer> set = new HashSet<>();
        for (int x : arr1) set.add(x);
        for (int x : arr2) {
            if (!set.contains(x)) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------------
    // Challenge 2: Check if the given arrays are disjoint
    // ------------------------------------------------------------------------
    /**
     * Complexity:
     * - Time: O(n + m)
     * - Space: O(n)
     */
    public static boolean isDisjoint(int[] arr1, int[] arr2) {
        if (arr1 == null || arr2 == null) return true;
        Set<Integer> set = new HashSet<>();
        for (int x : arr1) set.add(x);
        for (int x : arr2) {
            if (set.contains(x)) return false; // Common element found
        }
        return true;
    }

    // ------------------------------------------------------------------------
    // Challenge 3: Find Symmetric Pairs in an Array
    // ------------------------------------------------------------------------
    /**
     * Pair (a, b) and (b, a) are symmetric.
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(n)
     */
    public static List<int[]> findSymmetricPairs(int[][] pairs) {
        List<int[]> result = new ArrayList<>();
        if (pairs == null || pairs.length == 0) return result;
        Map<Integer, Integer> map = new HashMap<>();

        for (int[] pair : pairs) {
            int first = pair[0];
            int second = pair[1];
            if (map.containsKey(second) && map.get(second) == first) {
                result.add(new int[]{second, first});
            } else {
                map.put(first, second);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Challenge 4: Trace the Complete Path of a Journey
    // ------------------------------------------------------------------------
    /**
     * Tickets: Map of (Departure -> Destination).
     * Reconstruct path from starting point (key that never appears as destination).
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(n)
     */
    public static List<String> traceJourney(Map<String, String> tickets) {
        List<String> journey = new ArrayList<>();
        if (tickets == null || tickets.isEmpty()) return journey;

        // Find reverse mapping to identify the unique start city
        Map<String, String> reverseMap = new HashMap<>();
        for (Map.Entry<String, String> entry : tickets.entrySet()) {
            reverseMap.put(entry.getValue(), entry.getKey());
        }

        String start = null;
        for (String departure : tickets.keySet()) {
            if (!reverseMap.containsKey(departure)) {
                start = departure;
                break;
            }
        }

        if (start == null) return journey; // Cyclic or invalid

        String curr = start;
        while (curr != null) {
            journey.add(curr);
            curr = tickets.get(curr);
        }
        return journey;
    }

    // ------------------------------------------------------------------------
    // Challenge 5: Find two pairs in an Array such that a+b = c+d
    // ------------------------------------------------------------------------
    public static class Pair {
        int first, second;
        public Pair(int first, int second) {
            this.first = first;
            this.second = second;
        }
    }

    public static List<int[]> findEqualSumPairs(int[] arr) {
        List<int[]> result = new ArrayList<>();
        if (arr == null || arr.length < 4) return result;
        Map<Integer, Pair> sumMap = new HashMap<>();

        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                int sum = arr[i] + arr[j];
                if (sumMap.containsKey(sum)) {
                    Pair prev = sumMap.get(sum);
                    // Ensure distinct indices
                    if (prev.first != i && prev.first != j && prev.second != i && prev.second != j) {
                        result.add(new int[]{arr[prev.first], arr[prev.second]});
                        result.add(new int[]{arr[i], arr[j]});
                        return result;
                    }
                } else {
                    sumMap.put(sum, new Pair(i, j));
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Challenge 6: Find If a Subarray with a Sum Equal to 0 Exists
    // ------------------------------------------------------------------------
    /**
     * Invariant: If cumulative prefixSum is repeated or becomes 0, a zero-sum subarray exists!
     *
     * Complexity:
     * - Time: O(n)
     * - Space: O(n)
     */
    public static boolean findZeroSumSubarray(int[] arr) {
        if (arr == null || arr.length == 0) return false;
        Set<Integer> prefixSums = new HashSet<>();
        int currentSum = 0;

        for (int x : arr) {
            currentSum += x;
            if (x == 0 || currentSum == 0 || prefixSums.contains(currentSum)) {
                return true;
            }
            prefixSums.add(currentSum);
        }
        return false;
    }

    // ------------------------------------------------------------------------
    // Challenge 7: First Non-Repeating Integer in an Array (Hashing)
    // ------------------------------------------------------------------------
    public static int firstUnique(int[] arr) {
        if (arr == null || arr.length == 0) return -1;
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (int x : arr) {
            counts.put(x, counts.getOrDefault(x, 0) + 1);
        }
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            if (entry.getValue() == 1) return entry.getKey();
        }
        return -1;
    }

    // ------------------------------------------------------------------------
    // Challenge 8: Remove Duplicates from Linked List using Hashing
    // ------------------------------------------------------------------------
    public static class Node {
        int val;
        Node next;
        Node(int val) { this.val = val; }
    }

    public static Node removeDuplicates(Node head) {
        if (head == null) return null;
        Set<Integer> seen = new HashSet<>();
        Node curr = head;
        Node prev = null;

        while (curr != null) {
            if (seen.contains(curr.val)) {
                prev.next = curr.next;
            } else {
                seen.add(curr.val);
                prev = curr;
            }
            curr = curr.next;
        }
        return head;
    }

    // ------------------------------------------------------------------------
    // Challenge 9: Union & Intersection of Lists using Hashing
    // ------------------------------------------------------------------------
    public static List<Integer> unionLists(List<Integer> l1, List<Integer> l2) {
        Set<Integer> set = new LinkedHashSet<>(l1);
        set.addAll(l2);
        return new ArrayList<>(set);
    }

    public static List<Integer> intersectLists(List<Integer> l1, List<Integer> l2) {
        Set<Integer> set1 = new HashSet<>(l1);
        Set<Integer> result = new LinkedHashSet<>();
        for (int x : l2) {
            if (set1.contains(x)) result.add(x);
        }
        return new ArrayList<>(result);
    }

    // ------------------------------------------------------------------------
    // Challenge 10: Find Two Numbers that Add up to "n" (Hashing)
    // ------------------------------------------------------------------------
    public static int[] twoSumHashing(int[] arr, int target) {
        if (arr == null || arr.length < 2) return new int[0];
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < arr.length; i++) {
            int complement = target - arr[i];
            if (map.containsKey(complement)) {
                return new int[]{complement, arr[i]};
            }
            map.put(arr[i], i);
        }
        return new int[0];
    }

    // ------------------------------------------------------------------------
    // Test Suite for Hash Table Challenges
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING HASH TABLE CHALLENGES TEST SUITE ");
        System.out.println("=================================================");

        // 1. Subset
        assert isSubset(new int[]{9, 4, 7, 1, -2, 6, 5}, new int[]{7, 1, -2});
        assert !isSubset(new int[]{9, 4, 7, 1}, new int[]{7, 8});

        // 2. Disjoint
        assert isDisjoint(new int[]{1, 2, 3, 4}, new int[]{5, 6, 7});
        assert !isDisjoint(new int[]{1, 2, 3, 4}, new int[]{4, 5, 6});

        // 3. Symmetric Pairs
        int[][] pairs = {{1, 2}, {3, 4}, {5, 9}, {4, 3}, {9, 5}};
        List<int[]> sym = findSymmetricPairs(pairs);
        assert sym.size() == 2;

        // 4. Trace Journey
        Map<String, String> tickets = new HashMap<>();
        tickets.put("Boston", "Texas");
        tickets.put("NewYork", "Boston");
        tickets.put("Texas", "Missouri");
        assert traceJourney(tickets).equals(Arrays.asList("NewYork", "Boston", "Texas", "Missouri"));

        // 5. a+b = c+d
        List<int[]> eqPairs = findEqualSumPairs(new int[]{3, 4, 7, 1, 2, 9, 8});
        assert !eqPairs.isEmpty();
        assert (eqPairs.get(0)[0] + eqPairs.get(0)[1]) == (eqPairs.get(1)[0] + eqPairs.get(1)[1]);

        // 6. Zero Sum Subarray
        assert findZeroSumSubarray(new int[]{6, 4, -7, 3, 12, 9});
        assert !findZeroSumSubarray(new int[]{1, 2, 3, 4});

        // 7. First Unique
        assert firstUnique(new int[]{9, 2, 3, 2, 6, 6}) == 9;

        // 8. Remove Dups LL
        Node h = new Node(1); h.next = new Node(2); h.next.next = new Node(2); h.next.next.next = new Node(3);
        removeDuplicates(h);
        assert h.next.next.val == 3 && h.next.next.next == null;

        // 9. Union & Intersection
        assert unionLists(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5)).equals(Arrays.asList(1, 2, 3, 4, 5));
        assert intersectLists(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4)).equals(Arrays.asList(2, 3));

        // 10. Two sum
        int[] ts = twoSumHashing(new int[]{2, 7, 11, 15}, 9);
        assert ts[0] + ts[1] == 9;

        System.out.println(" HASH TABLE CHALLENGES 1-10 ALL PASSED!");
        System.out.println("=================================================");
    }
}
