package org.zaikorea.ZaiClient;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.zaikorea.ZaiClient.configs.Config;
import org.zaikorea.ZaiClient.exceptions.EmptyBatchException;
import org.zaikorea.ZaiClient.exceptions.ZaiClientException;
import org.zaikorea.ZaiClient.request.Event;
import org.zaikorea.ZaiClient.request.EventBatch;
import org.zaikorea.ZaiClient.response.EventLoggerResponse;
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

    public ZaiClient(String zaiClientId, String zaiSecret) {
        this.zaiClientId = zaiClientId;
        this.zaiSecret = zaiSecret;
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

    private ZaiAPI instantiateZaiAPI() {
        String zaiClientId = this.zaiClientId;
        String zaiSecret = this.zaiSecret;

        OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
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
                .baseUrl(Config.eventsApiEndPoint)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        return retrofit.create(ZaiAPI.class);
    }

    protected String getExceptionMessage(Response<?> response) {
        String error = null;
        try {
            assert response.errorBody() != null;
            JsonElement element = JsonParser.parseString(response.errorBody().string());
            error = element.toString();
        } catch (Exception | AssertionError e) {
            e.printStackTrace();
        }

        String responseMessage = response.message();
        if (error == null)
            error = (responseMessage != null) ? responseMessage : "Internal server error.";

        return error;
    }
}
