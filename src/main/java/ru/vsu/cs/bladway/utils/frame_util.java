package ru.vsu.cs.bladway.utils;

import lombok.RequiredArgsConstructor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Range;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RequiredArgsConstructor
public class frame_util {
    public static void showImage(Mat imageMat, String title) {
        MatOfByte imageMatOfByte = new MatOfByte();
        Imgcodecs.imencode(".jpg", imageMat, imageMatOfByte);

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

    public static void showImageText(Mat imageMat, String title) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setTitle(title);
        frame.setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        String resultText = "";
        for (int i = 0; i < imageMat.rows(); i++) {
            for (int j = 0; j < imageMat.cols(); j++) {
                resultText += "[";
                for (int k = 0; k < imageMat.get(i, j).length; k++) {
                    resultText += imageMat.get(i, j)[k];
                    resultText += ", ";
                }
                resultText += "]";
            }
            resultText += "\n";
        }

        textArea.setText(resultText);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.pack();
    }

    public static void showImageTextScale(Mat imageMat, String title, double scaleX, double scaleY) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setTitle(title);
        frame.setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        Mat littleImageMat = new Mat();
        Imgproc.resize(imageMat, littleImageMat, new Size(imageMat.cols()*scaleX, imageMat.rows()*scaleY));
        String resultText = "";
        for (int i = 0; i < littleImageMat.rows(); i++) {
            for (int j = 0; j < littleImageMat.cols(); j++) {
                resultText += "[";
                for (int k = 0; k < littleImageMat.get(i, j).length; k++) {
                    resultText += littleImageMat.get(i, j)[k];
                    resultText += ", ";
                }
                resultText += "]";
            }
            resultText += "\n";
        }

        textArea.setText(resultText);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.pack();
    }

    public static void showImageTextCut(Mat imageMat, String title, Range cutX, Range cutY) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setTitle(title);
        frame.setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        Mat littleImageMat = new Mat();
        littleImageMat = imageMat.colRange(cutX).rowRange(cutY);
        String resultText = "";
        for (int i = 0; i < littleImageMat.rows(); i++) {
            for (int j = 0; j < littleImageMat.cols(); j++) {
                resultText += "[";
                for (int k = 0; k < littleImageMat.get(i, j).length; k++) {
                    resultText += littleImageMat.get(i, j)[k];
                    resultText += ", ";
                }
                resultText += "]";
            }
            resultText += "\n";
        }

        textArea.setText(resultText);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.pack();
    }

    public static Mat getChartImage(double[][] values, int K, int passages) throws IOException {
        DefaultCategoryDataset categoryDataset = new DefaultCategoryDataset();
        for (int i = 0; i < values.length; i++) {
            categoryDataset.addValue(values[i][0], "OpenCVRnd", (i + 1) + "");
            categoryDataset.addValue(values[i][1], "OpenCVPlusPlus", (i + 1) + "");
            categoryDataset.addValue(values[i][2], "OwnRnd", (i + 1) + "");
            categoryDataset.addValue(values[i][3], "OwnPlusPlus", (i + 1) + "");
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "D на изображение, на каждый из алгоритмов. Значения усреднены по всем проходам. Количество кластеров: " + K + " Количество проходов: " + passages,
                "Номер фотографии из датасета",
                "D - внутрикластерная дисперсия для всего изображения",
                categoryDataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
        chart.getTitle().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 40));

        CategoryPlot plot = chart.getCategoryPlot();

        plot.getDomainAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        plot.getRangeAxis().setLabelFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));

        plot.getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
        plot.getRangeAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDefaultLegendTextFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
        renderer.setDefaultLegendShape(new Rectangle2D.Double(0, 0, 50, 50));


        BufferedImage chartImageInt = chart.createBufferedImage(1920, 1080);
        BufferedImage chartImageByte = new BufferedImage(chartImageInt.getWidth(), chartImageInt.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        chartImageByte.getGraphics().drawImage(chartImageInt, 0, 0, null);

        ByteArrayOutputStream byteArrayOutputStreamChart = new ByteArrayOutputStream();
        ImageIO.write(chartImageByte, "jpg", byteArrayOutputStreamChart);
        byteArrayOutputStreamChart.flush();

        return Imgcodecs.imdecode(
                new MatOfByte(byteArrayOutputStreamChart.toByteArray()),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

}
