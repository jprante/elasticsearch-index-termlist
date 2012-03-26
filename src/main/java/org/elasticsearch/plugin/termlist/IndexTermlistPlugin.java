package org.elasticsearch.plugin.termlist;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.termlist.TermlistAction;
import org.elasticsearch.action.termlist.TransportTermlistAction;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.termlist.RestTermlistAction;

public class IndexTermlistPlugin extends AbstractPlugin {

    @Override 
    public String name() {
        return "index-termlist";
    }

    @Override 
    public String description() {
        return "Index termlists for Elasticsearch";
    }
    
    public void onModule(RestModule module) {
        module.addRestAction(RestTermlistAction.class);
    }

    public void onModule(ActionModule module) {
        module.registerAction(TermlistAction.INSTANCE, TransportTermlistAction.class);        
    }
    
}
