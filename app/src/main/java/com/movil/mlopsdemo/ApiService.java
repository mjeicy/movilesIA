package com.movil.mlopsdemo;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @POST("estudiantes")
    Call<Models.EstudianteResponse> registrar(@Body Models.EstudianteRequest req);

    @GET("estudiantes")
    Call<List<Models.EstudianteRow>> listar();

    @GET("metrics")
    Call<Models.MetricsResponse> metrics();
}