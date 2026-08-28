package com.springboot.android.api;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.springboot.android.BuildConfig;
import com.springboot.android.util.SessionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit = null;
    private static Context appContext;
    private static final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    /**
     * Copies cookies (SESSIONID, XSRF-TOKEN) set on a WebView-hosted OAuth2 flow into this
     * client's cookie jar. Needed because webauthn/** endpoints require a session-authenticated
     * principal (not just the JWT Authorization header), and android.webkit.CookieManager's
     * store is otherwise invisible to OkHttp.
     */
    public static void importCookiesFromWebView(Context context, String url) {
        String cookieHeader = android.webkit.CookieManager.getInstance().getCookie(url);
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (cookieHeader == null || cookieHeader.isEmpty() || httpUrl == null) {
            return;
        }

        List<Cookie> cookies = new ArrayList<>();
        for (String pair : cookieHeader.split(";")) {
            String trimmed = pair.trim();
            int idx = trimmed.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String name = trimmed.substring(0, idx);
            String value = trimmed.substring(idx + 1);
            Cookie cookie = Cookie.parse(httpUrl, name + "=" + value);
            if (cookie != null) {
                cookies.add(cookie);
                if ("XSRF-TOKEN".equals(name) && context != null) {
                    new SessionManager(context.getApplicationContext()).saveCsrfToken(value, "X-XSRF-TOKEN");
                }
            }
        }
        if (!cookies.isEmpty()) {
            cookieStore.put(httpUrl.host(), cookies);
        }
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .cookieJar(new CookieJar() {
                        @Override
                        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                            if (!cookies.isEmpty()) {
                                // Merge by name instead of overwriting: OkHttp calls this on
                                // every response, including ones with no Set-Cookie headers at
                                // all (empty list), which would otherwise wipe out cookies
                                // (e.g. SESSIONID imported from a WebView flow) that this
                                // particular response simply didn't mention.
                                java.util.Map<String, Cookie> merged = new java.util.LinkedHashMap<>();
                                List<Cookie> existing = cookieStore.get(url.host());
                                if (existing != null) {
                                    for (Cookie cookie : existing) {
                                        merged.put(cookie.name(), cookie);
                                    }
                                }
                                for (Cookie cookie : cookies) {
                                    merged.put(cookie.name(), cookie);
                                }
                                cookieStore.put(url.host(), new ArrayList<>(merged.values()));
                            }

                            // Extract CSRF token from cookies and save to SessionManager
                            if (appContext != null) {
                                for (Cookie cookie : cookies) {
                                    if (cookie.name().equals("XSRF-TOKEN")) {
                                        SessionManager sessionManager = new SessionManager(appContext);
                                        sessionManager.saveCsrfToken(cookie.value(), "X-XSRF-TOKEN");
                                    }
                                }
                            }
                        }

                        @Override
                        public List<Cookie> loadForRequest(HttpUrl url) {
                            List<Cookie> cookies = cookieStore.get(url.host());
                            return cookies != null ? cookies : new ArrayList<>();
                        }
                    });

            // Add authentication and CSRF interceptor
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder();

                    // Add authorization header and CSRF token if they exist
                    if (appContext != null) {
                        SessionManager sessionManager = new SessionManager(appContext);
                        String token = sessionManager.getAuthToken();
                        if (token != null && !token.isEmpty()) {
                            requestBuilder.header("Authorization", "Bearer " + token);
                        }

                        // Add CSRF token for state-changing requests (POST, PUT, DELETE, PATCH)
                        String method = original.method();
                        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("OPTIONS")) {
                            String csrfToken = sessionManager.getCsrfToken();
                            String csrfHeader = sessionManager.getCsrfHeader();
                            if (csrfToken != null && !csrfToken.isEmpty()) {
                                requestBuilder.header(csrfHeader, csrfToken);
                            }
                        }
                    }

                    requestBuilder.header("Content-Type", "application/json");
                    requestBuilder.method(original.method(), original.body());

                    Request request = requestBuilder.build();
                    Response response = chain.proceed(request);

                    // Log response body for debugging JSON parsing issues
                    if (BuildConfig.DEBUG && response.body() != null) {
                        String responseBody = response.body().string();
                        android.util.Log.d("ApiClient", "URL: " + request.url());
                        android.util.Log.d("ApiClient", "Response: " + responseBody.substring(0, Math.min(1000, responseBody.length())));
                        // Recreate response with the body since we consumed it
                        return response.newBuilder()
                                .body(okhttp3.ResponseBody.create(response.body().contentType(), responseBody))
                                .build();
                    }

                    return response;
                }
            });

            if (BuildConfig.DEBUG) {
                httpClient.addInterceptor(logging);
            }

            // Create lenient Gson instance to handle JSON parsing issues
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClient.build())
                    .build();
        }
        return retrofit;
    }
}
