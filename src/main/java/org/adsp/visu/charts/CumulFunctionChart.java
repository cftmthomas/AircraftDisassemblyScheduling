package org.adsp.visu.charts;

import javafx.collections.ObservableList;
import javafx.scene.chart.*;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.Duration;

import java.util.HashMap;

/**
 * chart for drawing a cumulative function such as a maximum load profile.
 */
public class CumulFunctionChart extends StackedAreaChart<Number,Number> {
    private static final double ZOOM_FACTOR = 1.2;
    private boolean ctrlPressed = false;
    private boolean altPressed = false;

    private String selectedKey;
    private HashMap<String, Series<Number, Number>> categoryCurves; // set of {description, Series} for the curve

    public CumulFunctionChart(Axis<Number> xAxis, Axis<Number> yAxis, double maxValue, double minValue) {
        super(xAxis, yAxis);
        this.categoryCurves = new HashMap<>();
        setMaxValue(maxValue);
        setMinValue(minValue);
        setupZoom();
    }

    public CumulFunctionChart(Axis<Number> xAxis, Axis<Number> yAxis, double maxValue) {
        super(xAxis, yAxis);
        this.categoryCurves = new HashMap<>();
        setMaxValue(maxValue);
        setupZoom();
    }

    public CumulFunctionChart() {
        super(new NumberAxis(), new NumberAxis());
        this.categoryCurves = new HashMap<>();
        setupZoom();
    }

    public CumulFunctionChart(double maxValue) {
        super(new NumberAxis(), new NumberAxis("Value", 0, maxValue, 1));
        this.categoryCurves = new HashMap<>();
        setupZoom();
    }

    public CumulFunctionChart(double maxValue, double minValue) {
        super(new NumberAxis(), new NumberAxis("Value", minValue, maxValue, 1));
        this.categoryCurves = new HashMap<>();
        setupZoom();
    }

    private void setupZoom(){
        // Set focus and make chart focus traversable
        setFocusTraversable(true);
        requestFocus();

        // Add key event handlers for Ctrl key press/release
        setOnKeyPressed(this::handleKeyPress);
        setOnKeyReleased(this::handleKeyRelease);

        // Add scroll event handler for zooming
        setOnScroll(this::handleScroll);
    }

    /**
     * set the current item for which to add cumul values
     * @param description description for which to add cumul values
     */
    public void setCategory(String description) {
        selectedKey = description;
        // create new Series for it if they were absent
        createSeriesIfNotExist(description);
    }

    private void createSeriesIfNotExist(String name) {
        if (!categoryCurves.containsKey(name)) {
            Series series = new Series();
            series.setName(name);
            categoryCurves.put(name, series);
            this.getData().add(series);
        }
    }

    /**
     * remove the current category from the graph
     * @param category category to remove from the graph
     */
    public void removeCategory(int category) {
        this.getData().remove(categoryCurves.get(category));
        categoryCurves.remove(category);
    }

    /**
     * clear the values associated to the current category
     * @param description description whose values will be removed
     */
    public void clearCategory(String description) {
        categoryCurves.get(description).getData().clear();
    }

    /**
     * clear the values but keeps the categories
     */
    public void clearData() {
        for(Series<Number, Number> series: categoryCurves.values()) series.getData().clear();
    }

    /**
     * add a new cumul value for the current capacity graph
     * the nodes processed must be called in order of processing
     * @param node new node that is processed
     * @param cumulValue cumulated value at the node
     */
    public void addCumulValue(int node, double cumulValue) {
        ObservableList data = categoryCurves.get(selectedKey).getData();
        if (data.size() > 0) { // a node before existed, set the cumul value for transition
            double predCumul = ((Number) ((Data) data.get(data.size() - 1)).getYValue()).doubleValue();
            data.add(new Data(node, predCumul));
        }
        data.add(new Data(node, cumulValue));
    }

    /**
     * add all nodes and cumul values to the given category
     * @param description description whose cumul value will be set
     * @param nodes nodes processed in the category, in order
     * @param cumulValues cumulated values at each node
     */
    public void addCumulValue(String description, int[] nodes, double... cumulValues) {
        assert (nodes.length == cumulValues.length);
        createSeriesIfNotExist(description);
        ObservableList data = categoryCurves.get(description).getData();
        if (data.size() > 0) {
            double predCumul = ((Number) ((Data) data.get(data.size() - 1)).getYValue()).doubleValue();
            data.add(new Data(nodes[0], predCumul));
        }
        data.add(new Data(nodes[0], cumulValues[0]));
        for (int i = 1; i < nodes.length ; ++i) {
            data.add(new Data(nodes[i], cumulValues[i-1]));
            data.add(new Data(nodes[i], cumulValues[i]));
        }
    }

    /**
     * add all nodes and cumul values to the current category
     * @param nodes nodes processed in the category, in order
     * @param cumulValues cumulated values at each node
     */
    public void addCumulValue(int[] nodes, double... cumulValues) {
        addCumulValue(selectedKey, nodes, cumulValues);
    }

    /**
     * add the node and its value to the cumul function of the current category
     * @param node last node currently visited by the category
     * @param value value of node (not cumulated)
     */
    public void addStep(int node, double value, String label) {
        ObservableList data = categoryCurves.get(selectedKey).getData();
        double predCumul = 0.;
        if (data.size() > 0) {
            predCumul = ((Number) ((Data) data.get(data.size() - 1)).getYValue()).doubleValue();
            data.add(new Data(node, predCumul));
            if(!label.isBlank()){
                Tooltip tooltip = new Tooltip(label);
                tooltip.setShowDelay(new Duration(0));
                Tooltip.install(((Data) data.get(data.size()-1)).getNode(), tooltip);
            }
        }
        data.add(new Data(node, predCumul + value));
        if(!label.isBlank()){
            Tooltip tooltip = new Tooltip(label);
            tooltip.setShowDelay(new Duration(0));
            Tooltip.install(((Data) data.get(data.size()-1)).getNode(), tooltip);
        }
    }

    public void addStep(int node, double value) {
        addStep(node, value, "");
    }

    /**
     * add the nodes and their values to the cumul function of the current description
     * @param description description whose cumul value will be set
     * @param nodes last node currently processed in the description
     * @param values value of the nodes (not cumulated)
     * @param labels labels to display on node hovering
     */
    public void addSteps(String description, int[] nodes, double[] values, String[] labels) {
        assert (nodes.length == values.length);
        createSeriesIfNotExist(description);
        ObservableList data = categoryCurves.get(description).getData();
        double predCumul = 0.;
        if (data.size() > 0) {
            predCumul = ((Number) ((Data) data.get(data.size() - 1)).getYValue()).doubleValue();
            data.add(new Data(nodes[0], predCumul));
            if(!labels[0].isBlank()){
                Tooltip tooltip = new Tooltip(labels[0]);
                tooltip.setShowDelay(new Duration(0));
                Tooltip.install(((Data) data.get(data.size()-1)).getNode(), tooltip);
            }
        }
        data.add(new Data(nodes[0], predCumul + values[0]));
        if(!labels[0].isBlank()){
            Tooltip tooltip = new Tooltip(labels[0]);
            tooltip.setShowDelay(new Duration(0));
            Tooltip.install(((Data) data.get(data.size()-1)).getNode(), tooltip);
        }
        predCumul += values[0];
        for (int i = 1; i < nodes.length ; ++i) {
            data.add(new Data(nodes[i], predCumul));
            if(!labels[i].isBlank()){
                Tooltip tooltip = new Tooltip(labels[i]);
                tooltip.setShowDelay(new Duration(0));
                Tooltip.install(((Data) data.get(data.size() - 1)).getNode(), tooltip);
            }
            data.add(new Data(nodes[i], predCumul + values[i]));
            if(!labels[i].isBlank()){
                Tooltip tooltip = new Tooltip(labels[i]);
                tooltip.setShowDelay(new Duration(0));
                Tooltip.install(((Data) data.get(data.size() - 1)).getNode(), tooltip);
            }
            predCumul += values[i];
        }
    }

    public void addSteps(String description, int[] nodes, double[] values) {
        addSteps(description, nodes, values, new String[nodes.length]);
    }

    /**
     * add the nodes and their values to the cumul function of the current category
     * @param nodes last nodes currently processed in the category
     * @param values values of the nodes (not cumulated)
     */
    public void addSteps(int[] nodes, double[] values, String[] labels) {
        addSteps(selectedKey, nodes, values, labels);
    }

    public void addSteps(int[] nodes, double[] values) {
        addSteps(selectedKey, nodes, values);
    }

    /**
     * set the value for the max value, updating the maximum y-axis value
     * @param maxValue maximum value for the cumulative graph
     */
    public void setMaxValue(double maxValue) {
        ((ValueAxis<Number>) getYAxis()).setUpperBound(maxValue);
    }

    /**
     * set the value for the min value, updating the minimum y-axis value
     * @param minValue minimum value for the cumulative graph
     */
    public void setMinValue(double minValue) {
        ((ValueAxis<Number>) getYAxis()).setLowerBound(minValue);
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.isControlDown()) {
            ctrlPressed = true;
        }
        else if (event.isAltDown()) {
            altPressed = true;
        }
    }

    private void handleKeyRelease(KeyEvent event) {
        if (!event.isControlDown()) {
            ctrlPressed = false;
        }
        if (!event.isAltDown()) {
            altPressed = false;
        }
    }

    private void handleScroll(ScrollEvent event) {
        if (ctrlPressed) {
            double deltaY = event.getDeltaY();
            if (deltaY == 0) {
                return;
            }

            StackedAreaChart<Number, Number> chart = (StackedAreaChart<Number, Number>) event.getSource();
            NumberAxis xAxis = (NumberAxis) chart.getXAxis();
            xAxis.setAutoRanging(false);

            double scaleFactor = (deltaY > 0) ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;

            // Adjust the axis ranges based on the scroll direction
            xAxis.setLowerBound(xAxis.getLowerBound() * 1/scaleFactor);
            xAxis.setUpperBound(xAxis.getUpperBound() * 1/scaleFactor);

            event.consume();
        } else if (altPressed) {
            double deltaY = event.getDeltaY();
            if (deltaY == 0) {
                return;
            }

            StackedAreaChart<Number, Number> chart = (StackedAreaChart<Number, Number>) event.getSource();
            NumberAxis yAxis = (NumberAxis) chart.getYAxis();
            yAxis.setAutoRanging(false);

            double scaleFactor = (deltaY > 0) ? ZOOM_FACTOR : 1 / ZOOM_FACTOR;

            // Adjust the axis ranges based on the scroll direction
            yAxis.setLowerBound(yAxis.getLowerBound() * scaleFactor);
            yAxis.setUpperBound(yAxis.getUpperBound() * scaleFactor);

            event.consume();
        }
    }
}
