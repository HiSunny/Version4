package utils;
//求加速度平均值 加窗
import java.util.LinkedList;
import java.util.Queue;

public class MovingAverage {
    private static float filterSum = 0;
    private static float filterResult = 0;
    private final static Queue<Float> maWindow = new LinkedList<>();
    //求加速度平均值 acc：加速度的值 length：窗口长度，求平均时/length
    public static float movingAverage(float acc, int length) {
        filterSum += acc;
        maWindow.add(acc);

        if(maWindow.size() > length) {
            float head = maWindow.remove();
            filterSum -= head;
        }
        //求length个加速度的平均值
        if(! maWindow.isEmpty()) {
            filterResult = filterSum / maWindow.size();
        }
        return filterResult;
    }
}
