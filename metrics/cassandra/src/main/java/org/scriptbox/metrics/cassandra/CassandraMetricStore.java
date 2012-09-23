package org.scriptbox.metrics.cassandra;

import java.util.ArrayList;
import java.util.List;

import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.cassandra.serializers.ObjectSerializer;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.cassandra.service.template.ColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.SuperCfResult;
import me.prettyprint.cassandra.service.template.SuperCfRowMapper;
import me.prettyprint.cassandra.service.template.SuperCfTemplate;
import me.prettyprint.cassandra.service.template.ThriftColumnFamilyTemplate;
import me.prettyprint.cassandra.service.template.ThriftSuperCfTemplate;
import me.prettyprint.hector.api.Keyspace;

import org.scriptbox.metrics.model.MetricResolution;
import org.scriptbox.metrics.model.MetricStore;
import org.scriptbox.metrics.model.MetricTree;

public class CassandraMetricStore implements MetricStore {

	static final String METRIC_TREE_CF = "MetricTree";
	static final String METRIC_SEQUENCE_CF = "MetricSequence";
	static final String ALL_NAMES_KEY = "AllMetrics";
	
	Keyspace keyspace;
	SuperCfTemplate<String,String,String> metricTreeTemplate;
	ColumnFamilyTemplate<String,Long> metricSequenceTemplate;
	
	public CassandraMetricStore() {
	}
	
	public Keyspace getKeyspace() {
		return keyspace;
	}

	public void setKeyspace(Keyspace keyspace) {
		this.keyspace = keyspace;
	}

	public void initialize() {
		StringSerializer ser = StringSerializer.get();
		metricTreeTemplate = new ThriftSuperCfTemplate<String,String,String>( keyspace, METRIC_TREE_CF, ser, ser, ser );
		metricSequenceTemplate = new ThriftColumnFamilyTemplate<String,Long>( keyspace, METRIC_SEQUENCE_CF, ser, LongSerializer.get() );
	}

	public void begin() {
		CassandraMetricRange.begin();
	}
	
	public void end() {
		CassandraMetricRange.end();
	}

	public MetricTree createMetricTree( String name, List<MetricResolution> resolutions ) {
		CassandraMetricTree tree = new CassandraMetricTree( this, name, resolutions );
		tree.persist();
		return tree;
	}
	
	public List<MetricTree> getAllMetricTrees() {
		return metricTreeTemplate.querySuperColumns(ALL_NAMES_KEY, null, new SuperCfRowMapper<String, String, String, List<MetricTree>>() {
			 public List<MetricTree> mapRow(SuperCfResult<String, String, String> result) {
				List<MetricTree> trees = new ArrayList<MetricTree>();
				if( result.hasResults() )  {
					for( String treeName : result.getColumnNames() ) {
						List<MetricResolution> resolutions = (List<MetricResolution>)ObjectSerializer.get().fromByteBuffer(result.getByteBuffer(treeName, "resolutions"));
						trees.add( new CassandraMetricTree( CassandraMetricStore.this, treeName, resolutions) );
					}
				}
				return trees;
			 }
		} );
		
	}
}