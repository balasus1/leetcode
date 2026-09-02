import java.util.Arrays;

public class RotateArrayByK {

    public static void main(String[] args) {
        int[] nums = { 1, 2, 3, 4, 5, 6, 7 };
        int arraylen = nums.length;
        if (arraylen == 0) return;
        int k = 3;
        k = k % arraylen;
        System.out.println("k = " + k);
        rotate(nums, 0, arraylen - 1);
        rotate(nums, 0, k - 1);
        rotate(nums, k, arraylen - 1);
        System.out.println(Arrays.toString(nums));
    }

    public static void rotate(int[] nums, int left, int right) {
        while (left < right) {
            int temp = nums[left];
            nums[left] = nums[right];
            nums[right] = temp;
            left++;
            right--;
        }
    }
}
