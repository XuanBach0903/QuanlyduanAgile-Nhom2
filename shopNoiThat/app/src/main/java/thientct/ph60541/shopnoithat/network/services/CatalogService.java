package thientct.ph60541.shopnoithat.network.services;

import com.google.gson.JsonArray;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CatalogService {
    @GET("danh-muc")
    Call<JsonArray> getDanhMuc();
}
