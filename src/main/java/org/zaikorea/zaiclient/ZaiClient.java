package org.zaikorea.zaiclient;

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
import okhttp3.logging.HttpLoggingInterceptor;

import org.zaikorea.zaiclient.configs.Config;
import org.zaikorea.zaiclient.exceptions.EmptyBatchException;
import org.zaikorea.zaiclient.exceptions.ZaiClientException;
// import org.zaikorea.zaiclient.request.Event;
import org.zaikorea.zaiclient.request.events.Event;
import org.zaikorea.zaiclient.request.EventBatch;
import org.zaikorea.zaiclient.request.events.EventRequest;
// import org.zaikorea.zaiclient.request.RecommendationRequest;
// import org.zaikorea.zaiclient.request.recommendations.GetUserRecommendation;
import org.zaikorea.zaiclient.request.recommendations.RecommendationRequest;
import org.zaikorea.zaiclient.request.items.AddItem;
import org.zaikorea.zaiclient.request.items.DeleteItem;
import org.zaikorea.zaiclient.request.items.UpdateItem;
import org.zaikorea.zaiclient.response.EventLoggerResponse;
import org.zaikorea.zaiclient.response.ItemResponse;
import org.zaikorea.zaiclient.response.RecommendationResponse;
import org.zaikorea.zaiclient.security.ZaiHeaders;
import retrofit2.Call;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ZaiClient {

    private final String zaiClientId;
    private final String zaiSecret;
    private final ZaiAPI zaiAPI;
    private static final int DEFAULT_CONNECT_TIMEOUT = 10;
    private static final int DEFAULT_READ_TIMEOUT = 30;
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

    public ItemResponse sendRequest(AddItem request) throws IOException, ZaiClientException{
        Call<ItemResponse> call = zaiAPI.addItem(request.getPayload());
        request.getPayload().stream().forEach(
            item -> {
                System.out.println(item.getItemId());
            }
        );

        Response<ItemResponse> response = call.execute();

        if (!response.isSuccessful()) {
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));
        }

        return response.body();
    }

    public ItemResponse sendRequest(UpdateItem request) throws IOException, ZaiClientException{
        Call<ItemResponse> call = zaiAPI.updateItem(request.getPayload());

        Response<ItemResponse> response = call.execute();

        if (!response.isSuccessful()) {
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));
        }

        return response.body();
    }

    public ItemResponse sendRequest(DeleteItem request) throws IOException, ZaiClientException{
        Call<ItemResponse> call = zaiAPI.deleteItem(request.getPayload());

        Response<ItemResponse> response = call.execute();

        if (!response.isSuccessful()) {
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));
        }

        return response.body();
    }

    public RecommendationResponse sendRequest(RecommendationRequest recommendationRequest) throws IOException, ZaiClientException {
        Call<RecommendationResponse> call = zaiAPI.getRecommendations(
                mlApiEndpoint + recommendationRequest.getPath(this.zaiClientId), recommendationRequest.getPayload()
        );

        Response<RecommendationResponse> response = call.execute();

        if (!response.isSuccessful()) {
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));
        }

        return response.body();
    }

    public EventLoggerResponse sendRequest(EventRequest eventRequest) throws IOException, ZaiClientException {
        List<Event> events = eventRequest.getPayload();

        Call<EventLoggerResponse> call = zaiAPI.addEventLog(events);

        Response<EventLoggerResponse> response = call.execute();

        if (!response.isSuccessful()) {
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));
        }

        return response.body();
    }

    public EventLoggerResponse sendRequest(EventRequest eventRequest, boolean isTest) throws IOException, ZaiClientException {
        List<Event> events = eventRequest.getPayload();

        if (isTest) {
            for (Event event : events) {
                event.setTimeToLive(Config.testEventTimeToLive);
            }
        }

        Call<EventLoggerResponse> call = zaiAPI.addEventLog(events);

        Response<EventLoggerResponse> response = call.execute();

        if (!response.isSuccessful()) {
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));
        }

        return response.body();

    }

    public EventLoggerResponse addEventLog(Event event) throws IOException, ZaiClientException {
        Call<EventLoggerResponse> call = zaiAPI.addEventLog(event);

        Response<EventLoggerResponse> response = call.execute();

        if (!response.isSuccessful())
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

        return response.body();
    }

    public EventLoggerResponse addEventLog(Event event, boolean isTest) throws IOException, ZaiClientException {
        if (isTest) {
            event.setTimeToLive(Config.testEventTimeToLive);
        }

        Call<EventLoggerResponse> call = zaiAPI.addEventLog(event);

        Response<EventLoggerResponse> response = call.execute();

        if (!response.isSuccessful())
            throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

        return response.body();
    }

    // public EventLoggerResponse addEventLog(EventBatch eventBatch) throws IOException, ZaiClientException, EmptyBatchException {
    //     List<Event> events = eventBatch.getEventList();

    //     if (events.size() == 0) throw new EmptyBatchException();

    //     Call<EventLoggerResponse> call = zaiAPI.addEventLog(events);
    //     Response<EventLoggerResponse> response = call.execute();

    //     if (!response.isSuccessful())
    //         throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

    //     eventBatch.setLogFlag();

    //     return response.body();
    // }

    // public EventLoggerResponse addEventLog(EventBatch eventBatch, boolean isTest) throws IOException, ZaiClientException, EmptyBatchException {
    //     List<Event> events = eventBatch.getEventList();

    //     if (events.size() == 0) throw new EmptyBatchException();

    //     if (isTest) {
    //         for (Event event : events) {
    //             event.setTimeToLive(Config.testEventTimeToLive);
    //         }
    //     }

    //     Call<EventLoggerResponse> call = zaiAPI.addEventLog(events);
    //     Response<EventLoggerResponse> response = call.execute();

    //     if (!response.isSuccessful())
    //         throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

    //     eventBatch.setLogFlag();

    //     return response.body();
    // }

    // public RecommendationResponse getRecommendations(RecommendationRequest recommendation) throws IOException, ZaiClientException {
    //     Call<RecommendationResponse> call = zaiAPI.getRecommendations(
    //             mlApiEndpoint + recommendation.getPath(this.zaiClientId), recommendation
    //     );
    //     Response<RecommendationResponse> response = call.execute();

    //     if (!response.isSuccessful())
    //         throw new ZaiClientException(getExceptionMessage(response), new HttpException(response));

    //     return response.body();
    // }

    private ZaiAPI instantiateZaiAPI() {
        String zaiClientId = this.zaiClientId;
        String zaiSecret = this.zaiSecret;
        String zaiHttpLog = System.getenv("ZAI_HTTP_LOG");

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
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
            });

        if (zaiHttpLog != null && zaiHttpLog.equals("true")) {
            clientBuilder = clientBuilder.addInterceptor(logging);
        }

        OkHttpClient client = clientBuilder.build();

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
            this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
            this.readTimeout = DEFAULT_READ_TIMEOUT;
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

            if (Pattern.matches("^[a-zA-Z0-9-]+$", endpoint)) {
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
