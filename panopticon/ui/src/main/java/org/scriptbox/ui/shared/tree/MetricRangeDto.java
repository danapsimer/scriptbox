package org.scriptbox.ui.shared.tree;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.scriptbox.metrics.model.Metric;
import org.scriptbox.ui.shared.timed.TimeBasedLoadResult;

public class MetricRangeDto implements TimeBasedLoadResult<Metric>, Serializable {

	private static final long serialVersionUID = 1L;
	
	private Date first;
	private Date last;
	private Date start;
	private Date end;
	private float min;
	private float max;
	private List<Metric> metrics;
	
	public MetricRangeDto() {
	}
	
	public MetricRangeDto( long first, long last, long start, long end, float min, float max, List<Metric> metrics ) {
		this.first = new Date(first);
		this.last = new Date(last);
		this.start = new Date(start);
		this.end = new Date(end);
		this.min = min;
		this.max = max;
		this.metrics = metrics;
	}

	public float getMin() {
		return min < 0 ? 0 : min;
	}

	public float getMax() {
		return max < 0 ? 0 : max;
	}

	public Date getStart() {
		return start;
	}

	public Date getEnd() {
		return end;
	}

	public Date getFirst() {
		return first;
	}
	
	public Date getLast() {
		return last;
	}
	
	public List<Metric> getData() {
		return metrics;
	}
	
}