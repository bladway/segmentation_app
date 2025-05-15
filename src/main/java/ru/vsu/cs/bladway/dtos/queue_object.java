package ru.vsu.cs.bladway.dtos;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import ru.vsu.cs.bladway.utils.math_util;

@AllArgsConstructor
@NoArgsConstructor
public class queue_object implements Comparable<queue_object> {
    public pixel pixel;
    public pixel center_pixel;
    public Integer center_index;

    @Override
    public int compareTo(queue_object o) {
        return math_util.find_color_distance(this.pixel, this.center_pixel)
                .compareTo(math_util.find_color_distance(o.pixel, o.center_pixel));
    }
    // храним ссылку на первый идеальный пиксель и его цвет, у которого приоритет равен нулю и просто на каждом шаге
    // приоритет вычислять по этой взвешенной разнице. Когда нужно присваивать пиксель вершине, то этой медоиде и будем
    // присваивать
}
