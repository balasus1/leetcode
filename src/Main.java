import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.out.println("===1.Prefix Sum===");
        prefixSum();
    }

    public static void prefixSum() {
        int[] nums = {1, 2, 3, 4, 5, 6};
        int[] prefixSumArray = PrefixSum.prefixSum(nums);
        System.out.println("Prefix Array: " + Arrays.toString(prefixSumArray));
        int rangeSum = PrefixSum.rangeSum(prefixSumArray, 1, 3);
        System.out.println("Range Sum: " + rangeSum);
    }
}