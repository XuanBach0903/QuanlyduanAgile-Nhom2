package com.example.appbanghe.network;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.CookieManager;
import java.net.CookiePolicy;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.JavaNetCookieJar;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static volatile Retrofit retrofit;

    private ApiClient() {
    }

    public static Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    retrofit = buildRetrofit(context);
                }
            }
        }
        return retrofit;
    }

    private static Retrofit buildRetrofit(Context context) {
        Gson gson = new GsonBuilder().setLenient().create();

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        SessionManager sessionManager = new SessionManager(context);

        Interceptor authInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder();

            String token = sessionManager.getToken();
            if (token != null && !token.trim().isEmpty()) {
                builder.header("Authorization", "Bearer " + token);
            }

            return chain.proceed(builder.build());
        };

        OkHttpClient client = new OkHttpClient.Builder()
                .cookieJar(new JavaNetCookieJar(cookieManager))
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build();

        return new Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();
    }

    public static <T> T createService(Context context, Class<T> serviceClass) {
        return getRetrofit(context).create(serviceClass);
    }
}
