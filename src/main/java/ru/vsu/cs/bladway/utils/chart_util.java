package ru.vsu.cs.bladway.utils;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Range;
import org.opencv.imgcodecs.Imgcodecs;
import ru.vsu.cs.bladway.enums.center_init_method;
import ru.vsu.cs.bladway.enums.segmentation_method;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class chart_util {
    public static void show_image(Mat image, String title) {
        MatOfByte imageMatOfByte = new MatOfByte();
        Imgcodecs.imencode(".png", image, imageMatOfByte);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setTitle(title);
        JLabel label = new JLabel();
        frame.getContentPane().add(label);
        ImageIcon imageImageIcon = new ImageIcon(imageMatOfByte.toArray());
        label.setIcon(imageImageIcon);
        frame.pack();
    }

    public static Mat get_ci_seg_methods_xy_chart_image(
            Map<Pair<center_init_method, segmentation_method>, Double[][]> values,
            String title,
            String x_axis_label,
            String y_axis_label
    ) throws IOException {
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (Pair<center_init_method, segmentation_method> pair : values.keySet()) {
            XYSeries series = new XYSeries(pair.getKey() + " + " + pair.getValue());
            for (int i = 0; i < values.get(pair)[0].length; i++) {
                series.add(values.get(pair)[0][i], values.get(pair)[1][i]);
            }
            dataset.addSeries(series);
        }
        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                x_axis_label,
                y_axis_label,
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );


        chart.getTitle().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        plot.getRangeAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        plot.getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        plot.getRangeAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        LegendItemCollection old_legend_collection = plot.getLegendItems();
        LegendItemCollection new_legend_collection = new LegendItemCollection();
        for (int i = 0; i < old_legend_collection.getItemCount(); i++) {
            LegendItem new_legend_item = new LegendItem(
                    old_legend_collection.get(i).getLabel(), old_legend_collection.get(i).getDescription(),
                    old_legend_collection.get(i).getToolTipText(), old_legend_collection.get(i).getURLText(),
                    true, new Rectangle2D.Double(0, 0, 15, 15),
                    true, old_legend_collection.get(i).getFillPaint(),
                    false, old_legend_collection.get(i).getOutlinePaint(),
                    old_legend_collection.get(i).getOutlineStroke(), true,
                    old_legend_collection.get(i).getLine(), old_legend_collection.get(i).getLineStroke(),
                    old_legend_collection.get(i).getLinePaint());
            new_legend_item.setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            new_legend_collection.add(new_legend_item);
        }
        plot.setFixedLegendItems(new_legend_collection);
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            plot.getRenderer().setSeriesStroke(i, new BasicStroke(8f));
        }


        BufferedImage image_base = chart.createBufferedImage(1000, 750);
        BufferedImage image =
                new BufferedImage(image_base.getWidth(), image_base.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        image.getGraphics().drawImage(image_base, 0, 0, null);
        ByteArrayOutputStream image_stream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", image_stream);
        return Imgcodecs.imdecode(
                new MatOfByte(image_stream.toByteArray()),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public static void print_mat(Mat imageMat, String title) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setTitle(title);
        frame.setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        StringBuilder resultText = new StringBuilder();
        for (int i = 0; i < imageMat.rows(); i++) {
            for (int j = 0; j < imageMat.cols(); j++) {
                resultText.append("[");
                for (int k = 0; k < imageMat.get(i, j).length; k++) {
                    resultText.append(imageMat.get(i, j)[k]);
                    resultText.append(", ");
                }
                resultText.append("]");
            }
            resultText.append("\n");
        }

        textArea.setText(resultText.toString());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.pack();
    }

    public static void print_mat(Mat image, String title, Range x_range, Range y_range) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setTitle(title);
        frame.setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        Mat littleImageMat = image.colRange(x_range).rowRange(y_range);
        StringBuilder resultText = new StringBuilder();
        for (int i = 0; i < littleImageMat.rows(); i++) {
            for (int j = 0; j < littleImageMat.cols(); j++) {
                resultText.append("[");
                for (int k = 0; k < littleImageMat.get(i, j).length; k++) {
                    resultText.append(littleImageMat.get(i, j)[k]);
                    resultText.append(", ");
                }
                resultText.append("]");
            }
            resultText.append("\n");
        }

        textArea.setText(resultText.toString());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.pack();
    }

}
