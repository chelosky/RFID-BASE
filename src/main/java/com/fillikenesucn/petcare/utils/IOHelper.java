package com.fillikenesucn.petcare.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.fillikenesucn.petcare.models.Acontecimiento;
import com.fillikenesucn.petcare.models.Pet;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Esta clase representa a un helper para la obtencion de información de las mascotas y sus acontecimientos
 * @author: Rodrigo Dorat Merejo
 * @version: 06/04/2020
 */

public class IOHelper {

    /**
     * Función que se encarga de verificar si ya está creado el archivo de texto
     * @param context contexto asociado a la vista que llama a la función
     * @return Retorna un booleano que avisa si existe el archivo de texto
     */
    public static Boolean CheckFile(Context context){
        try {
            // Buscamos el txt de 'base de datos'
            FileInputStream fis = context.openFileInput("petcare.txt");
            // Si existe retorna verdadero para que no se cree de nuevo
            if (fis != null) { return true; }
            // Si no existe retorna falso para crear uno
            else { return false; }
        } catch (FileNotFoundException e){ return false; }
    }

    /**
     * Función que se encarga de generar un nuevo archivo de texto
     * @param context contexto asociado a la vista que llama a la función
     * @param objStr String que representa la información que habrá dentro del texto (en esta oportunidad es un array vacío
     */
    public static void WriteJson(Context context, String objStr){
        FileOutputStream outputStream;
        try {
            // Usado para transformar de JSON a String que simula un JSON
            Gson gson = new Gson();
            // Se genera el archivo de texto por primera vez
            outputStream = context.openFileOutput("petcare.txt", Context.MODE_PRIVATE);
            // Dentro del txt sólo hay un  array vacío
            outputStream.write(gson.toJson(objStr).getBytes());
            outputStream.close();
            Log.d("DORAT", "Archivo creado exitosamente!");
        } catch (Exception e) { Log.d("DORAT", "No se pudo crear el archivo..."); }
    }

    /**
     * Función que se encarga de leer el archivo de texto de la aplicación
     * @param context contexto asociado a la vista que llama a la función
     * @return Retorna un string con la información que existe dentro del archivo
     */
    public static String ReadFileString(Context context){
        try {
            // Buscamos y leemos el archivo de texto que funciona como 'base de datos'
            FileInputStream fis = context.openFileInput("petcare.txt");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            // Pasamos el texto con saltos de línea a una sola línea de texto
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
            // Limpiamos la línea de texto de caracteres innecesarios que se agregan con GSON
            String json = sb.toString();
            json = json.replace("\\", "");
            json = json.substring(1);
            json = json.substring(0, json.length() - 1);
            return json;
        }catch (FileNotFoundException e){
            return "FileNotFoundException";
        }catch (IOException e){
            return "IOException";
        }
    }

    /**
     * Función que se encarga de revisar si el EPC que queremos asignar a una mascota ya está en uso
     * @param json información que hay dentro del archivo de texto
     * @param object el nuevo objeto mascota que se va a ingresar con el EPC
     * @return Retorna un Integer que corresponde a la posición en la lista si encontró el EPC utilizado, en su defecto -1
     */
    public static Integer CheckActiveEPC(JSONArray json, JSONObject object){
        try {
            // Buscamos en la JSONArray
            for (int i = 0; i < json.length(); i++) {
                // Guardamos el JSONObject visitado
                JSONObject curr = json.getJSONObject(i);
                // Revisamos si coincide con la EPC y si está activo
                if (curr.getString("EPC").equals(object.getString("EPC"))
                        && curr.getBoolean("active")) return i;
            }
            return -1;
        } catch (Exception e) {
            Log.d("DORAT", e.toString());
            return -1;
        }
    }

    /**
     * Función complementaria que se encarga de revisar si el EPC que queremos asignar a una mascota ya está en uso
     * @param context contexto asociado a la vista que llama a la función
     * @param epc el tag escaneado para buscar a la mascota
     * @return Retorna un Integer que corresponde a la posición en la lista si encontró el EPC utilizado, en su defecto -1
     */
    public static Integer CheckActiveEPC(Context context, String epc){
        try{
            // Se lee el archivo
            String jsonTxt = IOHelper.ReadFileString(context);
            // Lo transformamos a una lista
            JSONArray jsonArray = new JSONArray(jsonTxt);
            // Inicializamos un objeto que tendrá el EPC
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("EPC",epc);
            // Usamos la función original para ver si existe
            return CheckActiveEPC(jsonArray, jsonObject);
        }catch (Exception e){
            Log.d("DORAT", e.toString());
            return -1;
        }
    }

    /**
     * Función que se encarga de generar un nuevo archivo de texto con la información ACTUALIZADA
     * @param context contexto asociado a la vista que llama a la función
     * @param pet objeto Pet que se añadirá a la lista
     * @return retorna true si termina de agregar la mascota, en caso contrario false
     */
    public static boolean AddPet(Context context, Pet pet){
        try {
            String sxml = ReadFileString(context); // Obtenemos el JSON desde el contexto de la APP
            JSONArray json = new JSONArray(sxml); // Transformamos el String a JSONArray
            Gson gson = new Gson();
            String objStr = gson.toJson(pet); // Transformamos el Pet a String
            JSONObject object = new JSONObject(objStr); // Transformamos el String a JSONObject
            if (CheckActiveEPC(json, object) == -1){ // Check if the EPC is used by an active pet
                json.put(object); // Agregamos el JSONObject al JSONArray
                WriteJson(context,json.toString()); // Escribimos el nuevo txt
                return true;
            }
            Toast.makeText(context, "TAG OCUPADO", Toast.LENGTH_SHORT).show();
            return false;
        } catch (Exception e) {
            Log.d("DORAT", e.toString());
            Toast.makeText(context, "ERROR AL REGISTRAR", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Función que se encarga de agregar un nuevo evento a la mascota
     * @param context contexto asociado a la vista que llama a la función
     * @param acontecimiento información del nuevo evento que se va a agregar
     * @param epc el EPC que corresponde al de la mascota que le pertenece el evento
     * @return retorna true si termina de agregar el evento, en caso contrario false
     */
    public static boolean AddEvent(Context context, Acontecimiento acontecimiento, String epc){
        try {
            String sxml = ReadFileString(context); // Obtenemos el JSON desde el contexto de la APP
            JSONArray json = new JSONArray(sxml); // Transformamos el String a JSONArray
            Gson gson = new Gson();
            String objStr = gson.toJson(acontecimiento); // Transformamos el acontecimiento a String
            JSONObject event = new JSONObject(objStr); // Transformamos el String a JSONObject
            JSONObject tag = new JSONObject(); // Transformamos el epc a formato JSONObject
            tag.put("EPC", epc);
            Integer indexPet = CheckActiveEPC(json, tag); // Verificamos si el EPC esta usado por alguna mascota activa
            if (indexPet != -1){
                JSONObject object = json.getJSONObject(indexPet); // Obtenemos la mascota
                JSONArray acontecimientos = object.getJSONArray("acontecimientos"); // Obtener los acontecimientos de la mascota
                acontecimientos.put(event); // Agregar un nuevo Acontecimeinto
                WriteJson(context,json.toString()); // Escribir el nuevo txt
                return true;
            } else {
                Toast.makeText(context, "NO HAY REGISTRO PARA ESTE TAG", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            Log.d("DORAT", e.toString());
            Toast.makeText(context, "ERROR AL AGREGAR EVENTO", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Función que se encarga de obtener la información de la mascota por su EPC
     * @param context contexto asociado a la vista que llama a la función
     * @param epc el tag escaneado para buscar a la mascota
     * @return Retorna un objeto mascota con toda su información
     */
    public static Pet GetPet(Context context, String epc){
        try {
            String sxml = ReadFileString(context); // Obtener el JSON desde el contexto de la APP
            JSONArray json = new JSONArray(sxml); // Transformar el String a formato JSONArray
            int indexPet = CheckActiveEPC(context,epc); // Verificar el EPC
            if(indexPet != -1){
                JSONObject jsonObject = json.getJSONObject(indexPet);
                // Transformar el JSONObject a un objeto Pet
                Pet pet = new Pet(jsonObject.getString("name"),jsonObject.getString("sex"),jsonObject.getString("birthdate"),jsonObject.getString("address"),
                        jsonObject.getString("allergies"),jsonObject.getString("species"),jsonObject.getString("EPC"));
                pet = GetEventsPerPet(pet,jsonObject); // Obtenemos la lista de acontecimientos
                return pet; // Return the Pet Object with the Event List
            }
            return null; // Retornamos vacío si no encontramos el EPC o si la mascota estaba inactiva
        } catch (Exception e) {
            Log.d("DORAT", e.toString());
            Toast.makeText(context, "NO SE PUDO OBTENER LA MASCOTA", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * Función que se encarga de obtener la información de todas las mascotas
     * @param context contexto asociado a la vista que llama a la función
     * @return Retorna una lista de objetos de mascotas con toda su información
     */
    public static ArrayList<Pet> PetList(Context context){
        try {
            String sxml = ReadFileString(context); // Obtener el JSON desde el contexto de la APP
            JSONArray json = new JSONArray(sxml); // Transformar el String a formato JSONArray
            ArrayList<Pet> petList = new ArrayList<Pet>(); // Variable de una lista de objetos Pet
            for (int i = 0; i < json.length(); i++) {
                JSONObject curr = json.getJSONObject(i); // Guardamos el JSONObject de la mascota en una variable
                if (curr.getBoolean("active")) {
                    // Transformamos de JSONObject a un objecto Pet
                    Pet pet = new Pet(curr.getString("name"), curr.getString("sex"),
                                                                    curr.getString("birthdate"),
                                                                    curr.getString("address"),
                                                                    curr.getString("allergies"),
                                                                    curr.getString("species"),
                                                                    curr.getString("EPC"));
                    // Obtenemos la lista de acontecimientos
                    pet = GetEventsPerPet(pet, curr);
                    // Se añade a la lista
                    petList.add(pet);
                }
            }
            return petList; // Retorna una lista de objeto Pet
        } catch (Exception e) {
            Toast.makeText(context, "NO SE PUDO OBTENER EL LISTADO DE MASCOTAS", Toast.LENGTH_SHORT).show();
            Log.d("DORAT", e.toString());
            return null;
        }
    }

    /**
     * Función que se encarga de obtener la información de todas las actividades por la mascota
     * @param pet objeto mascota a la que se le asignan los eventos
     * @param curr es el objeto JSON que se obtuvo al leer el archivo de texto
     * @return Retorna la mascota con su lista de eventos
     */
    public static Pet GetEventsPerPet(Pet pet, JSONObject curr){
        try {
            // Obtenemos la lista de acontecimientos
            JSONArray list = curr.getJSONArray("acontecimientos");
            for (int j = 0; j < list.length(); j++) {
                // Guardamos el acontecimiento de la posición j en una variable
                JSONObject event = list.getJSONObject(j);
                // Transformamos el JSONObject al objeto Acontecimiento
                Acontecimiento acontecimiento = new Acontecimiento(event.getString("titulo"),
                                                                    event.getString("fecha"),
                                                                     event.getString("descripcion"));
                // Guardamos los acontecimientos de la mascota en una lista de objectos Acontecimiento
                pet.addEvent(acontecimiento);
            }
            return pet;
        } catch (JSONException e) {
            Log.d("DORAT", e.toString());
            return null;
        }
    }

    /**
     * Función que se encarga de actualizar la información de una mascota
     * @param context contexto asociado a la vista que llama a la función
     * @param newPet objeto mascota que se va a actualizar
     * @param oldEPC el el EPC que tenía originalmente la mascota (para encontrarlo en la base de datos)
     * @return Retorna la mascota con su lista de eventos
     */
    public static void UpdatePetInfo(Context context, Pet newPet, String oldEPC){
        try {
            String sxml = ReadFileString(context); // Obtener el JSON desde el contexto de la APP
            JSONArray json = new JSONArray(sxml); // Transformar el String a formato JSONArray
            JSONObject epc = new JSONObject();
            epc.put("EPC",oldEPC);
            Integer indexPet = CheckActiveEPC(json,epc);
            JSONObject curr = json.getJSONObject(indexPet); // Guardamos el JSONObject de la mascota en una variable
            // Transformamos de un objeto Pet a un JSONObject
            curr.put("name", newPet.getName());
            curr.put("sex", newPet.getSex());
            curr.put("birthdate", newPet.getBirthdate());
            curr.put("address", newPet.getAddress());
            curr.put("allergies", newPet.getAllergies());
            curr.put("species", newPet.getSpecies());
            curr.put("EPC", newPet.getEPC());
            WriteJson(context,json.toString()); // Escribimos el nuevo txt
            Toast.makeText(context, "MASCOTA ACTUALIZADA", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, "HA OCURRIDO UN ERROR", Toast.LENGTH_SHORT).show();
            Log.d("DORAT", e.toString());
        }
    }

    /**
     * Función que se encarga de obtener la información de todas las actividades por la mascota
     * @param context contexto asociado a la vista que llama a la función
     * @param epc el el EPC que tiene asignada la mascota
     * @return Retorna la mascota con su lista de eventos
     */
    public static boolean UnlinkPet(Context context, String epc){
        try{
            String txtJson = ReadFileString(context); // Leemos el archivo
            JSONArray jsonArray = new JSONArray(txtJson); // Transformamos a una lista
            int indexPet = CheckActiveEPC(context,epc); // Obtenemos la posición en la lista
            if (indexPet != -1){
                JSONObject jsonObject = jsonArray.getJSONObject(indexPet); // Obtenemos la mascota
                jsonObject.put("active", false); // Lo desvinculamos
                WriteJson(context, jsonArray.toString()); // Se escribe el nuevo archivo
                return true;
            }
            return false;
        }catch (Exception e){
            Log.d("DORAT", e.toString());
            return false;
        }
    }

    /**
     * Función que se encarga de obtener la información de todas las actividades por la mascota
     * @param context contexto asociado a la vista que llama a la función
     * @param epc tag para buscar la mascota correspondiente
     * @param indexAcontecimiento la posición en la lista de acontecimientos
     * @return Retorna la mascota con su lista de eventos
     */
    public static void UpdateEventList(Context context, String epc, int indexAcontecimiento){
        try{
            String txtJson = ReadFileString(context); // Leemos el archivo
            JSONArray jsonArray = new JSONArray(txtJson); // Transformamos a una lista
            int indexPet = CheckActiveEPC(context,epc); // Obtenemos la posición en la lista
            if (indexPet != -1){
                Pet pet = GetPet(context, epc);
                ArrayList<Acontecimiento> acontecimientos = pet.getEventList(); // Obtenemos la lista de acontecimientos
                acontecimientos.remove(indexAcontecimiento); // Removemos el que no necesitamos
                jsonArray.remove(indexPet); // Eliminamos la mascota
                WriteJson(context, jsonArray.toString()); // actualiza el archivo
                AddPet(context,pet); // La añadimos con las acutalizaciones
                Toast.makeText(context, "LISTA DE EVENTOS ACTUALIZADA", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "NO SE ENCONTRÓ LA MASCOTA", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            Log.d("DORAT", e.toString());
            Toast.makeText(context, "ERROR AL BORRAR ACONTECIMIENTO", Toast.LENGTH_SHORT).show();
        }
    }

}
