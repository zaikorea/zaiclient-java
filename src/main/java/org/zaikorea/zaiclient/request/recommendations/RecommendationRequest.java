package org.zaikorea.zaiclient.request.recommendations;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.request.IRequest;

public class RecommendationRequest implements IRequest<RecommendationQuery> {
    protected String baseUrl = Config.mlApiEndPoint;

    protected RecommendationQuery recQuery;

    public RecommendationRequest() {
        // Do nothing
    }

    public void setRecQuery(RecommendationQuery recQuery) {
        this.recQuery = recQuery;
    }

    @Override
    public RecommendationQuery getPayload() {
        return recQuery;
    }
}
