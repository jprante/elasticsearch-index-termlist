package org.elasticsearch.plugin.termlist;

import java.util.Collection;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.module.termlist.TermlistModule;
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

    @Override 
    public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(TermlistModule.class);
        return modules;
    }    
}
