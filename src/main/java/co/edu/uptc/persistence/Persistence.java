package co.edu.uptc.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Persistence<T> {
    private final Gson gson;
    private final String filePath;
    private final Type type;
    private RuntimeTypeAdapterFactory<?> typeAdapterFactory;

    public Persistence(String filePath, Type listType, RuntimeTypeAdapterFactory<?> typeAdapterFactory) {
        this.filePath = filePath;
        this.type = listType;
        this.typeAdapterFactory = typeAdapterFactory;
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        if (typeAdapterFactory != null) {
            builder.registerTypeAdapterFactory(typeAdapterFactory);
        }
        this.gson = builder.create();
    }

    public Persistence(String filePath, Type listType) {
        this(filePath, listType, null);
    }

    public Persistence(String filePath, RuntimeTypeAdapterFactory<?> typeAdapterFactory) {
        this(filePath, new TypeToken<List<T>>() {
        }.getType(), typeAdapterFactory);
    }

    public Persistence(String filePath) {
        this(filePath, (RuntimeTypeAdapterFactory<?>) null);
    }

    public void saveList(List<T> objects) throws IOException {
        Path path = Paths.get(this.filePath);
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
            System.out.println("Persistence: Directorio creado en: " + parentDir.toAbsolutePath());
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8")) {
            gson.toJson(objects, writer);
        }
    }

    public List<T> loadList() throws IOException {
        Path path = Paths.get(this.filePath);
        if (!Files.exists(path)) {
            System.out.println("Persistence INFO: Archivo no encontrado en '" + this.filePath
                    + "'. Se devolverá una lista vacía.");
            return new ArrayList<>(); // ...devuelve una lista vacía.
        }
        try (Reader reader = new InputStreamReader(new FileInputStream(filePath), "UTF-8")) {
            List<T> list = gson.fromJson(reader, this.type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Persistence ERROR: Error al leer o parsear el archivo JSON: " + this.filePath);
            e.printStackTrace();
            throw new IOException("Error al leer el archivo JSON: " + e.getMessage(), e);
        }
    }

    public boolean delete() throws IOException {
        return Files.deleteIfExists(Paths.get(this.filePath));
    }

    public boolean exists() {
        return Files.exists(Paths.get(this.filePath));
    }

    public static class RuntimeTypeAdapterFactory<T> implements com.google.gson.TypeAdapterFactory {
        private final Class<?> baseType;
        private final String typeFieldName;
        private final java.util.Map<String, Class<?>> labelToSubtype = new java.util.LinkedHashMap<>();
        private final java.util.Map<Class<?>, String> subtypeToLabel = new java.util.LinkedHashMap<>();

        private RuntimeTypeAdapterFactory(Class<?> baseType, String typeFieldName) {
            this.baseType = baseType;
            this.typeFieldName = typeFieldName;
        }

        public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName) {
            return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName);
        }

        public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType) {
            return new RuntimeTypeAdapterFactory<>(baseType, "type");
        }

        public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type, String label) {
            labelToSubtype.put(label, type);
            subtypeToLabel.put(type, label);
            return this;
        }

        public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type) {
            return registerSubtype(type, type.getSimpleName());
        }

        @Override
        public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
            if (type == null || !baseType.isAssignableFrom(type.getRawType())) {
                return null;
            }

            final TypeAdapter<com.google.gson.JsonElement> jsonElementAdapter = gson
                    .getAdapter(com.google.gson.JsonElement.class);
            final java.util.Map<String, TypeAdapter<?>> labelToDelegate = new java.util.LinkedHashMap<>();
            final java.util.Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new java.util.LinkedHashMap<>();

            for (java.util.Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
                TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
                labelToDelegate.put(entry.getKey(), delegate);
                subtypeToDelegate.put(entry.getValue(), delegate);
            }

            return new TypeAdapter<R>() {
                @Override
                public R read(JsonReader in) throws IOException {
                    com.google.gson.JsonElement jsonElement = jsonElementAdapter.read(in);
                    if (!jsonElement.isJsonObject()) {
                        throw new com.google.gson.JsonParseException("Se esperaba un objeto JSON pero se encontró: "
                                + jsonElement.getClass().getSimpleName());
                    }
                    com.google.gson.JsonElement labelJsonElement = jsonElement.getAsJsonObject().get(typeFieldName);

                    if (labelJsonElement == null) {
                        throw new com.google.gson.JsonParseException(
                                "No se encontró el campo '" + typeFieldName + "' en el objeto JSON");
                    }
                    if (!labelJsonElement.isJsonPrimitive() || !labelJsonElement.getAsJsonPrimitive().isString()) {
                        throw new com.google.gson.JsonParseException(
                                "El campo '" + typeFieldName + "' debe ser un String");
                    }

                    String label = labelJsonElement.getAsString();
                    @SuppressWarnings("unchecked")
                    TypeAdapter<R> delegate = (TypeAdapter<R>) labelToDelegate.get(label);

                    if (delegate == null) {
                        throw new com.google.gson.JsonParseException(
                                "Tipo desconocido encontrado en JSON: '" + label + "'");
                    }

                    return delegate.fromJsonTree(jsonElement);
                }

                @Override
                public void write(JsonWriter out, R value) throws IOException {
                    Class<?> srcType = value.getClass();
                    String label = subtypeToLabel.get(srcType);
                    @SuppressWarnings("unchecked")
                    TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToDelegate.get(srcType);

                    if (delegate == null || label == null) {
                        throw new com.google.gson.JsonParseException(
                                "No se puede serializar el tipo " + srcType.getName() +
                                        ". ¿Está registrado en RuntimeTypeAdapterFactory?");
                    }

                    com.google.gson.JsonObject jsonObject = delegate.toJsonTree(value).getAsJsonObject();

                    if (jsonObject == null) {
                        throw new com.google.gson.JsonParseException(
                                "El TypeAdapter delegado devolvió null para " + srcType.getName());
                    }

                    if (jsonObject.has(typeFieldName)) {
                        System.err.println(
                                "Advertencia: El objeto ya tiene un campo '" + typeFieldName + "'. Se sobrescribirá.");
                    }

                    com.google.gson.JsonObject clone = new com.google.gson.JsonObject();
                    clone.add(typeFieldName, new com.google.gson.JsonPrimitive(label));

                    for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : jsonObject.entrySet()) {
                        clone.add(e.getKey(), e.getValue());
                    }

                    jsonElementAdapter.write(out, clone);
                }
            };
        }
    }
}