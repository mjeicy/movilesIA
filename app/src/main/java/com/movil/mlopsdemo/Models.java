package com.movil.mlopsdemo;

import java.util.List;

public class Models {

    public static class EstudianteRequest {
        public String nombre;
        public double asistencia, horas_estudio, parcial1, parcial2, tareas;

        public EstudianteRequest(String nombre, double asistencia, double horas,
                                 double p1, double p2, double tareas) {
            this.nombre = nombre;
            this.asistencia = asistencia;
            this.horas_estudio = horas;
            this.parcial1 = p1;
            this.parcial2 = p2;
            this.tareas = tareas;
        }
    }

    public static class EstudianteResponse {
        public String nombre, prediccion, riesgo, model_version;
        public double prob_aprobar, latency_ms;
    }

    public static class EstudianteRow {
        public long id;
        public String nombre, prediccion;
        public Double prob_aprobar;
    }

    public static class MetricsResponse {
        public String model_version, trained_at;
        public double accuracy, f1;
        public int total_predictions;
    }
}
