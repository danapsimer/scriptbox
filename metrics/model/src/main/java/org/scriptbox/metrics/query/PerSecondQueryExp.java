package org.scriptbox.metrics.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scriptbox.metrics.compute.ListBackedMetricRange;
import org.scriptbox.metrics.model.Metric;
import org.scriptbox.metrics.model.MetricRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerSecondQueryExp implements MetricQueryExp {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeltaQueryExp.class);

	private MetricQueryExp child;

	public PerSecondQueryExp(MetricQueryExp child) {
		this.child = child;
	}

	public Object evaluate(MetricQueryContext ctx) throws Exception {
		Set<MetricProvider> ret = new HashSet<MetricProvider>();
		Map<? extends MetricProvider, ? extends MetricRange> metrics = MetricQueries.evaluateProviders(ctx, child);
		for (MetricRange range : metrics.values() ) {
			List<Metric> deltas = new ArrayList<Metric>();
			Metric prev = null;
			Iterator<Metric> iter = range.getIterator(ctx.getChunk());
			while (iter.hasNext()) {
				Metric metric = iter.next();
				if (prev != null) {
					long sec = (metric.getMillis() - prev.getMillis()) / 1000;
					// don't really see this as a possibility but just being
					// anal retentive
					if (sec < 1) {
						sec = 1;
					}
					deltas.add(new Metric(metric.getMillis(), metric.getValue() / sec));
				} 
				else {
					deltas.add(new Metric(metric.getMillis(), 0));
				}
				prev = metric;
			}
			ret.add(new ListBackedMetricRange("persecond(" + range.getDescription() + ")", range.getStart(), range.getEnd(), deltas));
		}
		return ret;
	}

	public String toString() {
		return "persecond(" + child + ")";
	}
}
