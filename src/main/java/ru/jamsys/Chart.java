package ru.jamsys;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ru.jamsys.core.flat.trend.PolyTrendLine;
import ru.jamsys.core.flat.util.UtilTrend;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

public class Chart {

    public static void main(String[] args) {
        List<Double> list = new ArrayList<>();
        String s = "0,0,0,0,1,2,2,2,2,2,2,2,4,4,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,6,6,6,6,7,8,8,8,8,8,8,8,8,8,9,10,11,12,13,14,14,16,16,16,16,16,17,17,18,18,18,18,18,19,21,23,24,26,26,26,26,26,27,29,29,30,30,30,30,31,31,31,31,31,31,31,31,32,32,33,33,35,36,37,38,39,39,41,41,41,44,46,47,48,48,49,50,50,50,50,51,51,52,52,52,53,53,53,54,55";
        //String s = "0.0,2.0,3.0,4.0,5.0,6.0,6.0,8.0,8.0,8.0,11.0,13.0,14.0,15.0,15.0,16.0,17.0,17.0,17.0,17.0,18.0,18.0,19.0,19.0,19.0,20.0,20.0,20.0,21.0,22.0";
        int offsetGretsky = 17;


        //String s = "0,0,0,0,1,2,2,2,2,2,2,2,4,4,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,6,6,6,6,7,8,8,8,8,8,8,8,8,8,9,10,11,12,13,14,14,16,16,16,16,16,17,17,18,18,18,18,18,19,21,23,24,26,26,26,26,26,27,29,29,30,30,30,30,31,31,31,31,31,31,31,31,32,32,33,33,35,36,37,38,39,39,41,41,41,44,46,47,48,48,49,50,50,50,50,51,51,52,52,52,53,53,53,54,55,56";
        //String s = "0.0,2.0,3.0,4.0,5.0,6.0,6.0,8.0,8.0,8.0,11.0,13.0,14.0,15.0,15.0,16.0,17.0,17.0,17.0,17.0,18.0,18.0,19.0,19.0,19.0,20.0,20.0,20.0,21.0,22.0,23.0";
        //int offsetGretsky = 16;

        Color trans = new Color(0xFF, 0xFF, 0xFF, 0);

        String[] split = s.split(",");
        int countGame = split.length;
        int curLine = (int) Double.parseDouble(split[split.length - 1]);
        int needGoals = (int) Double.parseDouble(split[split.length - 1]) + offsetGretsky;
        UtilTrend.XY xy = new UtilTrend.XY();
        for (String string : split) {
            xy.addY(Double.parseDouble(string));
        }

        PolyTrendLine polyTrendLine1 = new PolyTrendLine(1);
        polyTrendLine1.setValues(xy.getY(), xy.getX());

        PolyTrendLine polyTrendLine2 = new PolyTrendLine(2);
        polyTrendLine2.setValues(xy.getY(), xy.getX());

        PolyTrendLine polyTrendLine3 = new PolyTrendLine(3);
        polyTrendLine3.setValues(xy.getY(), xy.getX());

        PolyTrendLine polyTrendLine4 = new PolyTrendLine(4);
        polyTrendLine4.setValues(xy.getY(), xy.getX());

        PolyTrendLine polyTrendLine5 = new PolyTrendLine(5);
        polyTrendLine5.setValues(xy.getY(), xy.getX());


        // Создаем набор данных
        XYSeries series = new XYSeries("1");
        XYSeries seriesPolyAvg = new XYSeries("2");

        XYSeries seriesPoly1 = new XYSeries("3");
        XYSeries seriesPoly2 = new XYSeries("4");
        XYSeries seriesPoly3 = new XYSeries("5");
        XYSeries seriesPoly4 = new XYSeries("6");
        XYSeries seriesPoly5 = new XYSeries("7");

        xy.getXy().forEach((x, y) -> {
            series.add(x, y);
            seriesPoly1.add((double) x, polyTrendLine1.predict(x));
            seriesPoly2.add((double) x, polyTrendLine2.predict(x) - 0.2f);
            seriesPoly3.add((double) x, polyTrendLine3.predict(x) - 0.4f);
            seriesPoly4.add((double) x, polyTrendLine4.predict(x) - 0.6f);
            seriesPoly5.add((double) x, polyTrendLine5.predict(x) - 0.8f);
        });

        int size = xy.getXy().size();
        int findXGame = 0;
        for (int i = 1; i < 50; i++) {
            int realI = i + size;

            double base = addPredict(seriesPoly1, polyTrendLine1, realI, 0, needGoals);

            Double v2 = addPredict(seriesPoly2, polyTrendLine2, realI, -0.2f, needGoals);
            Double v3 = addPredict(seriesPoly3, polyTrendLine3, realI, -0.4f, needGoals);
            Double v4 = addPredict(seriesPoly4, polyTrendLine4, realI, -0.6f, needGoals);
            Double v5 = addPredict(seriesPoly5, polyTrendLine5, realI, -0.8f, needGoals);

            DoubleSummaryStatistics avg = new DoubleSummaryStatistics();

            avg.accept(getAvg(base, v2));
            avg.accept(getAvg(base, v3));
            avg.accept(getAvg(base, v4));
            avg.accept(getAvg(base, v5));

            double average = avg.getAverage();

            if (average > 0 && average <= needGoals + 3) {
                if (Math.round(average) == needGoals) {
                    findXGame = realI;
                }
                seriesPolyAvg.add(realI, average);
            }
        }

        // Создаем коллекцию данных
        XYSeriesCollection dataset = new XYSeriesCollection();

        dataset.addSeries(series);
        dataset.addSeries(seriesPolyAvg);
        dataset.addSeries(seriesPoly1);

        dataset.addSeries(seriesPoly2);
        dataset.addSeries(seriesPoly3);
        dataset.addSeries(seriesPoly4);
        dataset.addSeries(seriesPoly5);

        // Создаем график
        JFreeChart chart = ChartFactory.createXYLineChart(null, "Игры", "Голы", dataset);
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);

        plot.addRangeMarker(getMarkerHor(curLine, "Y = " + curLine));
        plot.addRangeMarker(getMarkerHor(needGoals, "Y = " + needGoals));
        plot.addDomainMarker(getMarkerVer(countGame, "X = " + countGame));
        if (findXGame > 0) {
            plot.addDomainMarker(getMarkerVer(findXGame, "X = " + findXGame));
        }

        // Меняем цвета линий
        plot.getRenderer().setSeriesPaint(0, Color.BLACK);
        plot.getRenderer().setSeriesPaint(1, Color.BLACK);

        plot.getRenderer().setSeriesPaint(2, Color.GRAY); // middle

        plot.getRenderer().setSeriesPaint(3, Color.PINK);
        plot.getRenderer().setSeriesPaint(4, Color.GREEN);
        plot.getRenderer().setSeriesPaint(5, Color.ORANGE);
        plot.getRenderer().setSeriesPaint(6, Color.CYAN);

        plot.getRenderer().setSeriesPaint(7, Color.GRAY);
        plot.getRenderer().setSeriesPaint(8, Color.GRAY);

        float[] dashPattern = {0f, 6f}; // 5 пикселей линия, 5 пикселей пробел
        BasicStroke dashedStroke = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 50.0f, dashPattern, 0.0f);

        float[] dashPattern2 = {5f, 5f}; // 5 пикселей линия, 5 пикселей пробел //,
        BasicStroke dashedStroke2 = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.0f, dashPattern2, 0.0f);

        float[] dashPattern3 = {0f, 500f}; // 5 пикселей линия, 5 пикселей пробел //,
        BasicStroke dashedStroke3 = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dashPattern3, 0.0f);

        Shape circle = new Ellipse2D.Double(-2, -2, 4, 4); // Радиус 3 пикселя

        int[] xPoints = {0, -3, 0, 3};
        int[] yPoints = {3, 0, -3, 0};
        Shape diamond = new Polygon(xPoints, yPoints, 4);
        Shape cross = new Line2D.Double(-3, -3, 3, 3); // Одна диагональ
        int[] xPoints2 = {-3, 3, 0};
        int[] yPoints2 = {3, 3, -3};
        Shape triangle = new Polygon(xPoints2, yPoints2, 3);

        renderer.setSeriesShape(1, diamond);
        renderer.setSeriesShapesVisible(1, true);


        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesShapesVisible(2, false);
        renderer.setSeriesShapesVisible(3, false);
        renderer.setSeriesShapesVisible(4, false);
        renderer.setSeriesShapesVisible(5, false);
        renderer.setSeriesShapesVisible(6, false);
        renderer.setSeriesShapesVisible(7, false);
        renderer.setSeriesShapesVisible(8, false);


        renderer.setSeriesStroke(0, new BasicStroke(2f));

        renderer.setSeriesStroke(1, dashedStroke3);
        renderer.setSeriesStroke(2, dashedStroke2);
        renderer.setSeriesStroke(3, dashedStroke);
        renderer.setSeriesStroke(4, dashedStroke);
        renderer.setSeriesStroke(5, dashedStroke);
        renderer.setSeriesStroke(6, dashedStroke);

        chart.removeLegend();
        chart.getPlot().setBackgroundPaint(trans);
        chart.getPlot().setOutlineVisible(false);
        chart.getXYPlot().setRangeGridlinesVisible(false);
        chart.getXYPlot().setDomainGridlinesVisible(false);
        chart.setAntiAlias(true);

        try {
            File file = new File("chart_" + offsetGretsky + "_" + countGame + ".png"); // Имя файла
            ChartUtils.saveChartAsPNG(file, chart, 1200, 800); // Ширина и высота изображения
            System.out.println("График сохранен в файл: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Double addPredict(XYSeries xySeries, PolyTrendLine polyTrendLine, int x, double offset, int maxLine) {
        double predict = polyTrendLine.predict(x);
        if (predict > maxLine) {
            return predict;
        }
        if (predict < 0) {
            return predict;
        }
        xySeries.add(x, predict + offset);
        return predict;
    }

    public static double getAvg(double base, double p1) {
        double w = Math.abs(base - p1);
        double prc = w * 100 / 120;
        if (prc < 0) {
            prc = 0;
        }
        if (prc > 100) {
            prc = 100;
        }
        double y = prc * w / 100;
        if (y < 1) {
            y = 1;
        }
        double average;
        if (base > p1) {
            average = base - w / y;
        } else {
            average = base + w / y;
        }
        return average;
    }

    public static ValueMarker getMarkerVer(double countGame, String label) {
        ValueMarker verticalMarker = new ValueMarker(countGame);
        verticalMarker.setPaint(Color.GRAY);
        verticalMarker.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[]{5f, 5f}, 0.0f));
        verticalMarker.setLabel(label);
        verticalMarker.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        verticalMarker.setLabelBackgroundColor(new Color(0xFF, 0xFF, 0xFF, 0));
        verticalMarker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
        verticalMarker.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
        return verticalMarker;
    }

    public static ValueMarker getMarkerHor(double curLine, String label) {
        ValueMarker horizontalMarker = new ValueMarker(curLine);
        horizontalMarker.setPaint(Color.GRAY);
        horizontalMarker.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10.0f, new float[]{5f, 5f}, 0.0f));
        horizontalMarker.setLabel(label);
        horizontalMarker.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        horizontalMarker.setLabelBackgroundColor(new Color(0xFF, 0xFF, 0xFF, 0));
        horizontalMarker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        horizontalMarker.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
        return horizontalMarker;
    }

}
