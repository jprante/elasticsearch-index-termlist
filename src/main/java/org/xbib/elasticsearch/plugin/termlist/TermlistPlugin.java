package org.xbib.elasticsearch.plugin.termlist;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.xbib.elasticsearch.plugin.termlist.action.TermlistAction;
import org.xbib.elasticsearch.plugin.termlist.action.TransportTermlistAction;
import org.xbib.elasticsearch.plugin.termlist.rest.RestTermlistAction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class TermlistPlugin extends Plugin implements ActionPlugin {

    private final Settings settings;

    public TermlistPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> extra = new ArrayList<>();
        if (settings.getAsBoolean("plugins.xbib.termlist.enabled", true)) {
            extra.add(new ActionHandler<>(TermlistAction.INSTANCE, TransportTermlistAction.class));
        }
        return extra;
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings,
                                             RestController restController,
                                             ClusterSettings clusterSettings,
                                             IndexScopedSettings indexScopedSettings,
                                             SettingsFilter settingsFilter,
                                             IndexNameExpressionResolver indexNameExpressionResolver,
                                             Supplier<DiscoveryNodes> nodesInCluster) {
        List<RestHandler> extra = new ArrayList<>();
        if (settings.getAsBoolean("plugins.xbib.termlist.enabled", true)) {
            extra.add(new RestTermlistAction(settings, restController));
        }
        return extra;
    }
}
