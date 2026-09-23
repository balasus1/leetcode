package dsa;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MostInteger {

    /**
     * Approach 1 (Recommended for Interviews): Simple Map using getOrDefault
     * 
     * - Easy to write, read, and explain.
     * - Tracks maximum frequency on the fly in O(n) time.
     * 
     * Time Complexity: O(n)
     * Space Complexity: O(u) where u is number of unique elements
     */
    public static int mostFrequentSimpleMap(int[] nums) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }

        Map<Integer, Integer> map = new HashMap<>();
        int maxCount = 0;
        int mostFrequent = nums[0];

        for (int num : nums) {
            int count = map.getOrDefault(num, 0) + 1;
            map.put(num, count);

            if (count > maxCount) {
                maxCount = count;
                mostFrequent = num;
            }
        }

        return mostFrequent;
    }

    /**
     * Approach 2: Map.merge() (Cleaner Single-Pass HashMap)
     * 
     * - Uses Map.merge() to update counts in 1 call instead of getOrDefault + put.
     * 
     * Time Complexity: O(n)
     * Space Complexity: O(u)
     */
    public static int mostFrequentHashMap(int[] nums) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }

        Map<Integer, Integer> counts = new HashMap<>();
        int maxCount = 0;
        int mostFrequent = nums[0];

        for (int num : nums) {
            int count = counts.merge(num, 1, Integer::sum);
            if (count > maxCount) {
                maxCount = count;
                mostFrequent = num;
            }
        }
        return mostFrequent;
    }

    /**
     * Approach 3: Direct Counting Array (Fastest overall when range is known & bounded)
     * 
     * - Zero object allocations, cache-friendly primitive array.
     * 
     * Time Complexity: O(n)
     * Space Complexity: O(range)
     */
    public static int mostFrequentBoundedRange(int[] nums, int min, int max) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }

        int[] freq = new int[max - min + 1];
        int maxCount = 0;
        int mostFrequent = nums[0];

        for (int num : nums) {
            int count = ++freq[num - min];
            if (count > maxCount) {
                maxCount = count;
                mostFrequent = num;
            }
        }
        return mostFrequent;
    }

    /**
     * Approach 4: Sort in-place (Most Space-Efficient: O(1) extra space)
     * 
     * - Sorts array and counts consecutive identical elements.
     * 
     * Time Complexity: O(n log n)
     * Space Complexity: O(1)
     */
    public static int mostFrequentSorted(int[] nums) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }

        Arrays.sort(nums);

        int mostFrequent = nums[0];
        int maxCount = 1;

        int currentElement = nums[0];
        int currentCount = 1;

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] == currentElement) {
                currentCount++;
            } else {
                currentElement = nums[i];
                currentCount = 1;
            }

            if (currentCount > maxCount) {
                maxCount = currentCount;
                mostFrequent = currentElement;
            }
        }
        return mostFrequent;
    }

    /**
     * Approach 5: Modern Java Stream (Functional & Concise)
     * 
     * Time Complexity: O(n)
     * Space Complexity: O(u)
     */
    public static int mostFrequentStream(int[] nums) {
        if (nums == null || nums.length == 0) {
            throw new IllegalArgumentException("Array is empty");
        }

        return Arrays.stream(nums)
                .boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(NoSuchElementException::new);
    }

    public static void main(String[] args) {
        int[] numArray = { 1, 1, 2, 3, 3, 2, 4, 4 };

        System.out.println("Input Array: " + Arrays.toString(numArray));
        System.out.println("Frequencies: 1 -> 2 times, 2 -> 2 times, 3 -> 2 times, 4 -> 2 times (Tie!)");
        System.out.println("--------------------------------------------------");
        System.out.println("1. Simple Map (getOrDefault) : " + mostFrequentSimpleMap(numArray));
        System.out.println("2. Single-Pass Map.merge()   : " + mostFrequentHashMap(numArray));
        System.out.println("3. Direct Range Array        : " + mostFrequentBoundedRange(numArray, 1, 4));
        System.out.println("4. Modern Java Stream        : " + mostFrequentStream(numArray));
        System.out.println("5. In-place Sorted           : " + mostFrequentSorted(numArray.clone()));
    }
}
