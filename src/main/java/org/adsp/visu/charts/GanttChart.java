package org.adsp.visu.charts;

// based from : https://stackoverflow.com/questions/27975898/gantt-chart-from-scratch

import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.*;

public class GanttChart<X,Y> extends XYChart<X,Y> {

    protected static class GanttElement {

        public double length;
        public String styleClass;
        public String label;


        public GanttElement(double lengthMs, String styleClass, String label) {
            this.length = lengthMs;
            this.styleClass = styleClass;
            this.label = label;
        }

        public GanttElement(double lengthMs, String styleClass) {
            this(lengthMs, styleClass, "");
        }

        public double getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
        }

        public String getStyleClass() {
            return styleClass;
        }

        public void setStyleClass(String styleClass) {
            this.styleClass = styleClass;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    /**
     * time block in the gant chart
     */
    public static class TimeBlock extends GanttElement {
        public TimeBlock(double lengthMs, String styleClass) {
            super(lengthMs, styleClass);
        }
        public TimeBlock(double lengthMs, String styleClass, String label) {
            super(lengthMs, styleClass, label);
        }
    }

    /**
     * transition from one time block to another
     */
    public static class Transition extends GanttElement {
        public Transition(double lengthMs, String styleClass) {
            super(lengthMs, styleClass);
        }
    }

    private double blockHeight = 10;

    public GanttChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis) {
        this(xAxis, yAxis, FXCollections.<Series<X, Y>>observableArrayList());
    }

    public GanttChart(@NamedArg("xAxis") Axis<X> xAxis, @NamedArg("yAxis") Axis<Y> yAxis, @NamedArg("data") ObservableList<Series<X,Y>> data) {
        super(xAxis, yAxis);
        if (!(xAxis instanceof ValueAxis && yAxis instanceof CategoryAxis)) {
            throw new IllegalArgumentException("Axis type incorrect, X and Y should both be NumberAxis");
        }
        setData(data);
    }

    private static String getStyleClass( Object obj) {
        return ((GanttElement) obj).getStyleClass();
    }

    protected static double getLength( Object obj) {
        return ((GanttElement) obj).getLength();
    }

    protected static String getLabel( Object obj) {
        return ((GanttElement) obj).getLabel();
    }

    @Override protected void layoutPlotChildren() {

        // will be used to compute the difference in y values
        double firstY = Double.POSITIVE_INFINITY;
        double secondY = Double.POSITIVE_INFINITY;

        // draw the blocks
        for (Series<X,Y> series : getData()) {
            //Series<X,Y> series = getData().get(seriesIndex);

            Iterator<Data<X,Y>> iter = getDisplayedDataIterator(series);
            while(iter.hasNext()) {
                Data<X,Y> item = iter.next();
                if (item.getExtraValue() instanceof TimeBlock) {
                    double x = getXAxis().getDisplayPosition(item.getXValue());
                    double y = getYAxis().getDisplayPosition(item.getYValue());
                    if (Double.isNaN(x) || Double.isNaN(y)) {
                        if (item.getNode() != null)
                            item.getNode().setVisible(false);
                        continue;
                    }
                    if(y < firstY){
                        secondY = firstY;
                        firstY = y;
                    } else if(y < secondY) secondY = y;
                    Node block = item.getNode();
                    Rectangle ellipse;
                    if (block != null) {
                        block.setVisible(true);
                        if (block instanceof StackPane) {
                            StackPane region = (StackPane) item.getNode();
                            boolean isBlock = item.getExtraValue() instanceof TimeBlock;
                            if (region.getShape() == null && isBlock) {
                                ellipse = new Rectangle();
                            } else if (region.getShape() instanceof Rectangle) {
                                ellipse = (Rectangle) region.getShape();
                            } else {
                                return;
                            }
                            if (isBlock) {
                                ellipse.setWidth(getLength(item.getExtraValue()) * ((getXAxis() instanceof NumberAxis) ? Math.abs(((NumberAxis) getXAxis()).getScale()) : 1));
                                ellipse.setHeight(getBlockHeight() * ((getYAxis() instanceof NumberAxis) ? Math.abs(((NumberAxis) getYAxis()).getScale()) : 1));
                                String label = getLabel(item.getExtraValue());
                                if(!label.isBlank()){
                                    Tooltip tooltip = new Tooltip(getLabel(item.getExtraValue()));
                                    tooltip.setShowDelay(new Duration(0));
                                    Tooltip.install(block, tooltip);
                                }
                            }
                            y -= getBlockHeight() / 2.0;

                            // Note: workaround for RT-7689 - saw this in ProgressControlSkin
                            // The region doesn't update itself when the shape is mutated in place, so we
                            // null out and then restore the shape in order to force invalidation.
                            region.setShape(null);
                            region.setShape(ellipse);
                            region.setScaleShape(false);
                            region.setCenterShape(false);
                            region.setCacheShape(false);

                            block.setLayoutX(x);
                            block.setLayoutY(y);
                        }
                    }
                }
            }
        }
        // compute the difference between the values in yvalues
        double ySpacing = firstY == Double.POSITIVE_INFINITY || secondY == Double.POSITIVE_INFINITY ? 0 : secondY - firstY;

        // draw the lines between the blocks
        for (int seriesIndex=0; seriesIndex < getData().size(); seriesIndex++) {
            Series<X,Y> series = getData().get(seriesIndex);

            Iterator<Data<X,Y>> iter = getDisplayedDataIterator(series);
            while(iter.hasNext()) {
                Data<X,Y> item = iter.next();
                if (item.getExtraValue() instanceof Transition) {
                    double x = getXAxis().getDisplayPosition(item.getXValue());
                    double y = getYAxis().getDisplayPosition(item.getYValue());
                    if (Double.isNaN(x) || Double.isNaN(y)) {
                        if (item.getNode() != null)
                            item.getNode().setVisible(false);
                        continue;
                    }
                    Node block = item.getNode();
                    block.setVisible(true);
                    Line line = (Line) block;
                    // start from this series
                    ((Line) block).setStartY(getBlockHeight() / 2);
                    // goes until the next series
                    line.setEndX(getLength(item.getExtraValue()) * ((getXAxis() instanceof NumberAxis) ? Math.abs(((NumberAxis) getXAxis()).getScale()) : 1));
                    ((Line) block).setEndY(ySpacing - getBlockHeight() /2);
                    block.setLayoutX(x);
                    block.setLayoutY(y);
                }
            }
        }
    }

    public double getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight( double blockHeight) {
        this.blockHeight = blockHeight;
    }

    @Override protected void dataItemAdded(Series<X,Y> series, int itemIndex, Data<X,Y> item) {
        Node block = createContainer(series, getData().indexOf(series), item, itemIndex);
        getPlotChildren().add(block);
    }

    @Override protected  void dataItemRemoved(final Data<X,Y> item, final Series<X,Y> series) {
        final Node block = item.getNode();
        getPlotChildren().remove(block);
        removeDataItemFromDisplay(series, item);
    }

    @Override protected void dataItemChanged(Data<X, Y> item) {
    }

    @Override protected  void seriesAdded(Series<X,Y> series, int seriesIndex) {
        for (int j=0; j<series.getData().size(); j++) {
            Data<X,Y> item = series.getData().get(j);
            Node container = createContainer(series, seriesIndex, item, j);
            getPlotChildren().add(container);
        }
    }

    @Override protected  void seriesRemoved(final Series<X,Y> series) {
        for (Data<X,Y> d : series.getData()) {
            final Node container = d.getNode();
            getPlotChildren().remove(container);
        }
        removeSeriesFromDisplay(series);
    }


    private Node createContainer(Series<X, Y> series, int seriesIndex, final Data<X,Y> item, int itemIndex) {

        Node container = item.getNode();
        boolean isExtractData = item.getExtraValue() instanceof GanttChart.TimeBlock;

        if (container == null && isExtractData) {
            container = new StackPane();
            item.setNode(container);
        } else if (container == null) {
            container = new Line();
            item.setNode(container);
        }

        if (isExtractData) {
            container.getStyleClass().add(getStyleClass(item.getExtraValue()));
        } else {
            container.getStyleClass().add("dashed-line");
        }

        return container;
    }

    @Override protected void updateAxisRange() {
        final Axis<X> xa = getXAxis();
        final Axis<Y> ya = getYAxis();
        List<X> xData = null;
        List<Y> yData = null;
        if(xa.isAutoRanging()) xData = new ArrayList<X>();
        if(ya.isAutoRanging()) yData = new ArrayList<Y>();
        if(xData != null || yData != null) {
            for(Series<X,Y> series : getData()) {
                for(Data<X,Y> data: series.getData()) {
                    if (data.getExtraValue() instanceof GanttChart.TimeBlock) { // block
                        if (xData != null) {
                            xData.add(data.getXValue());
                            xData.add(xa.toRealValue(xa.toNumericValue(data.getXValue()) + getLength(data.getExtraValue())));
                        }
                        if (yData != null) {
                            yData.add(data.getYValue());
                        }
                    } else { // line
                        //TODO
                    }
                }
            }
            if(xData != null) xa.invalidateRange(xData);
            if(yData != null) ya.invalidateRange(yData);
        }
    }

    /**
     * set the css file that will be used to provide the layout for the elements
     * either specify a css file in the same package or the complete path to a css file from another package
     * @param stylesheet name of the css file (with extension) that will provide the layout for the elements
     */
    public void setStylesheet(String stylesheet) {
        this.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
    }

    /**
     * add all given elements into the chart
     * @param val elements that needs to be added to the chart
     */
    public void addAll(Collection<? extends Series<X, Y>> val) {
        this.getData().addAll(val);
    }

    /**
     * add an element into the chart
     * @param val element that needs to be added to the chart
     */
    public void add(Series<X, Y> val) {
        this.getData().add(val);
    }

}
