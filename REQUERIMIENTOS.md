# Documento de Requerimientos
## MLOps: Predicción de rendimiento académico con IA

| | |
|---|---|
| **Materia** | INF-254 Dispositivos Móviles II |
| **Laboratorio** | Nro. 9: MLOps: predicción de rendimiento académico con IA |
| **Estudiante** | Michelle Jeicy Avilés Loza |
| **Docente** | Dr. Juan Marcos Miranda Nina, Ph.D. |
| **Versión del documento** | 1.0 |
| **Fecha** | 24 de noviembre de 2026 |
| **Ubicación** | La Paz, Bolivia |

---

## 1. Introducción

### 1.1 Propósito
Este documento especifica los requerimientos del sistema **MLOps de predicción de rendimiento académico**, un flujo de extremo a extremo que entrena y versiona un modelo de IA, lo despliega como API REST y lo consume desde una aplicación Android, dejando trazabilidad de cada predicción en una base de datos.

### 1.2 Alcance
El sistema permite registrar los datos académicos de un estudiante (asistencia, horas de estudio, notas parciales y tareas) y obtener:

- Una predicción (**Aprobará** / **En riesgo**).
- La probabilidad de aprobar y un nivel de riesgo (Bajo / Medio / Alto).
- Métricas del modelo y de operación (versión, accuracy, F1, latencia, total de predicciones).
- Un historial de los últimos registros.

El modelo apoya la detección temprana de estudiantes en riesgo y **no reemplaza la decisión del docente**.

### 1.3 Definiciones

| Término | Definición |
|---|---|
| MLOps | Prácticas para entrenar, versionar, desplegar y monitorear modelos de ML en producción |
| CI/CD | Integración y despliegue continuos (cada `git push` redespliega el servicio) |
| Trazabilidad | Registro de cada predicción con su versión de modelo y latencia |
| Latencia | Tiempo de inferencia del modelo, en milisegundos |

---

## 2. Descripción general

### 2.1 Arquitectura

```
Google Colab ──► model_v1.joblib + metrics.json ──► GitHub ──► Render (FastAPI)
                                                                  │      │
                                                                  ▼      ▼
                                                        App Android   Supabase
                                                      (Java/Retrofit) (PostgreSQL)
```

### 2.2 Tecnologías

| Componente | Tecnología | Función |
|---|---|---|
| Entrenamiento | Google Colab + scikit-learn | Entrena un Random Forest y exporta `model_v1.joblib` |
| Base de datos | Supabase (PostgreSQL) | Tabla `estudiantes` para trazabilidad |
| API | FastAPI + Uvicorn | Expone `/estudiantes`, `/metrics`, `/health` |
| Versionado | GitHub | Repositorio `api-academica` |
| Despliegue | Render (plan Free) | Web Service con CI/CD |
| Cliente móvil | Android Studio + Retrofit 2.9.0 + Gson | App en Java |

### 2.3 Usuarios
- **Docente / usuario final:** ingresa los datos del estudiante y consulta la predicción.
- **Responsable MLOps:** entrena, versiona y despliega nuevas versiones del modelo.

### 2.4 Restricciones
- El plan gratuito de Render suspende el servicio por inactividad; la primera petición puede tardar.
- Android mínimo: **API 24 (Android 7.0)**; lenguaje **Java 8**.
- La versión de `scikit-learn` en `requirements.txt` debe coincidir con la usada en Colab.
- El modelo se entrena con datos sintéticos (1000 registros, semilla 42).

---

## 3. Requerimientos funcionales

### 3.1 Modelo de IA

| ID | Requerimiento | Prioridad |
|---|---|---|
| RF-01 | El sistema debe entrenar un `RandomForestClassifier` (100 árboles) con las variables `asistencia`, `horas_estudio`, `parcial1`, `parcial2`, `tareas` para predecir `aprobado`. | Alta |
| RF-02 | El entrenamiento debe usar una partición 80 % entrenamiento / 20 % prueba. | Alta |
| RF-03 | El sistema debe calcular **accuracy** y **F1** sobre el conjunto de prueba. | Alta |
| RF-04 | El modelo debe exportarse como `model_v1.joblib` y las métricas como `metrics.json` (versión, accuracy, F1, fecha de entrenamiento). | Alta |
| RF-05 | El cuaderno debe leer las credenciales de Supabase desde Colab Secrets (`SUPABASE_URL`, `SUPABASE_KEY`). | Media |

### 3.2 API REST (FastAPI)

| ID | Requerimiento | Prioridad |
|---|---|---|
| RF-06 | `GET /health` debe devolver el estado del servicio y la versión del modelo. | Alta |
| RF-07 | `GET /metrics` debe devolver el contenido de `metrics.json` más `total_predictions`. | Alta |
| RF-08 | `POST /estudiantes` debe recibir `nombre`, `asistencia`, `horas_estudio`, `parcial1`, `parcial2`, `tareas`, ejecutar la inferencia y devolver `nombre`, `prediccion`, `prob_aprobar`, `riesgo`, `model_version`, `latency_ms`. | Alta |
| RF-09 | La predicción será **"Aprobará"** si `prob_aprobar ≥ 0.5`; en caso contrario, **"En riesgo"**. | Alta |
| RF-10 | El riesgo será **Bajo** (≥ 0.75), **Medio** (≥ 0.5 y < 0.75) o **Alto** (< 0.5). | Alta |
| RF-11 | La API debe validar los rangos de entrada: asistencia, parciales y tareas entre 0 y 100; horas de estudio entre 0 y 40. | Alta |
| RF-12 | Cada predicción debe guardarse en Supabase con probabilidad, predicción, versión del modelo y latencia. | Alta |
| RF-13 | `GET /estudiantes` debe devolver los **5 últimos registros** (`id`, `nombre`, `prediccion`, `prob_aprobar`) en orden descendente. | Media |
| RF-14 | Si Supabase no responde o no está configurado, la API debe seguir devolviendo la predicción sin fallar. | Media |
| RF-15 | La API debe publicar documentación interactiva en `/docs`. | Baja |

### 3.3 Aplicación Android

| ID | Requerimiento | Prioridad |
|---|---|---|
| RF-16 | La app debe presentar un formulario con: nombre, asistencia, horas de estudio, parcial 1, parcial 2 y tareas. | Alta |
| RF-17 | La app debe validar que todos los campos estén completos y mostrar un mensaje si falta alguno. | Alta |
| RF-18 | Al presionar **Registrar y predecir**, la app debe enviar los datos a `POST /estudiantes` y mostrar nombre, predicción, probabilidad (%) y riesgo. | Alta |
| RF-19 | La app debe consultar `/metrics` y mostrar versión del modelo, accuracy, F1, fecha de entrenamiento, latencia, total de predicciones, trazabilidad y despliegue. | Alta |
| RF-20 | La app debe consultar `GET /estudiantes` y mostrar los últimos registros. | Media |
| RF-21 | La app debe mostrar mensajes de error ante fallas de red o respuestas no exitosas. | Media |

### 3.4 Persistencia (Supabase)

| ID | Requerimiento | Prioridad |
|---|---|---|
| RF-22 | Debe existir la tabla `estudiantes` con los campos: `id`, `created_at`, `nombre`, `asistencia`, `horas_estudio`, `parcial1`, `parcial2`, `tareas`, `prob_aprobar`, `prediccion`, `model_version`, `latency_ms`. | Alta |

### 3.5 MLOps

| ID | Requerimiento | Prioridad |
|---|---|---|
| RF-23 | **Versionado:** el modelo y el código deben almacenarse en el repositorio GitHub `api-academica`. | Alta |
| RF-24 | **CI/CD:** cada `git push` debe redesplegar automáticamente la API en Render. | Alta |
| RF-25 | **Monitoreo:** el sistema debe registrar latencia de inferencia y total de predicciones. | Media |
| RF-26 | **Reentrenamiento:** debe ser posible publicar `model_v2.joblib`, actualizar `main.py` y `metrics.json`, y que la app muestre la versión `v2` sin modificar su código. | Media |

---

## 4. Requerimientos no funcionales

| ID | Categoría | Requerimiento |
|---|---|---|
| RNF-01 | Rendimiento | La inferencia debe completarse en pocos milisegundos y su latencia debe reportarse en cada respuesta. |
| RNF-02 | Disponibilidad | El servicio debe estar accesible desde una URL pública HTTPS en Render (con posible arranque en frío en plan Free). |
| RNF-03 | Compatibilidad | La app debe funcionar en Android 7.0 (API 24) o superior. |
| RNF-04 | Usabilidad | La interfaz debe ser una sola pantalla con desplazamiento (`ScrollView`) y teclado numérico en campos numéricos. |
| RNF-05 | Tolerancia a fallos | Timeouts de conexión y lectura de 60 s en la app (por el arranque en frío) y de 5 s hacia Supabase en la API. |
| RNF-06 | Seguridad | Las llaves de Supabase deben configurarse como variables de entorno (Render) o secretos (Colab) y **no** subirse al repositorio ni mostrarse en capturas. |
| RNF-07 | Mantenibilidad | El modelo debe poder actualizarse sin cambiar la app móvil. |
| RNF-08 | Reproducibilidad | El entrenamiento debe usar semillas fijas (`42`) y registrar la versión de scikit-learn. |
| RNF-09 | Portabilidad | La API debe ejecutarse con Python 3 (se sugiere `PYTHON_VERSION=3.11.9` si falla la instalación). |

---

## 5. Requerimientos de datos

### 5.1 Variables de entrada

| Variable | Tipo | Rango válido |
|---|---|---|
| `nombre` | texto | no vacío |
| `asistencia` | decimal | 0 – 100 |
| `horas_estudio` | decimal | 0 – 40 |
| `parcial1` | decimal | 0 – 100 |
| `parcial2` | decimal | 0 – 100 |
| `tareas` | decimal | 0 – 100 |

### 5.2 Variable objetivo (entrenamiento)
`aprobado = 1` si `0.30·parcial1 + 0.30·parcial2 + 0.15·asistencia + 0.15·tareas + 1.0·horas_estudio + ruido ≥ 60`; en caso contrario `0`.

---

## 6. Interfaces externas

### 6.1 Endpoints de la API

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/health` | Estado del servicio y versión del modelo |
| GET | `/metrics` | Métricas de entrenamiento y total de predicciones |
| POST | `/estudiantes` | Registra datos y devuelve la predicción |
| GET | `/estudiantes` | Últimos 5 registros |

**Ejemplo de petición** (`POST /estudiantes`):

```json
{"nombre": "Ana", "asistencia": 90, "horas_estudio": 10, "parcial1": 80, "parcial2": 75, "tareas": 85}
```

### 6.2 Variables de entorno

| Variable | Dónde | Descripción |
|---|---|---|
| `SUPABASE_URL` | Render | Project URL de Supabase |
| `SUPABASE_KEY` | Render | anon public key de Supabase |
| `PYTHON_VERSION` | Render (opcional) | `3.11.9` |

### 6.3 Estructura del repositorio

```
api-academica/
├── main.py
├── requirements.txt
├── model_v1.joblib
└── metrics.json
```

### 6.4 Dependencias

**API (`requirements.txt`):** `fastapi`, `uvicorn`, `scikit-learn==1.6.1` (igual a la versión de Colab), `joblib`, `numpy`, `requests`.

**Android (`app/build.gradle`):** `retrofit:2.9.0`, `converter-gson:2.9.0`, compatibilidad Java 8, permiso `INTERNET` en el manifiesto.

---

## 7. Configuración de despliegue en Render

| Configuración | Valor |
|---|---|
| Runtime | Python 3 |
| Build Command | `pip install -r requirements.txt` |
| Start Command | `uvicorn main:app --host 0.0.0.0 --port $PORT` |
| Plan | Free |

---

## 8. Criterios de aceptación

| ID | Criterio |
|---|---|
| CA-01 | Colab genera `model_v1.joblib` y `metrics.json` e imprime accuracy, F1 y versión de scikit-learn. |
| CA-02 | `GET /health` responde `{"status": "ok", "model_version": "v1"}`. |
| CA-03 | Con el JSON de ejemplo (Ana), `POST /estudiantes` responde **Aprobará**. |
| CA-04 | Con notas bajas (30 y 20) y asistencia 50, la respuesta es **En riesgo**. |
| CA-05 | Cada petición exitosa crea una fila nueva en la tabla `estudiantes` de Supabase. |
| CA-06 | La app muestra predicción, puntos MLOps y últimos registros tras presionar **Registrar y predecir**. |
| CA-07 | Un `git push` provoca un nuevo despliegue en Render. |
| CA-08 | Tras subir `model_v2.joblib` y actualizar `main.py` y `metrics.json`, la app muestra la versión `v2`. |

---

## 9. Matriz de trazabilidad MLOps

| Punto MLOps | Evidencia | Dónde verlo | Requerimientos |
|---|---|---|---|
| Versionado del modelo | `model_v1.joblib` en el repositorio y versión en la app | GitHub / App | RF-04, RF-23 |
| Métricas de entrenamiento | `metrics.json` (accuracy y F1) | Colab / App | RF-03, RF-04, RF-07 |
| Serving (API) | FastAPI con `/estudiantes`, `/metrics`, `/health` | Render `/docs` | RF-06 a RF-15 |
| CI/CD | Cada `git push` redespliega | GitHub → Render | RF-24 |
| Monitoreo | Latencia y total de predicciones | App | RF-19, RF-25 |
| Trazabilidad | Cada predicción en la tabla `estudiantes` | Supabase | RF-12, RF-22 |
| Reentrenamiento | `model_v2.joblib` + push; la app muestra v2 | GitHub / App | RF-26 |

---

## 10. Riesgos y consideraciones

| Riesgo | Mitigación |
|---|---|
| Arranque en frío de Render (plan Free) | Abrir `/health` antes de usar la app; timeouts de 60 s en el cliente |
| Incompatibilidad de versión de scikit-learn al cargar el modelo | Fijar en `requirements.txt` la versión impresa por Colab |
| Row Level Security deshabilitado en la tabla `estudiantes` | Aceptable solo en laboratorio; en producción habilitar RLS y políticas |
| Uso de la `anon key` desde el servidor | En producción usar la `service_role key` solo en el backend y nunca exponerla |
| Datos de entrenamiento sintéticos | Reentrenar con datos reales antes de usar el modelo para decisiones |
| Sesgo y uso responsable | El resultado es un apoyo a la decisión docente, no un dictamen |
| Fallo silencioso al guardar en Supabase (`except: pass`) | Agregar registro de errores (logging) en versiones futuras |

---

## 11. Mejoras futuras

- Registrar y comparar múltiples versiones de modelo (`v1`, `v2`, ...).
- Pruebas automatizadas de la API (`pytest`) en GitHub Actions.
- Autenticación en la API y en la app.
- Historial paginado y filtros por estudiante.
- Detección de *data drift* y reentrenamiento programado.
