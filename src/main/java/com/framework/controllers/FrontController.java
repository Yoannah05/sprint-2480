package com.framework.controllers;

import com.framework.annotations.GET;
import com.framework.model.Mapping;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class FrontController extends HttpServlet {

    private Map<String, Mapping> urlMappings = new HashMap<>();

    // Variable pour stocker le nom du package des contrôleurs
    private String controllerPackage;

    @Override
    public void init() throws ServletException {
        // Récupérer le nom du package des contrôleurs depuis les paramètres d'initialisation
        controllerPackage = getServletConfig().getInitParameter("controller-package");
        // Scanner les contrôleurs
        scanControllers();
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        // Récupérer le chemin de l'URL de la requête
        String requestURL = request.getPathInfo();

        // Rechercher le Mapping associé au chemin URL de la requête
        Mapping mapping = urlMappings.get(requestURL);

        if (mapping != null) {
            // Afficher le chemin URL et le Mapping
            out.println("URL: " + requestURL);
            out.println("Mapping: " + mapping);
        } else {
            // Afficher qu'il n'y a pas de méthode associée à ce chemin
            out.println("No method associated with URL: " + requestURL);
        }

        out.close();
    }

    private void scanControllers() {
        try {
            // Charger le class loader pour accéder aux classes du package
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = getClass().getClassLoader();
            }

            // Convertir le nom du package en chemin relatif pour le class loader
            String packagePath = controllerPackage.replace(".", "/");

            // Récupérer les ressources (fichiers .class) du package
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file")) {
                    File packageDirectory = new File(resource.toURI());
                    if (packageDirectory.isDirectory()) {
                        // Parcourir les fichiers dans le répertoire du package
                        File[] files = packageDirectory.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                // Vérifier si le fichier est une classe .class
                                if (file.isFile() && file.getName().endsWith(".class")) {
                                    // Charger la classe à partir du fichier
                                    String className = file.getName().replace(".class", "");
                                    Class<?> clazz = Class.forName(controllerPackage + "." + className);
                                    // Parcourir les méthodes de la classe pour détecter les annotations GET
                                    for (var method : clazz.getDeclaredMethods()) {
                                        if (method.isAnnotationPresent(GET.class)) {
                                            GET getAnnotation = method.getAnnotation(GET.class);
                                            String url = getAnnotation.value();
                                            urlMappings.put(url, new Mapping(clazz.getName(), method.getName()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "FrontController";
    }
}
