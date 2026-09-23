package interview_prep_mastery._08_heaps;

import java.util.*;

/**
 * ============================================================================
 * MODULE 08: HEAPS - MAX/MIN HEAP & CHALLENGES
 * Topics:
 * - Max Heap and Min Heap Array Implementations
 * - Challenge 1: Convert Max-Heap to Min-Heap
 * - Challenge 2: Find K Smallest Elements in an Array
 * - Challenge 3: Find K Largest Elements in an Array
 * ============================================================================
 */
public class HeapChallenges {

    // ------------------------------------------------------------------------
    // 1. Min-Heap Implementation
    // ------------------------------------------------------------------------
    public static class MinHeap {
        private int[] heap;
        private int size;
        private int capacity;

        public MinHeap(int capacity) {
            this.capacity = capacity;
            this.heap = new int[capacity];
            this.size = 0;
        }

        public void insert(int val) {
            if (size == capacity) {
                // Resize
                capacity *= 2;
                heap = Arrays.copyOf(heap, capacity);
            }
            heap[size] = val;
            heapifyUp(size);
            size++;
        }

        public int poll() {
            if (size == 0) throw new NoSuchElementException("Heap is empty");
            int root = heap[0];
            heap[0] = heap[size - 1];
            size--;
            heapifyDown(0);
            return root;
        }

        public int peek() {
            if (size == 0) throw new NoSuchElementException("Heap is empty");
            return heap[0];
        }

        public int size() {
            return size;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        private void heapifyUp(int i) {
            while (i > 0) {
                int parent = (i - 1) / 2;
                if (heap[i] < heap[parent]) {
                    swap(heap, i, parent);
                    i = parent;
                } else {
                    break;
                }
            }
        }

        private void heapifyDown(int i) {
            while (2 * i + 1 < size) {
                int left = 2 * i + 1;
                int right = 2 * i + 2;
                int smallest = left;
                if (right < size && heap[right] < heap[left]) {
                    smallest = right;
                }
                if (heap[i] > heap[smallest]) {
                    swap(heap, i, smallest);
                    i = smallest;
                } else {
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // 2. Max-Heap Implementation
    // ------------------------------------------------------------------------
    public static class MaxHeap {
        private int[] heap;
        private int size;
        private int capacity;

        public MaxHeap(int capacity) {
            this.capacity = capacity;
            this.heap = new int[capacity];
            this.size = 0;
        }

        public void insert(int val) {
            if (size == capacity) {
                capacity *= 2;
                heap = Arrays.copyOf(heap, capacity);
            }
            heap[size] = val;
            heapifyUp(size);
            size++;
        }

        public int poll() {
            if (size == 0) throw new NoSuchElementException("Heap is empty");
            int root = heap[0];
            heap[0] = heap[size - 1];
            size--;
            heapifyDown(0);
            return root;
        }

        public int peek() {
            if (size == 0) throw new NoSuchElementException("Heap is empty");
            return heap[0];
        }

        public int size() {
            return size;
        }

        private void heapifyUp(int i) {
            while (i > 0) {
                int parent = (i - 1) / 2;
                if (heap[i] > heap[parent]) {
                    swap(heap, i, parent);
                    i = parent;
                } else {
                    break;
                }
            }
        }

        private void heapifyDown(int i) {
            while (2 * i + 1 < size) {
                int left = 2 * i + 1;
                int right = 2 * i + 2;
                int largest = left;
                if (right < size && heap[right] > heap[left]) {
                    largest = right;
                }
                if (heap[i] < heap[largest]) {
                    swap(heap, i, largest);
                    i = largest;
                } else {
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 1: Convert a Max-Heap to a Min-Heap
    // ------------------------------------------------------------------------
    /**
     * Bottom-up min-heapify starting from the last non-leaf node (n/2 - 1) down to 0.
     *
     * Complexity:
     * - Time: O(n) in-place
     * - Space: O(1)
     */
    public static void convertMaxToMinHeap(int[] maxHeap) {
        if (maxHeap == null || maxHeap.length <= 1) return;
        int n = maxHeap.length;
        for (int i = (n / 2) - 1; i >= 0; i--) {
            minHeapifyDown(maxHeap, i, n);
        }
    }

    private static void minHeapifyDown(int[] arr, int i, int n) {
        int smallest = i;
        int left = 2 * i + 1;
        int right = 2 * i + 2;

        if (left < n && arr[left] < arr[smallest]) {
            smallest = left;
        }
        if (right < n && arr[right] < arr[smallest]) {
            smallest = right;
        }
        if (smallest != i) {
            swap(arr, i, smallest);
            minHeapifyDown(arr, smallest, n);
        }
    }

    // ------------------------------------------------------------------------
    // Challenge 2: Find the K Smallest Elements in an Array
    // ------------------------------------------------------------------------
    /**
     * Using a Max-Heap of size K:
     * Keep the smallest K elements seen so far.
     *
     * Complexity:
     * - Time: O(n log k)
     * - Space: O(k)
     */
    public static int[] findKSmallest(int[] arr, int k) {
        if (arr == null || k <= 0) return new int[0];
        if (k > arr.length) k = arr.length;
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());

        for (int x : arr) {
            maxHeap.offer(x);
            if (maxHeap.size() > k) {
                maxHeap.poll();
            }
        }

        int[] result = new int[k];
        for (int i = k - 1; i >= 0; i--) {
            result[i] = maxHeap.poll();
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Challenge 3: Find the K Largest Elements in an Array
    // ------------------------------------------------------------------------
    /**
     * Using a Min-Heap of size K:
     * Keep the largest K elements seen so far.
     *
     * Complexity:
     * - Time: O(n log k)
     * - Space: O(k)
     */
    public static int[] findKLargest(int[] arr, int k) {
        if (arr == null || k <= 0) return new int[0];
        if (k > arr.length) k = arr.length;
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        for (int x : arr) {
            minHeap.offer(x);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = minHeap.poll();
        }
        return result;
    }

    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // ------------------------------------------------------------------------
    // Test Suite for Heap Challenges
    // ------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println(" RUNNING HEAP CHALLENGES TEST SUITE ");
        System.out.println("=================================================");

        // 1. MinHeap
        MinHeap minH = new MinHeap(5);
        minH.insert(10); minH.insert(4); minH.insert(15); minH.insert(1);
        assert minH.poll() == 1;
        assert minH.poll() == 4;

        // 2. MaxHeap
        MaxHeap maxH = new MaxHeap(5);
        maxH.insert(10); maxH.insert(4); maxH.insert(15); maxH.insert(1);
        assert maxH.poll() == 15;
        assert maxH.poll() == 10;

        // 3. Convert Max to Min Heap
        int[] maxHeapArr = {9, 4, 7, 1, -2, 6, 5};
        convertMaxToMinHeap(maxHeapArr);
        // Verify min-heap property for all nodes
        for (int i = 0; i <= (maxHeapArr.length / 2) - 1; i++) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            if (left < maxHeapArr.length) assert maxHeapArr[i] <= maxHeapArr[left];
            if (right < maxHeapArr.length) assert maxHeapArr[i] <= maxHeapArr[right];
        }

        // 4. K Smallest
        int[] arr = {9, 4, 7, 1, -2, 6, 5};
        int[] kSmall = findKSmallest(arr, 3);
        Arrays.sort(kSmall);
        assert Arrays.equals(kSmall, new int[]{-2, 1, 4});

        // 5. K Largest
        int[] kLarge = findKLargest(arr, 3);
        Arrays.sort(kLarge);
        assert Arrays.equals(kLarge, new int[]{6, 7, 9});

        System.out.println(" HEAP CHALLENGES ALL PASSED!");
        System.out.println("=================================================");
    }
}
