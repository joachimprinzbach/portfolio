package name.abuchen.portfolio.ui.views.dashboard.heatmap;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.util.Colors;
import name.abuchen.portfolio.ui.util.InfoToolTip;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.DashboardResources;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.ui.views.dashboard.WidgetDelegate;

public abstract class AbstractHeatmapWidget<N extends Number> extends WidgetDelegate<HeatmapModel<N>>
{
    private Composite table;
    private Label title;
    private DashboardResources resources;

    public AbstractHeatmapWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ReportingPeriodConfig(this));
    }

    @Override
    public Composite createControl(Composite parent, DashboardResources resources)
    {
        this.resources = resources;

        Composite container = new Composite(parent, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).margins(5, 5).applyTo(container);
        container.setBackground(parent.getBackground());

        title = new Label(container, SWT.NONE);
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$
        GridDataFactory.fillDefaults().grab(true, false).applyTo(title);

        table = new Composite(container, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(table);
        table.setBackground(container.getBackground());

        return container;
    }

    protected abstract HeatmapModel<N> build();

    private void fillTable(HeatmapModel<N> model, Composite table, DashboardResources resources)
    {
        addHeaderRow(table, model);

        DoubleFunction<Color> coloring = optionallyGet(ColorSchemaConfig.class)
                        .map(s -> s.getValue().buildColorFunction(resources.getResourceManager())).orElse(null);

        Values<N> formatter = model.getFormatter();

        model.getRows().forEach(row -> {

            Label label = new Label(table, SWT.CENTER);
            label.setText(row.getLabel());

            row.getData().forEach(data -> {
                CLabel dataLabel = new CLabel(table, SWT.CENTER);

                if (data != null)
                {
                    dataLabel.setText(formatter.format(data));
                    if (coloring != null)
                        dataLabel.setBackground(coloring.apply((double) data));
                    dataLabel.setFont(resources.getSmallFont());
                }

                if (model.getCellToolTip() != null)
                    InfoToolTip.attach(dataLabel, model.getCellToolTip().apply(data));
            });
        });

        SimpleGridLayout layout = new SimpleGridLayout();
        layout.setNumColumns(model.getHeaderSize() + 1);
        layout.setNumRows((int) model.getRows().count() + 1);
        layout.setRowHeight(table.getFont().getFontData()[0].getHeight() + 8);

        table.setLayout(layout);
        table.layout(true);
    }

    private void addHeaderRow(Composite table, HeatmapModel<?> model)
    {
        // Top Left is empty
        new Label(table, SWT.NONE);

        model.getHeader().forEach(label -> {
            CLabel l = new CLabel(table, SWT.CENTER);
            l.setText(label);
            l.setBackground(Colors.WHITE);

            InfoToolTip.attach(l, label);
        });
    }

    @Override
    public Supplier<HeatmapModel<N>> getUpdateTask()
    {
        return this::build;
    }

    @Override
    public void update(HeatmapModel<N> model)
    {
        title.setText(getWidget().getLabel() != null ? getWidget().getLabel() : ""); //$NON-NLS-1$

        for (Control child : table.getChildren())
            child.dispose();

        fillTable(model, table, resources);

        table.getParent().getParent().layout(true);
    }

    @Override
    public Control getTitleControl()
    {
        return title;
    }

    protected void addMonthlyHeader(HeatmapModel<?> model, int numDashboardColumns, boolean showSum)
    {
        TextStyle textStyle;
        if (numDashboardColumns == 1)
            textStyle = TextStyle.FULL;
        else if (numDashboardColumns == 2)
            textStyle = TextStyle.SHORT;
        else
            textStyle = TextStyle.NARROW;

        // no harm in hardcoding the year as each year has the same months
        for (LocalDate m = LocalDate.of(2016, 1, 1); m.getYear() == 2016; m = m.plusMonths(1))
            model.addHeader(m.getMonth().getDisplayName(textStyle, Locale.getDefault()));
        if (showSum)
            model.addHeader("\u03A3"); //$NON-NLS-1$
    }

    protected Double geometricMean(List<Double> values)
    {
        if (values.isEmpty())
            return null;

        if (values.size() == 1)
            return values.get(0);

        double sum = 1;
        for (Double v : values)
            sum *= (1 + v);

        return Math.pow(sum, 1 / (double) values.size()) - 1;
    }
}
