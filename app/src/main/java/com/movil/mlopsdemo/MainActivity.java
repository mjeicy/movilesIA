package com.movil.mlopsdemo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvResult, tvMlops, tvHistorial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etNombre = findViewById(R.id.etNombre);
        EditText etAsist = findViewById(R.id.etAsist);
        EditText etHoras = findViewById(R.id.etHoras);
        EditText etP1 = findViewById(R.id.etP1);
        EditText etP2 = findViewById(R.id.etP2);
        EditText etTareas = findViewById(R.id.etTareas);
        Button btn = findViewById(R.id.btnRegistrar);
        tvResult = findViewById(R.id.tvResult);
        tvMlops = findViewById(R.id.tvMlops);
        tvHistorial = findViewById(R.id.tvHistorial);

        btn.setOnClickListener(view -> {
            String nombre = etNombre.getText().toString().trim();
            Double asist = parse(etAsist);
            Double horas = parse(etHoras);
            Double p1 = parse(etP1);
            Double p2 = parse(etP2);
            Double tareas = parse(etTareas);

            if (nombre.isEmpty() || asist == null || horas == null
                    || p1 == null || p2 == null || tareas == null) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            tvResult.setText("Consultando API...");
            tvMlops.setText("");
            tvHistorial.setText("");

            Models.EstudianteRequest req =
                    new Models.EstudianteRequest(nombre, asist, horas, p1, p2, tareas);

            RetrofitClient.getApi().registrar(req)
                    .enqueue(new Callback<Models.EstudianteResponse>() {
                        @Override
                        public void onResponse(Call<Models.EstudianteResponse> call,
                                               Response<Models.EstudianteResponse> resp) {
                            if (!resp.isSuccessful() || resp.body() == null) {
                                tvResult.setText("Error " + resp.code());
                                return;
                            }
                            Models.EstudianteResponse r = resp.body();
                            tvResult.setText(r.nombre + ": " + r.prediccion + "\n"
                                    + "Prob. de aprobar: "
                                    + String.format("%.1f", r.prob_aprobar * 100)
                                    + "% (riesgo " + r.riesgo + ")");
                            cargarMetricas(r);
                            cargarHistorial();
                        }

                        @Override
                        public void onFailure(Call<Models.EstudianteResponse> call, Throwable t) {
                            tvResult.setText("Error: " + t.getMessage());
                        }
                    });
        });
    }

    private void cargarMetricas(Models.EstudianteResponse r) {
        RetrofitClient.getApi().metrics().enqueue(new Callback<Models.MetricsResponse>() {
            @Override
            public void onResponse(Call<Models.MetricsResponse> call,
                                   Response<Models.MetricsResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null) return;
                Models.MetricsResponse m = resp.body();
                tvMlops.setText(
                        "PUNTOS MLOPS\n"
                                + "• Versión del modelo: " + r.model_version + "\n"
                                + "• Accuracy: " + m.accuracy + " | F1: " + m.f1 + "\n"
                                + "• Entrenado: " + m.trained_at + "\n"
                                + "• Latencia de inferencia: " + r.latency_ms + " ms\n"
                                + "• Predicciones totales: " + m.total_predictions + "\n"
                                + "• Trazabilidad: guardado en Supabase\n"
                                + "• Despliegue: GitHub → Render");
            }

            @Override
            public void onFailure(Call<Models.MetricsResponse> call, Throwable t) {
                tvMlops.setText("Error métricas: " + t.getMessage());
            }
        });
    }

    private void cargarHistorial() {
        RetrofitClient.getApi().listar().enqueue(new Callback<List<Models.EstudianteRow>>() {
            @Override
            public void onResponse(Call<List<Models.EstudianteRow>> call,
                                   Response<List<Models.EstudianteRow>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) return;
                StringBuilder sb = new StringBuilder("ÚLTIMOS REGISTROS\n");
                for (Models.EstudianteRow e : resp.body()) {
                    double p = e.prob_aprobar == null ? 0 : e.prob_aprobar;
                    sb.append("• ").append(e.nombre).append(": ")
                            .append(e.prediccion).append(" (")
                            .append(String.format("%.0f", p * 100)).append("%)\n");
                }
                tvHistorial.setText(sb.toString());
            }

            @Override
            public void onFailure(Call<List<Models.EstudianteRow>> call, Throwable t) {
                tvHistorial.setText("Error historial: " + t.getMessage());
            }
        });
    }

    private Double parse(EditText et) {
        try {
            return Double.parseDouble(et.getText().toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}