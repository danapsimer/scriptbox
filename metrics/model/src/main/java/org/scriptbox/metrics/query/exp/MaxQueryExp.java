package org.scriptbox.metrics.query.exp;

import java.util.Iterator;
import java.util.Map;

import org.scriptbox.metrics.compute.MetricCollator;
import org.scriptbox.metrics.model.Metric;
import org.scriptbox.metrics.model.MetricRange;
import org.scriptbox.metrics.query.main.MetricProvider;
import org.scriptbox.metrics.query.main.MetricQueries;
import org.scriptbox.metrics.query.main.MetricQueryContext;
import org.scriptbox.util.common.obj.ParameterizedRunnableWithResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaxQueryExp implements MetricQueryExp {

	private static final Logger LOGGER = LoggerFactory.getLogger(MaxQueryExp.class);

	private String name;
	private MetricQueryExp child;

	public MaxQueryExp(MetricQueryExp child) {
		this( null, child );
	}
	public MaxQueryExp( String name, MetricQueryExp child) {
		this.name = name;
		this.child = child;
	}

	public Object evaluate(final MetricQueryContext ctx) throws Exception {
	    Map<? extends MetricProvider,? extends MetricRange> metrics = MetricQueries.providers(ctx, child);
	    String label = toString();
		MetricCollator collator = new MetricCollator(label, label, ctx.getResolution(),metrics.values());
		return collator.collate(false, new ParameterizedRunnableWithResult<Metric, MetricRange>() {
			public Metric run(MetricRange range) {
				Iterator<Metric> iter = range.getIterator(ctx.getResolution());
				if( iter.hasNext() ) {
					float max = Float.MIN_VALUE;
					while (iter.hasNext()) {
						Metric metric = iter.next();
						max = Math.max(metric.getValue(), max);
					}
					return new Metric(range.getStart(), max);
				}
				return null;
			}
		});
	}

	public String toString() {
		return name != null ? name :  "max(" + child + ")";
	}
}
