public class PrefixSum {
    public static int[] prefixSum(int[] arr) {
        int[] prefixSum = new int[arr.length];
        // Assuming previous element is 0, set the first element
        // which is current element as it is.
        prefixSum[0] = arr[0];
        for (int i = 1; i < arr.length; i++) {
            // Add previous element with current element
            prefixSum[i] = prefixSum[i-1] + arr[i];
        }
        return prefixSum;
    }

    // rangeSum
    public static int rangeSum(int[] arr, int fromIndex, int toIndex) {
        if (fromIndex == 0) {
            return arr[toIndex];
        }
        return arr[toIndex] - arr[fromIndex - 1];
    }
}
