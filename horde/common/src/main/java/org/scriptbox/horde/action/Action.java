package org.scriptbox.horde.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scriptbox.horde.metrics.ActionAware;
import org.scriptbox.horde.metrics.ActionMetric;
import org.scriptbox.horde.metrics.AvgTransactionTime;
import org.scriptbox.horde.metrics.FailureCount;
import org.scriptbox.horde.metrics.MaxTransactionTime;
import org.scriptbox.horde.metrics.MinTransactionTime;
import org.scriptbox.horde.metrics.TransactionCount;
import org.scriptbox.horde.metrics.TransactionsPerSecond;
import org.scriptbox.horde.metrics.distro.DistroCountMetric;
import org.scriptbox.horde.metrics.distro.DistroPercentMetric;
import org.scriptbox.horde.metrics.mbean.AbstractDynamicExposableMBean;
import org.scriptbox.horde.metrics.mbean.ActionDynamicMetricMBean;
import org.scriptbox.horde.metrics.mbean.Exposable;
import org.scriptbox.horde.metrics.probe.Probe;
import org.scriptbox.util.common.obj.ParameterizedRunnableWithResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Action {

	private static final Logger LOGGER = LoggerFactory.getLogger( Action.class );
	
    private static final long[] DISTROS = new long[] { 100, 250, 500, 750, 1000, 2000, 4000, 8000, 16000, 32000, -1 };
    
	private static ThreadLocal<Map<String,Object>> attributes = new ThreadLocal<Map<String,Object>>() {
        public Map<String,Object> initialValue() {
            return new HashMap<String,Object>();
        }
    };

    private String name;
	private ActionScript script;
    private ParameterizedRunnableWithResult<Integer,List> iterations;
    private ParameterizedRunnableWithResult<Boolean,List> init;
    private ParameterizedRunnableWithResult<Boolean,List> run;
    private ParameterizedRunnableWithResult<Boolean,List> pre;
    private ParameterizedRunnableWithResult<Boolean,List> post;
    
    private Set<ActionMetric> preMetrics = new HashSet<ActionMetric>();
    private Set<ActionMetric> metrics = new HashSet<ActionMetric>();
    private Set<ActionMetric> postMetrics = new HashSet<ActionMetric>();

    private AbstractDynamicExposableMBean mbean;
    private Map<String,AbstractDynamicExposableMBean> probes = new HashMap<String,AbstractDynamicExposableMBean>();
    private List<AbstractDynamicExposableMBean> mbeans = new ArrayList<AbstractDynamicExposableMBean>();
    
    public Action( ActionScript script, String name ) {
        this.script = script;
        this.name = name;
        this.mbean = new ActionDynamicMetricMBean( this );
        mbeans.add( this.mbean );
    }
    
    public ActionScript getActionScript() {
    	return script;
    }
    public String getName() {
    	return name;
    }
    
    public ParameterizedRunnableWithResult<Integer,List>  getIterations() {
		return iterations;
	}

	public void setIterations(ParameterizedRunnableWithResult<Integer,List>  iterations) {
		this.iterations = iterations;
	}

    public ParameterizedRunnableWithResult<Boolean,List>  getInit() {
		return init;
	}

	public void setInit(ParameterizedRunnableWithResult<Boolean,List>  init) {
		this.init = init;
	}

	public ParameterizedRunnableWithResult<Boolean,List>  getRun() {
		return run;
	}

	public void setRun(ParameterizedRunnableWithResult<Boolean,List>  run) {
		this.run = run;
	}

	public ParameterizedRunnableWithResult<Boolean,List>  getPre() {
		return pre;
	}

	public void setPre(ParameterizedRunnableWithResult<Boolean,List>  pre) {
		this.pre = pre;
	}

	public ParameterizedRunnableWithResult<Boolean,List>  getPost() {
		return post;
	}

	public void setPost(ParameterizedRunnableWithResult<Boolean,List>  post) {
		this.post = post;
	}

	public void initialize() throws Exception {
    	
        if( run == null ) {  
            throw new RuntimeException( "No run block defined for action");
        }
        if( init != null ) {
        	init.run( script.getBoxScript().getArguments() );
        }
        
        if( pre != null ) {
            addPreMetric( new AvgTransactionTime("pre") );
        }
        
        addRunMetric( new TransactionsPerSecond() );
        addRunMetric( new TransactionCount() );
        addRunMetric( new MinTransactionTime() );
        addRunMetric( new MaxTransactionTime() );
        addRunMetric( new AvgTransactionTime() );
        addRunMetric( new FailureCount() );
       
        addRunDistroCountMetrics();
        addRunDistroPercentMetrics();
        
        if( post != null ) {
            addPostMetric( new AvgTransactionTime("post") );
        }
    }
	
    void addRunProbe( Probe probe ) {
    	if( probes.get(probe.getName()) != null ) {
    		throw new RuntimeException( "Already registered a probe: '" + probe.getName() + "'" );
    	}
    	
    	List<Exposable> exposables = probe.getExposables();
    	ActionDynamicMetricMBean bean = new ActionDynamicMetricMBean(this, "probe=" + probe.getName() );
    	for( Exposable exposable : exposables ) {
    		if( exposable instanceof ActionAware ) {
    			((ActionAware)exposable).init( this );
    		}
	        bean.addExposable( exposable );
    	}
    	probes.put( probe.getName(), bean );
    	mbeans.add( bean );
    }
   
    void addRunDistroCountMetrics() {
    	ActionDynamicMetricMBean distroCount = new ActionDynamicMetricMBean( this, "distro=count" );
        long prev = 0;
        for( long dist : DISTROS ) {
        	addRunDistroMetric( distroCount, new DistroCountMetric("distro", prev, dist ) );
        	prev = dist + 1;
        }
    	mbeans.add( distroCount );
    }
    
    void addRunDistroPercentMetrics() {
    	ActionDynamicMetricMBean distroPercent = new ActionDynamicMetricMBean( this, "distro=percent" );
        long prev = 0;
        for( long dist : DISTROS ) {
        	addRunDistroMetric( distroPercent, new DistroPercentMetric("distro", prev, dist ) );
        	prev = dist + 1;
        }
        mbeans.add( distroPercent );
    }
    
    private void addRunDistroMetric( AbstractDynamicExposableMBean bean, ActionMetric metric ) {
        metric.init( this );
        bean.addExposable( metric );
        metrics.add( metric );
    }
    
    void addRunMetric( ActionMetric metric ) {
        addActionMetric( metrics, metric );
    }
    void addPreMetric( ActionMetric metric ) {
        addActionMetric( preMetrics, metric );
    }
    void addPostMetric( ActionMetric metric ) {
        addActionMetric( postMetrics, metric );
    }
    private void addActionMetric( Collection<ActionMetric> mets, ActionMetric metric ) {
        metric.init( this );
        mbean.addExposable( metric );
        mets.add( metric );
    }
   
    public Object getTestAttribute( String name ) {
       return getVariable( name );
    } 
    public void setTestAttribute( String name, Object value ) {
    	setVariable( name, value );
    }
    
    public Object getVariable( String name ) {
       return attributes.get().get( name ); 
    } 
    public void setVariable( String name, Object value ) {
        attributes.get().put( name, value );
    }

    public void callAllAndCollectMetrics() throws Throwable {
       int count = 1;
       if( iterations != null ) {
    	   List<String> arguments = script.getBoxScript().getArguments();
    	   count = iterations.run( arguments );
       }
       for( int i=0 ; i < count ; i++ ) { 
	       callAndCollectMetrics( preMetrics, pre );
	       callAndCollectMetrics( metrics, run );
	       callAndCollectMetrics( postMetrics, post );
       }
    }
    
    public void callAndCollectMetrics( Collection<ActionMetric> met, ParameterizedRunnableWithResult<Boolean,List> closure ) 
    	throws Throwable
    {
        List<String> arguments = script.getBoxScript().getArguments();
        if( closure != null ) {
            long before = System.currentTimeMillis(); 
            try {
               boolean ret = closure.run( arguments );
               ActionMetric.collectAll( met, ret, System.currentTimeMillis() - before );
            }
            catch( VirtualMachineError ex ) {
                throw ex;
            }
            catch( Throwable ex ) {
               ActionMetric.collectAll( met, false, System.currentTimeMillis() - before );
               script.callGlobalErrorHandlers( ex );
            }
        }
    }
    
    void collectMetrics( boolean success, long millis ) {
        ActionMetric.collectAll( metrics, success, millis );
    }
    public void registerMBeans() throws Exception {
    	for( AbstractDynamicExposableMBean bean : mbeans ) {
    		bean.register();
    	}
    }    
    
    public void unregisterMBeans() throws Exception {
    	for( AbstractDynamicExposableMBean bean : mbeans ) {
    		bean.unregister();
    	}
    }
    
    public String toString() {
    	return "Action{ name=" + name + " }";
    }
}
