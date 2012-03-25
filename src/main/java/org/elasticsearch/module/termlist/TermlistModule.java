package org.elasticsearch.module.termlist;

import org.elasticsearch.action.GenericAction;
import org.elasticsearch.action.termlist.TermlistAction;
import org.elasticsearch.action.termlist.TransportTermlistAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

public class TermlistModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TransportTermlistAction.class).asEagerSingleton();
        MapBinder<GenericAction, TransportAction> transportActionsBinder =
                MapBinder.newMapBinder(binder(), GenericAction.class, TransportAction.class);
        transportActionsBinder.addBinding(TermlistAction.INSTANCE).to(TransportTermlistAction.class).asEagerSingleton();
    }
}
