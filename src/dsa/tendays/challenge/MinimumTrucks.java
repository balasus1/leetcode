public class MinimumTrucks {

    public static void main(String[] args) {
        List<Integer> deliveryTimes = Arrays.asList(
            12,
            80,
            120,
            140,
            201,
            263,
            330
        );
        minimumTrucks(deliveryTimes);
    }

    public static int minimumTrucks(List<Integer> deliveryTimes) {
        if (deliveryTimes == null || deliveryTimes.isEmpty()) {
            return 0;
        }
    }
}
