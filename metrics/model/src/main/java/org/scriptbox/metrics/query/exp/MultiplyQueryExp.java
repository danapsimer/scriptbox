package org.scriptbox.metrics.query.exp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scriptbox.metrics.compute.ListBackedMetricRange;
import org.scriptbox.metrics.model.Metric;
import org.scriptbox.metrics.model.MetricRange;
import org.scriptbox.metrics.query.main.MetricProvider;
import org.scriptbox.metrics.query.main.MetricQueries;
import org.scriptbox.metrics.query.main.MetricQueryContext;

public class MultiplyQueryExp implements MetricQueryExp {

	private String name;
	private MetricQueryExp child;
	private float multiplier;

	public MultiplyQueryExp(float multiplier, MetricQueryExp child) {
		this( null, multiplier, child );
	}
	
	public MultiplyQueryExp(String name, float multiplier, MetricQueryExp child) {
		this.name = name;
		this.child = child;
		this.multiplier = multiplier;
	}

	public Object evaluate(MetricQueryContext ctx) throws Exception {
		Set<MetricProvider> ret = new HashSet<MetricProvider>();
		Map<? extends MetricProvider, ? extends MetricRange> metrics = MetricQueries.providers(ctx, child);
		for (MetricRange range : metrics.values() ) {
			List<Metric> values = new ArrayList<Metric>();
			Iterator<Metric> iter = range.getIterator(ctx.getResolution());
			while (iter.hasNext()) {
				Metric metric = iter.next();
				values.add(new Metric(metric.getMillis(), metric.getValue() * multiplier));
			}
			String label = toString();
			ret.add(new ListBackedMetricRange( label, label, 
				range.getFullDateRange(), range.getStart(), range .getEnd(), values));
		}
		return ret;
	}

	public String toString() {
		return name != null ? name : "multiply(" + multiplier + "," + child + ")";
	}
}
