package ru.vsu.cs.bladway.utils;

import lombok.RequiredArgsConstructor;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import ru.vsu.cs.bladway.dtos.pixel;
import ru.vsu.cs.bladway.dtos.queue_object;
import ru.vsu.cs.bladway.dtos.segmentation_result;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.center_update_method;

import java.util.*;

import static ru.vsu.cs.bladway.segmentation_app.*;

@RequiredArgsConstructor
public class math_util {

    public static Double find_color_distance(Point3 first_point, Point3 second_point) {
        return Math.sqrt(
                (second_point.x - first_point.x) * (second_point.x - first_point.x) +
                        (second_point.y - first_point.y) * (second_point.y - first_point.y) +
                        (second_point.z - first_point.z) * (second_point.z - first_point.z)
        );
    }

    public static Double find_color_distance(pixel first_pixel, pixel second_pixel) {
        return Math.sqrt(
                (second_pixel.b - first_pixel.b) * (second_pixel.b - first_pixel.b) +
                        (second_pixel.g - first_pixel.g) * (second_pixel.g - first_pixel.g) +
                        (second_pixel.r - first_pixel.r) * (second_pixel.r - first_pixel.r)
        );
    }

    public static Double find_color_distance(pixel first_pixel, Point3 second_point) {
        return Math.sqrt(
                (second_point.x - first_pixel.b) * (second_point.x - first_pixel.b) +
                        (second_point.y - first_pixel.g) * (second_point.y - first_pixel.g) +
                        (second_point.z - first_pixel.r) * (second_point.z - first_pixel.r)
        );
    }

    public static Double find_color_distance(Point3 first_point, pixel second_pixel) {
        return Math.sqrt(
                (second_pixel.b - first_point.x) * (second_pixel.b - first_point.x) +
                        (second_pixel.g - first_point.y) * (second_pixel.g - first_point.y) +
                        (second_pixel.r - first_point.z) * (second_pixel.r - first_point.z)
        );
    }

    private static void replace_values(Mat mat, int what, int on_what) {
        // Маска заменяемого элемента
        Mat binary_mask = new Mat();
        Core.compare(mat, new Scalar(what), binary_mask, Core.CMP_EQ);
        mat.setTo(new Scalar(on_what), binary_mask);
    }

    private static void swap_values(Mat mat, int y, int x) {
        // Маска первого элемента
        Mat y_binary_mask = new Mat();
        Core.compare(mat, new Scalar(y), y_binary_mask, Core.CMP_EQ);
        // Маска второго элемента;
        Mat x_binary_mask = new Mat();
        Core.compare(mat, new Scalar(x), x_binary_mask, Core.CMP_EQ);
        // Копируем второй элемент на позиции первого
        mat.setTo(new Scalar(x), y_binary_mask);
        // Копируем первый элемент на позиции второго
        mat.setTo(new Scalar(y), x_binary_mask);
    }

    private static pixel find_new_center(Mat image, Mat binary_center_mask, center_update_method method) {
        Point3 sums_bgr = new Point3(0, 0, 0);
        long pixels_in_cluster = 0L;   // Количество пикселей в данном кластере
        // Проходим по каждому пикселю изображения и собираем информацию о кластере
        for (int y = 0; y < image.rows(); ++y) {
            for (int x = 0; x < image.cols(); ++x) {
                // Если пиксель принадлежит данному центру
                if ((int) binary_center_mask.get(y, x)[0] != 0) {
                    Point3 pixel_bgr = new Point3(image.get(y, x));
                    // Складываем данные по компонентам RGB
                    sums_bgr.x += pixel_bgr.x;
                    sums_bgr.y += pixel_bgr.y;
                    sums_bgr.z += pixel_bgr.z;
                    pixels_in_cluster++;
                }
            }
        }
        // Среднее значение цветов кластера
        Point3 mean = new Point3(
                sums_bgr.x / pixels_in_cluster,
                sums_bgr.y / pixels_in_cluster,
                sums_bgr.z / pixels_in_cluster
        );
        if (method == center_update_method.MEAN) {
            return new pixel(-1, -1, mean);
        }
        // Теперь найдем пиксель, наиболее близкий к этому среднему значению
        pixel medoid = null;
        double min_distance = Double.MAX_VALUE;
        // Находим пиксель с минимальным расстоянием
        for (int y = 0; y < image.rows(); ++y) {
            for (int x = 0; x < image.cols(); ++x) {
                // Если пиксель принадлежит данному кластеру
                if ((int) binary_center_mask.get(y, x)[0] != 0) {
                    Point3 pixel_bgr = new Point3(image.get(y, x));
                    double distance = find_color_distance(mean, pixel_bgr);
                    if (distance < min_distance) {
                        min_distance = distance;
                        medoid = new pixel(y, x, pixel_bgr);
                    }
                }
            }
        }
        return medoid;
    }

    public static pixel pick_window_center(Mat image) {
        // Первым центром берем пиксель из центральной части изображения, наиболее похожий на существующие
        int window_start_row = (int) (image.rows() * (0.5 - center_window_size / 2));
        int window_end_row = (int) (image.rows() * (0.5 + center_window_size / 2));
        int window_start_col = (int) (image.cols() * (0.5 - center_window_size / 2));
        int window_end_col = (int) (image.cols() * (0.5 + center_window_size / 2));
        Mat window_input_image = image.submat(window_start_row, window_end_row, window_start_col, window_end_col);
        Mat window_binary_center_mask = new Mat(
                window_input_image.rows(),
                window_input_image.cols(),
                CvType.CV_8UC1
        ).setTo(new Scalar(255));
        pixel center = find_new_center(window_input_image, window_binary_center_mask, center_update_method.MEDOID);
        // Сдвиг пикселей, чтобы значения соответствовали входному изображению
        center.y += window_start_row;
        center.x += window_start_col;
        return center;
    }

    private static List<pixel> pick_random_centers(Mat image, int K) {
        // Случайная инициализация центров
        List<pixel> centers = new ArrayList<>();
        for (int i = 0; i < K; i++) {
            // По умолчанию считаем, что новый добавляемый пиксель может совпадать с уже присутствующими
            int current_random_row = random.nextInt(image.rows());
            int current_random_col = random.nextInt(image.cols());
            // Проверяем, нет ли совпадения с уже существующими центрами по позиции
            for (pixel pixel : centers) {
                if (current_random_row == pixel.y && current_random_col == pixel.x) {
                    i--;
                    break;
                }
            }
            // Выполняем поиск i-го центра заново
            if (i == centers.size() - 1) continue;
            // Добавляем новый центр
            pixel center = new pixel(
                    current_random_row,
                    current_random_col,
                    image.get(current_random_row, current_random_col)
            );
            centers.add(center);
        }
        return centers;
    }

    private static List<pixel> pick_plus_plus_centers(Mat image, int K) {
        // Инициализация центров KMeans++ как самых удаленных друг от друга
        // со случайной первой
        List<pixel> centers = new ArrayList<>();
        boolean is_first_center = true;
        for (int k = 0; k < K; k++) {
            // Значение добавляемого центра
            pixel center;
            if (is_first_center) {
                // Первый центр в алгоритме выбирается случайным
                int y = random.nextInt(image.rows());
                int x = random.nextInt(image.cols());
                center = new pixel(y, x, image.get(y, x));
                is_first_center = false;
            } else {
                /* Нахождение пикселя, который находится на самом большом
                расстоянии от ближайшего центра из имеющихся */
                double max_global_distance_to_any_defined_center = -1;
                Point center_candidate = new Point();
                for (int y = 0; y < image.rows(); y++) {
                    for (int x = 0; x < image.cols(); x++) {
                        double min_distance_to_any_defined_center = Double.MAX_VALUE;
                        for (int l = 0; l < k; l++) {
                            min_distance_to_any_defined_center = Math.min(find_color_distance(
                                    centers.get(l),
                                    new Point3(image.get(y, x))
                            ), min_distance_to_any_defined_center);
                        }
                        if (min_distance_to_any_defined_center > max_global_distance_to_any_defined_center) {
                            max_global_distance_to_any_defined_center = min_distance_to_any_defined_center;
                            center_candidate = new Point(x, y);
                        }
                    }
                }
                center = new pixel(
                        (int) center_candidate.y,
                        (int) center_candidate.x,
                        image.get((int) center_candidate.y, (int) center_candidate.x)
                );
            }
            // Добавляем новый центр
            centers.add(center);
        }
        return centers;
    }

    private static List<pixel> pick_paper_centers(Mat image, int K) {
        // Инициализация центров с помощью окна и угловых пикселей
        List<pixel> centers = new ArrayList<>();
        // Первый пиксель берется из окна
        centers.add(pick_window_center(image));
        // Все остальные сегменты берем начиная с угловых частей изображения
        int div = (K - 1) / 4;
        int mod = (K - 1) % 4;
        if (div > 0) {
            centers.add(new pixel(0, 0, image));
            centers.add(new pixel(0, image.cols() - 1, image));
            centers.add(new pixel(image.rows() - 1, image.cols() - 1, image));
            centers.add(new pixel(image.rows() - 1, 0, image));
            // Сегменты, лежащие на гранях
            div--;
            int edge_segments;
            edge_segments = mod > 0 ? div + 1 : div;
            for (int i = 0; i < edge_segments; i++) {
                centers.add(new pixel(
                        0,
                        (image.cols() - 1) / (edge_segments + 1) * (i + 1),
                        image));
            }
            edge_segments = mod > 1 ? div + 1 : div;
            for (int i = 0; i < edge_segments; i++) {
                centers.add(new pixel(
                        (image.rows() - 1) / (edge_segments + 1) * (i + 1),
                        image.cols() - 1,
                        image));
            }
            edge_segments = mod > 2 ? div + 1 : div;
            for (int i = 0; i < edge_segments; i++) {
                centers.add(new pixel(
                        image.rows() - 1,
                        (image.cols() - 1) / (edge_segments + 1) * (i + 1),
                        image));
            }
            edge_segments = div;
            for (int i = 0; i < edge_segments; i++) {
                centers.add(new pixel(
                        (image.rows() - 1) / (edge_segments + 1) * (i + 1),
                        0,
                        image));
            }
        } else {
            if (mod > 0) centers.add(new pixel(0, 0, image));
            if (mod > 1) centers.add(new pixel(0, image.cols() - 1, image));
            if (mod > 2) centers.add(new pixel(image.rows() - 1, image.cols() - 1, image));
        }
        return centers;
    }


    private static void add_unvisited_near_pixels_to_queue(
            Mat image,
            Mat centers_labels,
            Queue<queue_object> queue,
            queue_object queue_object
    ) {
        pixel pixel = queue_object.pixel;
        if ((pixel.y > 0) && ((int) centers_labels.get(pixel.y - 1, pixel.x)[0] == -1)) {
            queue.add(new queue_object(
                    new pixel(pixel.y - 1, pixel.x, image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
        if ((pixel.y < image.rows() - 1) && ((int) centers_labels.get(pixel.y + 1, pixel.x)[0] == -1)) {
            queue.add(new queue_object(
                    new pixel(pixel.y + 1, pixel.x, image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
        if ((pixel.x > 0) && ((int) centers_labels.get(pixel.y, pixel.x - 1)[0] == -1)) {
            queue.add(new queue_object(
                    new pixel(pixel.y, pixel.x - 1, image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
        if ((pixel.x < image.cols() - 1) && ((int) centers_labels.get(pixel.y, pixel.x + 1)[0] == -1)) {
            queue.add(new queue_object(
                    new pixel(pixel.y, pixel.x + 1, image),
                    queue_object.center_pixel,
                    queue_object.center_index
            ));
        }
    }

    private static void make_paper_center_first(List<pixel> centers, Mat centers_labels, int K) {
        int paper_cluster_index = -1;
        int paper_cluster_in_window_size = -1;
        Mat window_centers_labels = centers_labels.submat(
                (int) (centers_labels.rows() * (0.5 - center_window_size / 2)),
                (int) (centers_labels.rows() * (0.5 + center_window_size / 2)),
                (int) (centers_labels.cols() * (0.5 - center_window_size / 2)),
                (int) (centers_labels.cols() * (0.5 + center_window_size / 2))
        );
        for (int k = 0; k < K; k++) {
            Mat window_binary_center_mask = new Mat();
            Core.compare(window_centers_labels, new Scalar(k), window_binary_center_mask, Core.CMP_EQ);
            int cluster_in_window_size = (int) Core.sumElems(window_binary_center_mask).val[0] / 255;
            if (cluster_in_window_size > paper_cluster_in_window_size) {
                paper_cluster_index = k;
                paper_cluster_in_window_size = cluster_in_window_size;
            }
        }
        Collections.swap(centers, 0, paper_cluster_index);
        swap_values(centers_labels, 0, paper_cluster_index);
    }

    private static Mat get_calculated_markup(Mat centers_labels, int K) {
        Mat calculated_markup = new Mat();
        centers_labels.copyTo(calculated_markup);
        // Заменяем в рассчитанной маске все сегменты не нулевого на 255
        for (int k = 1; k < K; k++) {
            replace_values(calculated_markup, k, 255);
        }
        return calculated_markup;
    }

    private static double calculate_error(Mat centers_labels, int K, Mat expected_markup) {
        Mat calculated_markup = get_calculated_markup(centers_labels, K);
        Mat correct_binary_mask = new Mat();
        Core.compare(calculated_markup, expected_markup, correct_binary_mask, Core.CMP_EQ);
        double correct = Core.sumElems(correct_binary_mask).val[0]
                / 255 / centers_labels.rows() / centers_labels.cols();
        return 1 - correct;
    }

    public static segmentation_result ordinary_k_means(
            Mat image,
            int K,
            int iteration_count,
            center_init_method method,
            Mat image_markup,
            List<pixel> set_centers
    ) {
        Mat centers_labels = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);
        List<pixel> centers =
                method == center_init_method.RANDOM ? pick_random_centers(image, K) :
                        method == center_init_method.PLUS_PLUS ? pick_plus_plus_centers(image, K) :
                                method == center_init_method.PAPER ? pick_paper_centers(image, K) :
                                        method == center_init_method.SET ? set_centers :
                                                pick_random_centers(image, K);

        List<Double> iteration_errors = new ArrayList<>();
        List<Long> processing_times = new ArrayList<>();
        // Начинаем отсчет времени
        long start = System.currentTimeMillis();
        // Выполняем заданное количество итераций
        for (int i = 0; i < iteration_count; i++) {
            /* Прикрепляем пиксели к ближайшим центрам
            и находим среднее значения для смещения туда центра */
            for (int y = 0; y < image.rows(); y++) {
                for (int x = 0; x < image.cols(); x++) {
                    double min_distance = Double.MAX_VALUE;
                    int min_distance_center = -1;
                    for (int k = 0; k < centers.size(); k++) {
                        double distance = find_color_distance(
                                centers.get(k),
                                new Point3(image.get(y, x))
                        );
                        if (distance < min_distance) {
                            min_distance = distance;
                            min_distance_center = k;
                        }
                    }
                    centers_labels.put(y, x, min_distance_center);
                }
            }
            // Высчитываем новое положение каждого центра
            for (int k = 0; k < centers.size(); k++) {
                Mat binary_center_mask = new Mat();
                Core.compare(centers_labels, new Scalar(k), binary_center_mask, Core.CMP_EQ);
                pixel new_center = find_new_center(image, binary_center_mask, center_update_method.MEAN);
                centers.set(k, new_center);
            }
            // Добавляем общее время в накопительный массив
            processing_times.add(System.currentTimeMillis() - start);
            // Переупорядочиваем центры меток так, чтобы нулевой кластер был кластером бумаги
            make_paper_center_first(centers, centers_labels, K);
            // Считаем ошибку на этой итерации
            if (image_markup != null) iteration_errors.add(calculate_error(centers_labels, K, image_markup));
        }
        return new segmentation_result(centers_labels, centers, iteration_errors, processing_times);
    }


    public static segmentation_result constraints_k_medoids(
            Mat image,
            int K,
            int iteration_count,
            center_init_method method,
            Mat image_markup,
            List<pixel> set_centers
    ) {
        Mat centers_labels = new Mat(image.rows(), image.cols(), CvType.CV_8SC1);
        List<pixel> centers =
                method == center_init_method.RANDOM ? pick_random_centers(image, K) :
                        method == center_init_method.PLUS_PLUS ? pick_plus_plus_centers(image, K) :
                                method == center_init_method.PAPER ? pick_paper_centers(image, K) :
                                        method == center_init_method.SET ? set_centers :
                                                pick_random_centers(image, K);
        PriorityQueue<queue_object> queue = new PriorityQueue<>();

        List<Double> iteration_errors = new ArrayList<>();
        List<Long> processing_times = new ArrayList<>();
        // Начинаем отсчет времени
        long start = System.currentTimeMillis();
        // Выполняем заданное число итераций
        for (int i = 0; i < iteration_count; i++) {
            // На новой итерации метки центров сбрасываются
            centers_labels.convertTo(centers_labels, CvType.CV_8SC1);
            centers_labels.setTo(new Scalar(-1));
            // Добавляем центры в очередь с нулевым приоритетом
            for (int k = 0; k < centers.size(); k++) {
                queue.add(new queue_object(
                        centers.get(k), centers.get(k), k
                ));
            }
            // Запускаем очередь
            while (!queue.isEmpty()) {
                queue_object queue_object = queue.poll();
                pixel pixel = queue_object.pixel;
                // Проверка, что добавляемый пиксель еще не добавлен
                if ((int) centers_labels.get(pixel.y, pixel.x)[0] == -1) {
                    centers_labels.put(pixel.y, pixel.x, queue_object.center_index);
                    add_unvisited_near_pixels_to_queue(image, centers_labels, queue, queue_object);
                }
            }
            // Высчитываем новое положение каждого центра
            // Центр берется из кластера
            for (int k = 0; k < centers.size(); k++) {
                Mat binary_center_mask = new Mat();
                Core.compare(centers_labels, new Scalar(k), binary_center_mask, Core.CMP_EQ);
                pixel new_center = find_new_center(image, binary_center_mask, center_update_method.MEDOID);
                centers.set(k, new_center);
            }
            // Добавляем общее время в накопительный массив
            processing_times.add(System.currentTimeMillis() - start);
            // Когда все метки центров расставлены, переходим в только положительные значения
            centers_labels.convertTo(centers_labels, CvType.CV_8UC1);
            // Переупорядочиваем центры меток так, чтобы нулевой кластер был кластером бумаги
            make_paper_center_first(centers, centers_labels, K);
            // Считаем ошибку на этой итерации
            if (image_markup != null) iteration_errors.add(calculate_error(centers_labels, K, image_markup));
        }
        return new segmentation_result(centers_labels, centers, iteration_errors, processing_times);
    }

    public static Mat draw_segments(Mat image, List<pixel> centers, Mat centers_labels, int K) {
        Mat output_image = image.clone();
        for (int k = 0; k < K; k++) {
            // Бинаризационная маска текущего сегмента
            Mat binary_center_mask = new Mat();
            Core.compare(centers_labels, new Scalar(k), binary_center_mask, Core.CMP_EQ);
            output_image.setTo(new Scalar(centers.get(k).b, centers.get(k).g, centers.get(k).r), binary_center_mask);
        }
        return output_image;
    }

    public static Mat draw_contours(Mat image, Mat centers_labels, int K) {
        Mat output_image = image.clone();
        for (int k = 0; k < K; k++) {
            // Бинаризационная маска текущего сегмента
            Mat binary_center_mask = new Mat();
            Core.compare(centers_labels, new Scalar(k), binary_center_mask, Core.CMP_EQ);
            // Осуществляем отсуп внутри каждого сегмента, чтобы границы были более явными
            Mat erode_shape = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(6, 6));
            Mat erode_binary_center_mask = new Mat();
            Imgproc.erode(binary_center_mask, erode_binary_center_mask, erode_shape);
            // Нахождение контуров текущего сегмента
            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(erode_binary_center_mask, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
            // Отрисовка всех контуров кластера цветом по очереди
            Imgproc.drawContours(
                    output_image,
                    contours,
                    -1,
                    colors[k],
                    8
            );
        }
        return output_image;
    }

    public static Mat cut_edges(Mat image, Mat centers_labels, int K) {
        Mat calculated_markup = get_calculated_markup(centers_labels, K);
        Mat whole_result_image = image.clone();
        whole_result_image.setTo(new Scalar(255, 255, 255), calculated_markup);
        // Нахождение минимальной и максимальной границы значащих пикселей
        int min_y = Integer.MAX_VALUE;
        int max_y = Integer.MIN_VALUE;
        int min_x = Integer.MAX_VALUE;
        int max_x = Integer.MIN_VALUE;
        for (int y = 0; y < calculated_markup.rows(); ++y) {
            for (int x = 0; x < calculated_markup.cols(); ++x) {
                if ((int) calculated_markup.get(y, x)[0] == 0) { // Значимая точка
                    min_x = Math.min(min_x, x);
                    min_y = Math.min(min_y, y);
                    max_x = Math.max(max_x, x);
                    max_y = Math.max(max_y, y);
                }
            }
        }
        return whole_result_image.submat(min_y, max_y, min_x, max_x);
    }

}
