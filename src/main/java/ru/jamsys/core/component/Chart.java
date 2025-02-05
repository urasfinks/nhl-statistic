package ru.jamsys.core.component;

import lombok.Getter;
import lombok.Setter;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.flat.trend.PolyTrendLine;
import ru.jamsys.core.flat.util.UtilTrend;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.DoubleSummaryStatistics;

@Setter
@Component
@Lazy
public class Chart extends RepositoryPropertiesField {

    @PropertyName("run.args.chart.folder")
    private String folder;

    public Chart(ServiceProperty serviceProperty) {
        autoFill(serviceProperty);
    }

    @Getter
    @Setter
    public static class Response {
        String pathChart;
        int countGame;
        int needCountGoals;
        int initGame;
        int initCountGoals;
    }

    public Response createChart(UtilTrend.XY xy, int offsetGretsky) {
        Response response = new Response();
        response.setInitGame(xy.getY().length);

        int countGame = xy.getXy().size();
        String fileName = "chart_" + offsetGretsky + "_" + countGame + ".png";
        File file = new File(folder + "/" + fileName); // Имя файла
        response.setPathChart(file.getAbsolutePath());

        double currentGoals = xy.getY()[xy.getY().length - 1];
        response.setInitCountGoals((int) currentGoals);
        int needGoals = (int) currentGoals + offsetGretsky;
        response.setNeedCountGoals(needGoals);

        PolyTrendLine polyTrendLine1 = new PolyTrendLine(1, xy.getY(), xy.getX());
        PolyTrendLine polyTrendLine2 = new PolyTrendLine(2, xy.getY(), xy.getX());
        PolyTrendLine polyTrendLine3 = new PolyTrendLine(3, xy.getY(), xy.getX());
        PolyTrendLine polyTrendLine4 = new PolyTrendLine(4, xy.getY(), xy.getX());
        PolyTrendLine polyTrendLine5 = new PolyTrendLine(5, xy.getY(), xy.getX());

        // Создаем набор данных
        XYSeries seriesGoals = new XYSeries("goals");
        XYSeries seriesPolyPredict = new XYSeries("predict");

        XYSeries seriesPoly1 = new XYSeries("base");
        XYSeries seriesPoly2 = new XYSeries("p2");
        XYSeries seriesPoly3 = new XYSeries("p3");
        XYSeries seriesPoly4 = new XYSeries("p4");
        XYSeries seriesPoly5 = new XYSeries("p5");

        xy.getXy().forEach((x, y) -> {
            seriesGoals.add(x, y);
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
            if (average > needGoals && findXGame == 0) {
                findXGame = realI;
                response.setCountGame(findXGame - countGame);
            }
            if (average > 0 && average <= needGoals + 3) {
                seriesPolyPredict.add(realI, average);
            }
        }

        if (file.exists()) {
            //System.out.println("File: " + file.getAbsolutePath() + " exist");
            return response;
        }

        XYSeriesCollection dataset = new XYSeriesCollection();

        dataset.addSeries(seriesGoals);
        dataset.addSeries(seriesPolyPredict);
        dataset.addSeries(seriesPoly1);

        dataset.addSeries(seriesPoly2);
        dataset.addSeries(seriesPoly3);
        dataset.addSeries(seriesPoly4);
        dataset.addSeries(seriesPoly5);

        // Создаем график
        JFreeChart chart = ChartFactory.createXYLineChart(null, "Игры", "Голы", dataset);
        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 13));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.PLAIN, 13));
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);

        plot.addRangeMarker(getMarkerHor(currentGoals, "Y = " + (int) currentGoals));
        plot.addRangeMarker(getMarkerHor(needGoals, "Y = " + needGoals));

        plot.addDomainMarker(getMarkerVer(countGame, "X = " + countGame));
        if (findXGame > 0) {
            plot.addDomainMarker(getMarkerVer(findXGame, "X = " + findXGame));
        }

        plot.getRenderer().setSeriesPaint(0, Color.BLACK);
        plot.getRenderer().setSeriesPaint(1, Color.BLACK);
        plot.getRenderer().setSeriesPaint(2, Color.GRAY); // middle
        plot.getRenderer().setSeriesPaint(3, Color.PINK);
        plot.getRenderer().setSeriesPaint(4, Color.GREEN);
        plot.getRenderer().setSeriesPaint(5, Color.ORANGE);
        plot.getRenderer().setSeriesPaint(6, Color.CYAN);

        float[] dashPattern = {0f, 6f}; // 5 пикселей линия, 5 пикселей пробел
        BasicStroke dashedStroke = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 50.0f, dashPattern, 0.0f);

        float[] dashPattern2 = {5f, 5f}; // 5 пикселей линия, 5 пикселей пробел //,
        BasicStroke dashedStroke2 = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.0f, dashPattern2, 0.0f);

        float[] dashPattern3 = {0f, 500f}; // 5 пикселей линия, 5 пикселей пробел //,
        BasicStroke dashedStroke3 = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, dashPattern3, 0.0f);

        int[] xPoints = {0, -2, 0, 2};
        int[] yPoints = {2, 0, -2, 0};
        Shape diamond = new Polygon(xPoints, yPoints, 4);

        renderer.setSeriesShape(1, diamond);
        renderer.setSeriesShapesVisible(0, false);
        renderer.setSeriesShapesVisible(1, true);
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
        chart.getPlot().setBackgroundPaint(new Color(0xFF, 0xFF, 0xFF, 0));
        chart.getPlot().setOutlineVisible(false);
        chart.getXYPlot().setRangeGridlinesVisible(false);
        chart.getXYPlot().setDomainGridlinesVisible(false);
        chart.setAntiAlias(true);

        try {

            ChartUtils.saveChartAsPNG(file, chart, 800, 600); // Ширина и высота изображения
            //System.out.println("График сохранен в файл: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
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
        verticalMarker.setLabelFont(new Font("SansSerif", Font.PLAIN, 13));
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
        horizontalMarker.setLabelFont(new Font("SansSerif", Font.PLAIN, 13));
        horizontalMarker.setLabelBackgroundColor(new Color(0xFF, 0xFF, 0xFF, 0));
        horizontalMarker.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
        horizontalMarker.setLabelTextAnchor(TextAnchor.BOTTOM_RIGHT);
        return horizontalMarker;
    }

}
