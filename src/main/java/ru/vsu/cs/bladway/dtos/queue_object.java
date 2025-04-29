package ru.vsu.cs.bladway.dtos;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import ru.vsu.cs.bladway.utils.math_util;

@AllArgsConstructor
@NoArgsConstructor
public class queue_object implements Comparable<queue_object> {
    public ru.vsu.cs.bladway.dtos.pixel pixel;
    public ru.vsu.cs.bladway.dtos.pixel center_pixel;
    public Integer center_index;

    @Override
    public int compareTo(queue_object o) {
        return math_util.find_color_distance(this.pixel, this.center_pixel)
                .compareTo(math_util.find_color_distance(o.pixel, o.center_pixel));
    }
    //private Point3 pixel_color_offset; // обновляется по разнице цветов смежных пикселей - сумма этой разницы и предыдущего значения
    // сам приоритет в очереди уже опеределяется по взвеси цветов
    // либо хранить ссылку на первый идеальный пиксель и его цвет, у которого приоритет равен нулю и просто на каждом шаге
    // приоритет вычислять по этой взвешенной разнице. а когда надо присваивать пиксель вершине, то этой медоиде и будет присваивать
}
