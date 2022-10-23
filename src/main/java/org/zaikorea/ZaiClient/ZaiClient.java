package org.zaikorea.ZaiClient;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.EmptyBatchException;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;
import org.zaikorea.ZaiClient.request.Event;
import org.zaikorea.ZaiClient.request.EventBatch;
import org.zaikorea.ZaiClient.request.RecommendationRequest;
import org.zaikorea.ZaiClient.response.EventLoggerResponse;
import org.zaikorea.ZaiClient.response.RecommendationResponse;
import org.zaikorea.ZaiClient.security.ZaiHeaders;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ZaiClient {

    private final String zaiClientId;
    private final String zaiSecret;
    private final ZaiAPI zaiAPI;
    private static final int defaultConnectTimeout = 10;
    private static final int defaultReadTimeout = 30;
    private int connectTimeout;
    private int readTimeout;
    private String eventsApiEndpoint;
    private String mlApiEndpoint;

    public ZaiClient(Builder builder) {
        this.zaiClientId = builder.zaiClientId;
        this.zaiSecret = builder.zaiSecret;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.eventsApiEndpoint = builder.eventsApiEndpoint;
        this.mlApiEndpoint = builder.mlApiEndpoint;
        this.zaiAPI = this.instantiateZaiAPI();
    }

    public EventLoggerResponse addEventLog(Event event) throws IOException, ZaiClientException {
        Call<EventLoggerResponse> call = zaiAPI.addEventLog(event);

        Response<EventLoggerResponse> response = call.execute();

        if (!response.isSuccessful())
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

        return response.body();
    }

    public EventLoggerResponse addEventLog(EventBatch eventBatch) throws IOException, ZaiClientException, EmptyBatchException {
        List<Event> events = eventBatch.getEventList();

        if (events.size() == 0) throw new EmptyBatchException();

        Call<EventLoggerResponse> call = zaiAPI.addEventLog(events);
        Response<EventLoggerResponse> response = call.execute();

        if (!response.isSuccessful())
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

        eventBatch.setLogFlag();

        return response.body();
    }

    public EventLoggerResponse updateEventLog(Event event) throws IOException, ZaiClientException {
        Call<EventLoggerResponse> call = zaiAPI.updateEventLog(event);
        Response<EventLoggerResponse> response = call.execute();

        if (!response.isSuccessful())
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

        return response.body();
    }

    public EventLoggerResponse deleteEventLog(Event event) throws IOException, ZaiClientException {
        Call<EventLoggerResponse> call = zaiAPI.deleteEventLog(event);
        Response<EventLoggerResponse> response = call.execute();

        if (!response.isSuccessful())
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

        return response.body();
    }

    public EventLoggerResponse deleteEventLog(EventBatch eventBatch) throws IOException, ZaiClientException, EmptyBatchException {
        List<Event> events = eventBatch.getEventList();

        if (events.size() == 0) throw new EmptyBatchException();

        Call<EventLoggerResponse> call = zaiAPI.deleteEventLog(events);
        Response<EventLoggerResponse> response = call.execute();

        if (!response.isSuccessful())
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

        return response.body();
    }

    public RecommendationResponse getRecommendations(RecommendationRequest recommendation) throws IOException, ZaiClientException {
        Call<RecommendationResponse> call = zaiAPI.getRecommendations(
                mlApiEndpoint + recommendation.getPath(this.zaiClientId), recommendation
        );
        Response<RecommendationResponse> response = call.execute();

        if (!response.isSuccessful())
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

        return response.body();
    }

    private ZaiAPI instantiateZaiAPI() {
        String zaiClientId = this.zaiClientId;
        String zaiSecret = this.zaiSecret;

        OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(this.readTimeout, TimeUnit.SECONDS)
            .connectTimeout(this.connectTimeout, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                Request request = chain.request();
                String path = request.url().encodedPath();
                Map<String, String> zaiHeaders;
                try {
                    zaiHeaders = ZaiHeaders.generateZaiHeaders(zaiClientId, zaiSecret, path);
                } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                    throw new RuntimeException(e);
                }
                Request.Builder builder = request.newBuilder();
                for (Map.Entry<String, String> zaiHeader : zaiHeaders.entrySet())
                    builder = builder.addHeader(zaiHeader.getKey(), zaiHeader.getValue());
                return chain.proceed(builder.build());
            })
            .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(eventsApiEndpoint)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        return retrofit.create(ZaiAPI.class);
    }

    protected String getExceptionMessage(Response<?> response) {
        String error = null;
        try {
            assert response.errorBody() != null;
            JsonElement element = JsonParser.parseString(response.errorBody().toString());
            error = element.toString();
        } catch (Exception | AssertionError e) {
            e.printStackTrace();
        }

        String responseMessage = response.message();
        if (error == null)
            error = (responseMessage != null) ? responseMessage : "Internal server error.";

        return error;
    }

    public static class Builder {

        private final String zaiClientId;
        private final String zaiSecret;
        private int connectTimeout;
        private int readTimeout;
        private String eventsApiEndpoint;
        private String mlApiEndpoint;

        public Builder(String zaiClientId, String zaiSecret) {
            this.zaiClientId = zaiClientId;
            this.zaiSecret = zaiSecret;
            this.connectTimeout = defaultConnectTimeout;
            this.readTimeout = defaultReadTimeout;
            this.eventsApiEndpoint = String.format(Config.eventsApiEndPoint, "");
            this.mlApiEndpoint = String.format(Config.mlApiEndPoint, "");
        }

        public Builder connectTimeout(int seconds) {
            if (seconds > 0) {
                this.connectTimeout = seconds;
            }
            return this;
        }

        public Builder readTimeout(int seconds) {
            if (seconds > 0) {
                this.readTimeout = seconds;
            }
            return this;
        }

        public Builder customEndpoint(String endpoint) throws InvalidParameterException {
            if (endpoint.length() > 10)
                throw new InvalidParameterException("Custom endpoint should be less than or equal to 10.");

            if (Pattern.matches("^[a-zA-Z0-9-]$", endpoint)) {
                this.eventsApiEndpoint = String.format(Config.eventsApiEndPoint, "-"+endpoint);
                this.mlApiEndpoint = String.format(Config.mlApiEndPoint, "-"+endpoint);
            }
            else
                throw new InvalidParameterException("Only alphanumeric characters are allowed for custom endpoint.");

            return this;
        }

        public ZaiClient build() {
            return new ZaiClient(this);
        }

    }

}
