import java.util.HashMap;
import java.util.Map;

public class MajorityElement {
    /**
     * Approach-1: Boyer-Moore Majority Element
     * Returns the majority element of the given array. The majority element is
     * the element that appears more than n/2 times where n is the length of the
     * array.
     * 
     * @param nums the array to find the majority element in
     * @return the majority element
     */
    public int majorityElement(int[] nums) {
        int count = 0;
        int candidate = 0;
        for (int num : nums) {
            if (count == 0) {
                candidate = num;
            }
            count += num == candidate ? 1 : -1;
        }
        return candidate;
    }
    // Approach-2: HashMap
    public int majorityElement2(int[] nums) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int num : nums) {
            map.put(num, map.getOrDefault(num, 0) + 1);
        }
        int count = 0;
        int candidate = 0;
        for (int num : map.keySet()) {
            if (map.get(num) > count) {
                count = map.get(num);
                candidate = num;
            }
        }
        return candidate;
    }
}
