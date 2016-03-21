package org.xbib.elasticsearch.plugin.termlist;

import org.elasticsearch.action.ActionModule;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndicesModule;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;
import org.xbib.elasticsearch.action.termlist.TermlistAction;
import org.xbib.elasticsearch.action.termlist.TransportTermlistAction;
import org.xbib.elasticsearch.rest.action.termlist.RestTermlistAction;


public class TermlistPlugin extends Plugin {

    private final Settings settings;

    @Inject
    public TermlistPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "termlist";
    }

    @Override
    public String description() {
        return "termlist for Elasticsearch";
    }

    public void onModule(ActionModule module) {
        if (settings.getAsBoolean("plugins.termlist.enabled", true)) {
            module.registerAction(TermlistAction.INSTANCE, TransportTermlistAction.class);
        }
    }

    public void onModule(RestModule module) {
        if (settings.getAsBoolean("plugins.termlist.enabled", true)) {
            module.addRestAction(RestTermlistAction.class);
        }
    }
}
