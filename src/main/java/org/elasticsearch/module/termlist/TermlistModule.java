package org.elasticsearch.module.termlist;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.action.termlist.TermlistAction;
import org.elasticsearch.action.termlist.TransportTermlistAction;

public class TermlistModule extends ActionModule {

    public TermlistModule() {
        super(true);
    }
    
    @Override
    protected void configure() {
        registerAction(TermlistAction.INSTANCE, TransportTermlistAction.class);
    }
}
